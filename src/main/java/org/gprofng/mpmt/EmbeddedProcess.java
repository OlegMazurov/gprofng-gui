/* Copyright (C) 2026 Free Software Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses>.  */
package org.gprofng.mpmt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.mazurov.jdbe.ERPrint;

/**
 * The JDBE engine running on a thread of this JVM, presented as a {@link Process} so that {@link
 * IPC} and {@link IPCProcess} drive it exactly like an external gp-display-text: the same wire
 * protocol, over in-memory pipes instead of stdin/stdout.
 *
 * <p>The engine keeps its state in statics, so only one can run per JVM; {@link #start} waits for
 * the previous one to finish.
 */
final class EmbeddedProcess extends Process {

  /** How long a new engine waits for its predecessor to wind down. */
  private static final long PREVIOUS_ENGINE_TIMEOUT_SEC = 30;

  // Exit values understood by IPCProcess.ProcListener
  private static final int EXIT_FAILURE = 1;
  private static final int EXIT_OUT_OF_MEMORY = 16;

  private static EmbeddedProcess previous;

  private final BytePipe toEngine = new BytePipe();
  private final BytePipe fromEngine = new BytePipe();
  private final BytePipe errors = new BytePipe(); // nothing is written; see below
  private final CountDownLatch done = new CountDownLatch(1);
  private volatile int exitValue;

  static synchronized EmbeddedProcess start(String[] args) throws IOException {
    if (previous != null) {
      try {
        if (!previous.done.await(PREVIOUS_ENGINE_TIMEOUT_SEC, TimeUnit.SECONDS)) {
          throw new IOException("The previous analysis engine is still running");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new InterruptedIOException();
      }
    }
    EmbeddedProcess process = new EmbeddedProcess(args);
    previous = process;
    return process;
  }

  private EmbeddedProcess(String[] args) {
    Thread thread = new Thread(() -> run(args), "JDBE-engine");
    thread.setDaemon(true);
    thread.start();
  }

  private void run(String[] args) {
    int code = 0;
    try {
      ERPrint.runEmbedded(args, toEngine.in, fromEngine.out);
    } catch (ERPrint.ExitException e) {
      code = e.status;
    } catch (OutOfMemoryError e) {
      e.printStackTrace();
      code = EXIT_OUT_OF_MEMORY;
    } catch (Throwable t) {
      t.printStackTrace();
      code = EXIT_FAILURE;
    } finally {
      exitValue = code;
      // The peers see end-of-stream / a broken pipe, as they would when a process dies.
      fromEngine.out.close();
      toEngine.in.close();
      errors.out.close();
      done.countDown();
    }
  }

  @Override
  public OutputStream getOutputStream() {
    return toEngine.out;
  }

  @Override
  public InputStream getInputStream() {
    return fromEngine.in;
  }

  /**
   * The engine's own diagnostics go to this JVM's stderr (and ipc.log), so this stream only
   * exists for {@link org.gprofng.mpmt.ipc.IPCErrorReader}: it stays open and empty until the
   * engine ends.
   */
  @Override
  public InputStream getErrorStream() {
    return errors.in;
  }

  @Override
  public int waitFor() throws InterruptedException {
    done.await();
    return exitValue;
  }

  @Override
  public int exitValue() {
    if (done.getCount() > 0) {
      throw new IllegalThreadStateException();
    }
    return exitValue;
  }

  /** Asks the engine to end by closing its input; it exits on end-of-stream. */
  @Override
  public void destroy() {
    toEngine.out.close();
  }

  /**
   * A single-producer single-consumer in-memory pipe. Unlike {@link java.io.PipedInputStream} it
   * does not track the identity or liveness of the threads using it (the IPC readers are
   * restarted), and closing either end promptly releases a blocked peer.
   */
  private static final class BytePipe {
    private final byte[] buf = new byte[1 << 16];
    private int head;
    private int count;
    private boolean writerClosed;
    private boolean readerClosed;

    final PipeIn in = new PipeIn();
    final PipeOut out = new PipeOut();

    private synchronized int read(byte[] b, int off, int len) throws IOException {
      if (len == 0) {
        return 0;
      }
      while (count == 0) {
        if (readerClosed) {
          throw new IOException("Stream closed");
        }
        if (writerClosed) {
          return -1;
        }
        await();
      }
      int n = Math.min(len, count);
      for (int i = 0; i < n; i++) {
        b[off + i] = buf[(head + i) % buf.length];
      }
      head = (head + n) % buf.length;
      count -= n;
      notifyAll();
      return n;
    }

    private synchronized void write(byte[] b, int off, int len) throws IOException {
      while (len > 0) {
        if (writerClosed || readerClosed) {
          throw new IOException("Pipe closed");
        }
        if (count == buf.length) {
          await();
          continue;
        }
        int n = Math.min(len, buf.length - count);
        for (int i = 0; i < n; i++) {
          buf[(head + count + i) % buf.length] = b[off + i];
        }
        count += n;
        off += n;
        len -= n;
        notifyAll();
      }
    }

    private void await() throws InterruptedIOException {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new InterruptedIOException();
      }
    }

    private synchronized void closeReader() {
      readerClosed = true;
      notifyAll();
    }

    private synchronized void closeWriter() {
      writerClosed = true;
      notifyAll();
    }

    final class PipeIn extends InputStream {
      @Override
      public int read() throws IOException {
        byte[] one = new byte[1];
        return BytePipe.this.read(one, 0, 1) < 0 ? -1 : one[0] & 0xff;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return BytePipe.this.read(b, off, len);
      }

      @Override
      public int available() {
        synchronized (BytePipe.this) {
          return count;
        }
      }

      @Override
      public void close() {
        closeReader();
      }
    }

    final class PipeOut extends OutputStream {
      @Override
      public void write(int b) throws IOException {
        BytePipe.this.write(new byte[] {(byte) b}, 0, 1);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        BytePipe.this.write(b, off, len);
      }

      @Override
      public void close() {
        closeWriter();
      }
    }
  }
}

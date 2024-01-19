/* Copyright (C) 2022-2024 Free Software Foundation

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

package org.gprofng.mpmt.progress;

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.util.Stack;

public class SystemProgressPanel extends ProgressPanel {
  private ProgressHandle progressHandle;

  private class Task {
    private String name;

    public Task(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  private Stack<Task> progressBars = new Stack<Task>();

  public SystemProgressPanel() {
    super();
    progressHandle = createHandle();
  }

  public void reset() {
    finishProgress();
    setProgressCancelContext(null);
    progressBars = new Stack<Task>();
  }

  public synchronized Object progressBarStart(String taskName) {
    //      IPCContext.newCurrentContext("Unknown task...", IPCContext.Scope.SESSION, this);  <===
    // FIXUP: need real name of task like "Opening Experiment..."
    if (progressBars.empty()) {
      startProgress();
    }
    progressHandle.progress(taskName);
    Task task = new Task(taskName);
    progressBars.push(task);
    //        System.out.println("SystemProgresspanel start: " + progressBars.size() + " " +
    // progressBars.peek().getName());
    return task;
  }

  public synchronized void progressBarStop(Object id) {
    if (progressBars.empty()) {
      AnLog.log(
          "error: SystemProgresspanel:progressBarStop: progressBars is empty!. See bug 19598982");
      return;
    }
    boolean didIt = progressBars.remove(id);
    if (!didIt) {
      AnLog.log(
          "error: SystemProgresspanel:progressBarStop: can't find id " + ((Task) id).getName());
    }
    if (progressBars.empty()) {
      finishProgress(); // <=== FIXUP: should be done in IPCResult
      setProgressCancelContext(null); // <=== FIXUP: should be done in IPCResult
    } else {
      String taskName = progressBars.peek().getName();
      progressHandle.progress(taskName);
    }
    //        System.out.println("SystemProgresspanel stop : " + progressBars.size() + " " +
    // (progressBars.size() > 0 ? progressBars.peek().getName() : "-"));
  }

  private void startProgress() {
    AnUtility.invokeLaterOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            startProgressImpl();
          }
        });
  }

  private void startProgressImpl() {
    if (progressHandle == null) {
      return;
    }
    progressHandle.switchToIndeterminate();
    progressHandle.start();
  }

  public CancelContext getProgressCancelContext() {
    if (progressHandle == null) {
      return null;
    } else {
      return progressHandle.getCancelContext();
    }
  }

  public void setProgressCancelContext(final CancelContext cancelContex) {
    AnUtility.invokeLaterOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            setProgressCancelContextImpl(cancelContex); // /
          }
        });
  }

  private void setProgressCancelContextImpl(CancelContext cancelContex) {
    if (progressHandle == null) {
      return;
    }
    progressHandle.setCancelContext(cancelContex);
  }

  public void setProgress(final int percentage, final String proc_str) {
    AnUtility.invokeLaterOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            setProgressImpl(percentage, proc_str);
          }
        });
  }

  private void setProgressImpl(final int percentage, final String proc_str) {
    if (progressHandle == null) {
      return;
    }
    if (percentage > 0) {
      progressHandle.switchToDeterminate(100);
      progressHandle.progress(proc_str, percentage);
    } else {
      progressHandle.switchToIndeterminate();
      if (proc_str != null && proc_str.length() > 0) {
        progressHandle.progress(proc_str);
      }
    }
  }

  private void finishProgress() {
    AnUtility.invokeLaterOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            finishProgressImpl();
          }
        });
  }

  private void finishProgressImpl() {
    if (progressHandle == null) {
      return;
    }
    progressHandle.finish();
  }
}

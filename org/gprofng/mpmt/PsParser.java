/* Copyright (C) 2022 Free Software Foundation

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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A service for accessing 'ps'.
 *
 * <pre>
 * Typical usage:
 *
 *      PsParser psParser = PsParser.getDefault("Solaris"); // or "Linux"
 *      psParser.setPsOutput(psOutput); // String psOutput is 'ps -ef' output
 *      Vector<String> th = psParser.getData(true).header();
 *      java.util.regex.Pattern re = java.util.regex.Pattern.compile(".");
 *      Vector<Vector<String>> tp = psParser.getData(true).processes(re);
 *      (javax.swing.table.DefaultTableModel)jTable.getModel().setDataVector(tp, th);
 *
 * </pre>
 */
public abstract class PsParser {

  private static final Logger logger = Logger.getLogger(PsParser.class.getName());

  public final class PsData {

    private List<List<String>> processes = new ArrayList<List<String>>();

    private List<String> header = null;

    /** Translated header names in the table */
    public List<String> header() {
      return header;
    }

    void setHeader(List<String> header) {
      this.header = header;
    }

    public int commandColumnIdx() {
      return commandColumnIndex();
    }

    public int pidColumnIdx() {
      return pidColumnIndex();
    }

    //        public FileMapper getFileMapper() {
    //            return PsProvider.this.getFileMapper();
    //        }
    /** filter lines and convert to columns */
    public List<List<String>> processes(Pattern re) {
      List<List<String>> res = new ArrayList<List<String>>();
      // Do filtering
      outer:
      for (List<String> proc : processes) {
        for (String field : proc) {
          if (re.matcher(field).find()) {
            res.add(proc);
            continue outer;
          }
        }
      }
      return res;
    }

    void addProcess(String line) {
      int offset = 0;

      List<String> columns = new ArrayList<String>(headerStr().length - 3);
      for (int cx = 0; cx < headerStr().length; cx++) {
        String s = null;
        if (cx == 7) {
          s = line.substring(offset + fields[cx][0]);
        } else // extra check for UID
        if (headerStr()[cx].contains("UID")) {
          assert offset == 0;
          int end = fields[cx][1];
          while (line.charAt(end + 1) != ' ') {
            end++;
          }
          s = line.substring(fields[cx][0], end + 1);
          offset = end - fields[cx][1];
        } else {
          s = line.substring(offset + fields[cx][0], offset + fields[cx][1] + 1);
        }

        if (cx != 3 && cx != 5 /* && cx != 6 */) { // No "C", "TTY" and /* "TIME" */columns
          columns.add(s.trim());
        }
      }
      processes.add(columns);
    }

    private void updateCommand(String pid, String command) {
      for (List<String> proc : processes) {
        if (pid.equals(proc.get(pidColumnIdx()))) {
          proc.set(commandColumnIdx(), command);
        }
      }
    }
  }

  // can't be static, for format of ps output is different from host to host
  private List<String> parsedHeader = null;

  protected static final String zero = "0";
  private String uid = null;

  private int fields[][] = new int[8][2];

  /** Specialization of PsParser for Solaris */
  static class SolarisPsParser extends PsParser {

    private static final String header_str_solaris[] = {
      "UID", "PID", "PPID", "C", // skipped
      "STIME", "TTY    ", // skipped
      " TIME", // skipped
      "CMD",
    };

    public SolarisPsParser(/* NM Host host */ ) {
      super(/* NM host */ );
    }

    @Override
    public int commandColumnIndex() {
      return 4;
    }

    @Override
    public int pidColumnIndex() {
      return 1;
    }

    @Override
    public String[] headerStr() {
      return header_str_solaris;
    }

    /*
     * for executor, not used now,
     * in the future if we want to get uid from remote host
     * this is will be used.
     */
    //	protected String[] uidCommand1() {
    //	    String [] args = new String[2];
    //	    args[0] = "/usr/xpg4/bin/id";
    //	    args[1] = "-u";
    //	    return args;
    //	}
    @Override
    protected String uidCommand() {
      return "/usr/xpg4/bin/id -u";
    }

    /* OLD
    protected String[] psCommand1(String uid) {

        if (Log.Ps.null_uid)
    	uid = null;

        if ( (uid == null) || (uid.equals(zero)) ) {
    	// uid=0 => root; use ps -ef
    	// OLD return "LANG=C /bin/ps -www -o pid,tty,time,cmd";
    	String [] args = new String[2];
    	args[0] = "/usr/bin/ps";
    	args[1] = "-ef" ;
    	return args;
    	// return "LANG=C /usr/bin/ps -ef";
        } else {
    	String [] args = new String[3];
    	args[0] = "/usr/bin/ps";
    	args[1] = "-fu";
    	args[2] = uid;
    	return args;
    	// return "LANG=C /usr/bin/ps -fu " + uid;
        }
    }
            */
    @Override
    protected String psCommand(String uid) {
      // SHOULD set LC_ALL=C here since we're depending
      // on column widths to get to the individual ps items!
      // (moved to getData)

      //	    if (Log.Ps.null_uid)
      //		uid = null;
      if ((uid == null) || (uid.equals(zero))) {
        // uid=0 => root; use ps -ef
        return "/usr/bin/ps -ef";
      } else {
        return "/usr/bin/ps -fu " + uid;
      }
    }

    @Override
    public PsData getData(boolean allProcesses) {
      PsData res = super.getData(allProcesses);

      // pargs call if needed
      //            if (res != null && !DISABLE_PARGS && !res.processes.isEmpty()) {
      //                NativeProcessBuilder pargsBuilder =
      // NativeProcessBuilder.newProcessBuilder(exEnv);
      //                pargsBuilder.setExecutable("/usr/bin/pargs").redirectError();
      //                pargsBuilder.getEnvironment().put("LC_ALL", "C");
      //                String[] pargs_args = new String[res.processes.size()+1];
      //                pargs_args[0] = "-Fl";
      //                int idx = 1;
      //                for (Vector<String> proc : res.processes) {
      //                    pargs_args[idx++] = proc.get(pidColumnIndex());
      //                }
      //                pargsBuilder.setArguments(pargs_args);
      //
      //                try {
      //                    List<String> pargsOutput =
      // ProcessUtils.readProcessOutput(pargsBuilder.call());
      //                    updatePargsData(res, pargs_args, pargsOutput);
      //                } catch (IOException ex) {
      //                    Exceptions.printStackTrace(ex);
      //                }
      //            }
      return res;
    }
  }

  //    static void updatePargsData(PsData res, String[] pargs_args, List<String> pargsOutput) {
  //        int idx = 1;
  //        for (String procArgs : pargsOutput) {
  //            if (procArgs.isEmpty() ||
  //                    procArgs.startsWith("pargs: Warning") ||
  //                    procArgs.startsWith("pargs: Couldn't determine locale of target process") ||
  //                    procArgs.startsWith("pargs: Some strings may not be displayed properly")) {
  //                continue;
  //            }
  //            if (!procArgs.startsWith("pargs:")) {
  //                res.updateCommand(pargs_args[idx], procArgs);
  //            }
  //            idx++;
  //        }
  //        if ( (idx-1) != res.processes.size()) {     // we should check if the operation has been
  // applied to all processes
  //            throw new AssertionError("Process list:" + res.processes.toString() + "\npargs
  // output:" + pargsOutput.toString());
  //        }
  //    }
  /** Specialization of PsParser for Linux */
  static class LinuxPsParser extends PsParser {

    private static final String header_str_linux[] = {
      "UID     ",
      "PID",
      "PPID",
      "C", // skipped
      "STIME",
      "TTY   ", // skipped
      "    TIME", // skipped
      "CMD",
    };

    public LinuxPsParser(/*NM Host host */ ) {
      super(/*NM host */ );
    }

    @Override
    public int commandColumnIndex() {
      return 4;
    }

    @Override
    public int pidColumnIndex() {
      return 1;
    }

    @Override
    public String[] headerStr() {
      return header_str_linux;
    }

    /*
     * for executor, not used,
     * in the future if we want to get uid from remote host
     * this is will be used.
     */
    //	protected String [] uidCommand1() {
    //	    String [] args = new String[2];
    //	    args[0] = "/usr/bin/id";
    //	    args[1] = "-u";
    //	    return args;
    //	}
    @Override
    protected String uidCommand() {
      return "/usr/bin/id -u";
    }

    /* OLD
    protected String[] psCommand1(String uid) {

        if (Log.Ps.null_uid)
    	uid = null;

        if ( (uid == null) || (uid.equals(zero)) ) {
    	// uid=0 => root; use ps -ef
    	// OLD return "LANG=C /bin/ps -www -o pid,tty,time,cmd";
    	String [] args = new String[2];
    	args[0] = "/bin/ps";
    	args[1] = "-ef";
    	return args;
        } else {
    	String [] args = new String[3];
    	args[0] = "/bin/ps";
    	args[1] = "-fu";
    	args[2] = uid;
    	return args;
        }
    }
            */
    @Override
    protected String psCommand(String uid) {
      // SHOULD set LC_ALL=C here since we're depending
      // on column widths to get to the individual ps items!
      // (moved to getData)

      //	    if (Log.Ps.null_uid)
      //		uid = null;
      if ((uid == null) || (uid.equals(zero))) {
        // uid=0 => root; use ps -ef
        // OLD return "LANG=C /bin/ps -www -o pid,tty,time,cmd";
        return "/bin/ps -ef";
      } else {
        return "/bin/ps -fu " + uid + " --width 1024";
      }
    }
  }

  //    static class MacOSPsProvider extends LinuxPsProvider {
  //        private final static String header_str_mac[] = {
  //	    "  UID",
  //	    "   PID",
  //	    "  PPID",
  //	    "   C",		// skipped
  //	    "STIME",
  //	    "TTY     ",		// skipped
  //	    "    TIME",         // skipped
  //	    "CMD",
  //	};
  //
  //        @Override
  //        public String[] headerStr() {
  //            return header_str_mac;
  //        }
  //
  //        public MacOSPsProvider(Host host) {
  //            super(host);
  //        }
  //
  //        @Override
  //        protected String psCommand(String uid) {
  //            if ( (uid == null) || (uid.equals(zero)) ) {
  //		return "/bin/ps -ef";
  //	    } else {
  //		return "/bin/ps -fu " + uid;
  //	    }
  //        }
  //    }
  /** Specialization of PsProvider for Windows */
  //    static class WindowsPsProvider extends PsProvider {
  //        private FileMapper fileMapper = FileMapper.getDefault();
  //
  //	private final static String header_str_windows[] = {
  //	    "PID",
  //	    "PPID",
  //            "PGID",
  //            "WINPID",
  //	    "TTY",		// skipped
  //            "UID",
  //            "STIME",
  //	    "COMMAND",
  //	};
  //
  //	public WindowsPsProvider(Host host) {
  //	    super(host);
  //	}
  //
  //	public int commandColumnIndex() {
  //	    return 4;
  //	}
  //
  //        // see IZ 193741 - skip status column
  //        @Override
  //        protected int firstPosition() {
  //            return 1;
  //        }
  //
  //        public int pidColumnIndex() {
  //	    return 0;
  //	}
  //
  //	public String[] headerStr() {
  //	    return header_str_windows;
  //	}
  //
  //	protected String uidCommand() {
  //	    return getUtilityPath("id") + " -u";
  //	}
  //
  //	protected String psCommand(String uid) {
  //	    // SHOULD set LC_ALL=C here since we're depending
  //	    // on column widths to get to the individual ps items!
  //            // (moved to getData)
  //
  //	    if (Log.Ps.null_uid)
  //		uid = null;
  //
  //            // Always show all processes on Windows (-W option), see IZ 193743
  //	    if ( (uid == null) || (uid.equals(zero)) ) {
  //		// uid=0 => root; use ps -ef
  //		return getUtilityPath("ps") + " -W";
  //	    } else {
  //		return getUtilityPath("ps") + " -u " + uid + " -W";
  //	    }
  //	}
  //
  //        private String getUtilityPath(String util) {
  //            File file = new File(CompilerSetUtils.getCygwinBase() + "/bin", util + ".exe");
  //            if (file.exists()) {
  //                fileMapper = FileMapper.getByType(FileMapper.Type.CYGWIN);
  //            } else {
  //                fileMapper = FileMapper.getByType(FileMapper.Type.MSYS);
  //                file = new File(CompilerSetUtils.getCommandFolder(null), util + ".exe");
  //            }
  //            if (file.exists()) {
  //                return file.getAbsolutePath();
  //            }
  //            return util;
  //        }
  //
  //        @Override
  //        public FileMapper getFileMapper() {
  //            return fileMapper;
  //        }
  //    }
  //    public static synchronized PsProvider getDefault(Host host) {
  //        PsProvider psProvider = host.getResource(PsProvider.class);
  //        if (psProvider == null) {
  //            ExecutionEnvironment exEnv = host.executionEnvironment();
  //            if (!ConnectionManager.getInstance().connect(exEnv)) {
  //                return null;
  //            }
  //            try {
  //                HostInfo hostInfo = HostInfoUtils.getHostInfo(exEnv);
  //                switch (hostInfo.getOSFamily()) {
  //                    case LINUX:
  //                        psProvider = new LinuxPsProvider(host);
  //                        break;
  //                    case WINDOWS:
  //                        psProvider = new WindowsPsProvider(host);
  //                        break;
  //                    case MACOSX:
  //                        psProvider = new MacOSPsProvider(host);
  //                        break;
  //                    default:
  //                        psProvider = new SolarisPsProvider(host);
  //                }
  //            } catch (CancellationException e) {
  //                // user cancelled connection attempt
  //            } catch (Exception e) {
  //                Exceptions.printStackTrace(e);
  //            }
  //            host.putResource(PsProvider.class, psProvider);
  //        }
  //        return psProvider;
  //    }
  public static synchronized PsParser getDefault(String OSFamily) {
    PsParser psParser = null;
    if ("Linux".equals(OSFamily)) {
      psParser = new LinuxPsParser();
    }
    if ("Solaris".equals(OSFamily)) {
      psParser = new SolarisPsParser();
    }
    return psParser;
  }

  /** Return index of the CMD column. */
  protected abstract int commandColumnIndex();

  protected abstract int pidColumnIndex();

  protected abstract String[] headerStr();

  //    protected abstract String[] uidCommand1(); // for executor, not used
  protected abstract String psCommand(String root);

  // OLD protected abstract String[] psCommand1(String root); // for executor
  protected abstract String uidCommand(); // for Runtime.exe

  protected int firstPosition() {
    return 0;
  }

  // return file mapper (important only on Windows)
  //    public FileMapper getFileMapper() {
  //        return FileMapper.getDefault();
  //    }
  //
  //    protected final ExecutionEnvironment exEnv;
  //
  //    private PsProvider(Host host) {
  //        exEnv = host.executionEnvironment();
  //    }
  // "host" for getUid is usually "localhost"
  private String getUid() {
    //        if (uid == null) {
    //            try {
    //                NativeProcessBuilder npb = NativeProcessBuilder.newProcessBuilder(exEnv);
    //                npb.setCommandLine(uidCommand());
    //                NativeProcess process;
    //                try {
    //                    process = npb.call();
    //                } catch (Exception e) {
    //                    logger.log(Level.WARNING, "Failed to exec id command", e);
    //                    return exEnv.getUser();
    //                }
    //
    //                String res = ProcessUtils.readProcessOutputLine(process);
    //
    //                int exitCode = process.waitFor();
    //                if (exitCode != 0) {
    //                    String msg = "id command failed with " + exitCode;
    //                    logger.log(Level.WARNING, msg);
    //                    return exEnv.getUser();
    //                }
    //                if (!res.isEmpty()) {
    //                    uid = res;
    //                } else {
    //                    uid = exEnv.getUser();
    //                }
    //            } catch (Exception e) {
    //                ErrorManager.getDefault().annotate(e, "Failed to parse OutputStream of uid
    // command");
    //                ErrorManager.getDefault().notify(e);
    //            }
    //        }
    return uid;
  }

  /** Debugging method for printing column boundaries as discovered by parseHeader(). */
  private void printFields(String str) {
    System.out.printf("------------------------------------------------\n");
    System.out.printf("%s\n", str);

    for (int sx = 0; sx < str.length(); sx++) {
      boolean found = false;
      for (int cx = 0; cx < headerStr().length; cx++) {
        if (fields[cx][0] == sx) {
          System.out.printf("%d", cx);
          found = true;
        } else if (fields[cx][1] == sx) {
          System.out.printf("%d", cx);
          found = true;
        }
      }
      if (!found) {
        System.out.printf(" ");
      }
    }
    System.out.printf("\n");
    System.out.printf("------------------------------------------------\n");
  }

  /**
   * Return a Vector of headers based on the first line emitted by 'ps' and populate 'fields' as a
   * side-effect.
   *
   * <p>First lines look like this on solaris:
   *
   * <p>| UID PID PPID C STIME TTY TIME CMD | ivan 1501 1483 0 Nov 26 console 0:07 xterm -name
   * edit-left
   *
   * <p>and like this on linux:
   *
   * <p>|UID PID PPID C STIME TTY TIME CMD |ivan 11585 11583 0 12:39 pts/2 00:00:00 -csh
   *
   * <p>Field justifications are as follows:
   *
   * <p>field solaris linux ------------------------------ UID right left PID right right PPID right
   * right C 1 character 1 STIME right left TTY left left TIME right right CMD left left
   *
   * <p>The current column boundary determination is as follows: for solaris
   * |------------------------------------------------ | UID PID PPID C STIME TTY TIME CMD |0 01 12
   * 23 34 45 56 67 7 |------------------------------------------------ ... and linux ...:
   * |------------------------------------------------ |UID PID PPID C STIME TTY TIME CMD |0 01 12
   * 23 34 45 56 67 7 |------------------------------------------------
   *
   * <p>The left side of left-aligned columns is one column too much to the left (STIME, C, TTY and
   * CMD). This is no problem as long as the left edge of this columns is next to a right-aligned
   * column. It should just be a space and get eaten up by 'trim'. TTY on linux is the only one
   * which doesn't abide by this. But I'm going to wing it for now and postpone making this column
   * discovery even more involved.
   */
  List<String> parseHeader(String str) {

    /* OLD
    // parsedHeader is static so we only do this once
    if (parsedHeader != null)
        return parsedHeader;
            */
    //	if (Log.Ps.debug)
    //	    System.out.printf("parseHeader: '%s'\n", str);
    parsedHeader = new ArrayList<String>(headerStr().length - 3);
    for (int cx = 0; cx < headerStr().length; cx++) {
      String s = null;
      int i;

      i = str.indexOf(headerStr()[cx]);

      // fields[cx][0] the begining of this column
      // fields[cx][1] the end of this column
      if (i >= 0) { // found
        if (cx == 0) // first column
        {
          fields[cx][0] = firstPosition();
        }
        fields[cx][1] = i + headerStr()[cx].length() - 1;
      }

      if (cx == 7) {
        // last one
        s = str.substring(fields[cx][0]);
      } else {
        s = str.substring(fields[cx][0], fields[cx][1] + 1);
        fields[cx + 1][0] = i + headerStr()[cx].length();
      }
      //	if (Log.Ps.debug)
      //	    System.out.println("fields : " + fields[cx][0] + " " + fields[cx][1]);

      if (cx != 3 && cx != 5 /* && cx != 6 */) // No "C", "TTY" and /* "TIME" */ columns
      {
        parsedHeader.add(s.trim());
      }
    }

    //	if (Log.Ps.debug)
    //	    printFields(str);
    // translate header
    for (int hx = 0; hx < parsedHeader.size(); hx++) {
      String h = parsedHeader.get(hx);
      // NM parsedHeader.set(hx, Catalog.get("PS_HDR_" + h));
      parsedHeader.set(hx, h);
    }

    return parsedHeader;
  }

  /**
   * Execute a ps command and return the data.
   *
   * <p>Executes a ps command, captures the output, remembers the first line as the 'parsedHeader',
   * stuffs the rest of the lines into 'PsData.lines'. PsData will columnize lines later.
   */
  public PsData getData(boolean allProcesses) {
    PsData psData = new PsData();
    String luid = allProcesses ? null : getUid();

    //	try {
    //            //FIXME
    //            NativeProcessBuilder npb = NativeProcessBuilder.newProcessBuilder(exEnv);
    //            npb.setCommandLine(psCommand(luid));
    //            npb.getEnvironment().put("LANG", "C");
    //
    //            NativeProcess process;
    //	    try {
    //		process = npb.call();
    //	    } catch (Exception e) {
    //		logger.log(Level.WARNING, "Failed to exec ps command", e);
    //		return null;
    //	    }
    int lineNo = 0;
    // NM for (String line : ProcessUtils.readProcessOutput(process)) {
    for (String line : getPsOutput()) {
      //		if (Log.Ps.debug)
      //		    System.out.printf("PsOutput: '%s'\n", line);

      if (line.indexOf("UID", 0) != -1) {
        // first line
        psData.setHeader(parseHeader(line));
        lineNo++;
      } else if (lineNo++ > 0) {
        psData.addProcess(line);
      }
    }

    //            int exitCode = process.waitFor();
    //	    if (exitCode != 0) {
    //		String msg = "ps command failed with " + exitCode;
    //		logger.log(Level.WARNING, msg);
    //		return null;
    //	    }
    //
    //	} catch (Exception e) {
    //	    ErrorManager.getDefault().annotate(e, "Failed to parse OutputStream of ps command");
    //	    ErrorManager.getDefault().notify(e);
    //	}
    //
    //	if (psData.processes.isEmpty()) {
    //	    ErrorManager.getDefault().log(ErrorManager.EXCEPTION,
    //		"No lines from ");
    //	}
    return psData;
  }

  // TEMPORARY
  private String psOutput = null;

  public void setPsOutput(String s) {
    psOutput = s;
  }

  String[] getPsOutput() {
    String[] s = {""}; // TEMPORARY
    List<String> v = new ArrayList();
    if (null != psOutput) {
      for (int n = 0; n < psOutput.length(); ) {
        int k = psOutput.indexOf('\n', n);
        if (k < n) {
          break; // ignore incomplete line
        }
        String line = psOutput.substring(n, k);
        v.add(line);
        n = k + 1;
      }
    }
    int len = v.size();
    if (len > 0) {
      s = new String[len];
      for (int i = 0; i < len; i++) {
        s[i] = v.get(i);
      }
    }
    return s;
  }
}

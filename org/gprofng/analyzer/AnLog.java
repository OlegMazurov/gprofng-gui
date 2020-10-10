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

package org.gprofng.analyzer;

import org.gprofng.mpmt.persistence.UserPref;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AnLog {

  private static final String logfileName = "log.text";
  private static String logFilePath = null;
  private static FileWriter fileWriter = null;

  private static FileWriter getFileWriter() {
    if (fileWriter == null) {
      boolean ok = new File(getLogFileDir()).mkdirs();
      File logFile = new File(getLogFilePath());
      try {
        fileWriter = new FileWriter(logFile, false);
      } catch (IOException ioe) {
      }
    }
    return fileWriter;
  }

  public static String getLogFileDir() {
    return UserPref.getInstance().getAnalyzerDirPath();
  }

  public static String getLogFilePath() {
    if (logFilePath == null) {
      logFilePath = getLogFileDir() + "/" + logfileName;
    }
    return logFilePath;
  }

  public static void log(String text) {
    if (text == null) {
      return;
    }
    if (getFileWriter() == null) {
      System.err.println(text);
      return;
    }
    try {
      getFileWriter().write(text);
      getFileWriter().write("\n");
      getFileWriter().flush();
    } catch (IOException ioe) {
    }
  }
}

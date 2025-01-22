/* Copyright (C) 2022-2025 Free Software Foundation

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


package org.gprofng.mpmt.export;

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.export.ExportSupport.ExportFormat;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Export {
  public static void exportText(String text, String outputFilePath) {
    printTextToFile(text, outputFilePath);
  }

  public static void exportImage(Component component, String outputFilePath) {
    printImageToFile(component, outputFilePath);
  }

  // ----------------------------------------------------------------------------------------------------

  //    private static AnWindow getAnWindow() {
  //        return (AnWindow) Analyzer.getInstance().win_list.get(0);
  //    }

  // Print display
  private static void printTextToFile(String textImage, String file) {
    if (file == null || file.equals("")) {
      // Print to standard output
      System.out.println(textImage);
    } else {
      File of = null;
      try {
        of = new File(file);
        // Write to temp file
        BufferedWriter bw = new BufferedWriter(new FileWriter(of));
        bw.write(textImage);
        bw.close();
      } catch (IOException e) {
        String s = "analyzer: error: cannot write to file " + of.getAbsolutePath();
        // FIXUP
      }
    }
  }

  private static String printImageToFile(Component display, String file) {
    String s = null;
    if (display == null) {
      return null;
    }
    if (file != null) {
      if (file.equals("")) {
        s = "Please, specify file name (recommended suffix .jpg)";
        return s;
      } else {
        File of = null;
        try {
          of = new File(file);
          // Capture image to output file
          captureComponentToJpeg(display, of);
        } catch (IOException e) {
          s = "analyzer: error: cannot write to file " + of.getAbsolutePath();
          return s;
        }
      }
    }
    return null;
  }

  private static void captureComponentToJpeg(Component c, File destFile) throws IOException {
    BufferedImage image =
        new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
    c.paint(image.createGraphics());
    ImageIO.write(image, "JPEG", destFile);
  }

  private static ExportFormat getFormat() {
    ExportFormat format = null;
    int printMode = getPrintMode();
    if (printMode == 0) {
      format = ExportFormat.TEXT;
    } else if (printMode == 1) {
      format = ExportFormat.HTML;
    } else if (printMode == 2) {
      format = ExportFormat.CSV;
    } else {
      assert true;
    }
    return format;
  }

  public static void setFormat(ExportFormat format, Character delimiter) {
    String printMode = null;
    if (format == ExportFormat.TEXT) {
      printMode = "text";
    } else if (format == ExportFormat.HTML) {
      printMode = "html";
    } else if (format == ExportFormat.CSV) {
      printMode = "" + delimiter;
    } else {
      assert true;
    }
    setPrintMode(printMode);
  }

  private static Integer getLimit() {
    Integer limit = null;
    int lim = getPrintLimit();
    if (lim != 0) {
      limit = lim;
    }
    return limit;
  }

  public static void setLimit(Integer limit) {
    int lim = 0;
    if (limit != null) {
      lim = limit.intValue();
    }
    setPrintLimit(lim);
  }

  private String getDelimiter() {
    return getPrintDelim();
  }

  public static String printData(
      final int type, final int subtype, final String printer, final String file) {
    AnWindow window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      window.IPC().send("printData");
      window.IPC().send(0);
      window.IPC().send(type);
      window.IPC().send(subtype);
      window.IPC().send(printer);
      window.IPC().send(file);
      return window.IPC().recvString();
    }
  }

  private static int getPrintLimit() {
    AnWindow window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      window.IPC().send("getPrintLimit");
      window.IPC().send(0);
      return window.IPC().recvInt();
    }
  }

  private static String setPrintLimit(int limit) {
    AnWindow window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      window.IPC().send("setPrintLimit");
      window.IPC().send(0);
      window.IPC().send(limit);
      return (String) window.IPC().recvString();
    }
  }

  private static String setPrintMode(final String m) {
    AnWindow window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      window.IPC().send("setPrintMode");
      window.IPC().send(0);
      window.IPC().send(m);
      return window.IPC().recvString();
    }
  }

  private static int getPrintMode() {
    AnWindow window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      window.IPC().send("getPrintMode");
      window.IPC().send(0);
      return window.IPC().recvInt();
    }
  }

  private static String getPrintDelim() {
    AnWindow window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      window.IPC().send("getPrintDelim");
      window.IPC().send(0);
      String s = window.IPC().recvString();
      if (s == null) {
        return ",";
      }
      return s;
    }
  }
}

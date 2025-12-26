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

package org.gprofng.mpmt;

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.guitesting.GUITesting;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class AnLocale {

  private static boolean addDebugInfo = false;
  private static Locale locale = Locale.getDefault();
  private static ResourceBundle resource = null;

  static {
    try {
      if (!GUITesting.getInstance().isRunningUnderNB()) {
        ClassLoader loader = getLoader("org-gprofng-mpmt");
        resource = ResourceBundle.getBundle("org.gprofng.mpmt.Bundle", locale, loader);
      }
    } catch (MissingResourceException e) {
      System.err.println("Cannot find resource bundle.");
    }
  }

  // Get the locale ClassLoader
  public static ClassLoader getLoader(String name) {
    String path;
    URL[] urls;

    if (name.equals("analyzer_help")) {
      path = "file:" + Analyzer.fdhome + "/modules/docs/";
    } else {
      path = "file:" + Analyzer.fdhome + "/modules/autoload/";
    }
    try {
      urls = new URL[] {
        new URL(path + "locale/" + name + "_" + locale.getLanguage() + ".jar"),
        new URL(path + "locale/" + name + "_" + locale.getLanguage() + "_"
                + locale.getCountry() + ".jar"),
        new URL(path + name + ".jar")
      };
    } catch (MalformedURLException e) {
      return null;
    }

    return new URLClassLoader(urls);
  }

  private static String addDebugInfo(String ret) {
    ret = "XXX>" + ret + "<XXX";
    return ret;
  }

  // Same for getResource and for automatically creating Bundle.properties
  public static String getString(String str) {
    String ret = str;
    if (resource != null) {
      try {
        ret = resource.getString(str);
      } catch (MissingResourceException mre) {
        AnLog.log("MissingResourceException " + str);
      }
    }
    if (addDebugInfo) {
      ret = addDebugInfo(ret);
    }
    return ret;
  }

  // Same for getResource and for automatically creating Bundle.properties
  // postfix is needed for localizable strings with mnemonics (to avoid multiple mnemonics)
  public static String getString(String str, String postfix) {
    String strPostFix = str + "[" + postfix + "]";
    String ret;
    if (resource != null) {
      ret = getString(strPostFix);
    } else {
      ret = str;
      if (addDebugInfo) {
        ret = addDebugInfo(ret);
      }
    }
    return ret;
  }

  // Resource string for mnemonics
  public static char getString(char c, String key) {
    char ret;
    if (resource != null) {
      ret = getString(key).charAt(0);
    } else {
      ret = c;
      if (addDebugInfo) {
        ret = 'X';
      }
    }
    return ret;
  }
}

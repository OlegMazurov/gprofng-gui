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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

// Analyzer object
public abstract class AnObject {

  public static final String quote_space = "         ";
  public static final String zero_decimal = new DecimalFormat("0.   ").format(0.);
  public static final String zero_sdecimal = new DecimalFormat("+0.   ").format(0.);
  public static final String zero_percent = new DecimalFormat("0.  ").format(0.);
  public static final DecimalFormat format_decimal = new DecimalFormat("0.000");
  public static final DecimalFormat format_sdecimal = new DecimalFormat("+0.000;-0.000");
  public static final DecimalFormat format_percent = new DecimalFormat("0.00");

  public static final DecimalFormat format_group_decimal;
  public static final DecimalFormat format_group_sdecimal;
  public static final DecimalFormat format_group_integer;
  public static final DecimalFormat format_group_sinteger;

  static {
    DecimalFormatSymbols df_sym;
    df_sym = new DecimalFormatSymbols();
    df_sym.setGroupingSeparator('@');
    format_group_decimal = new DecimalFormat("#,##0.000", df_sym);
    format_group_sdecimal = new DecimalFormat("#,##0.000", df_sym);
    format_group_integer = new DecimalFormat("#,##0", df_sym);
    format_group_sinteger = new DecimalFormat("#,##0", df_sym);
    format_group_sinteger.setPositivePrefix("+");
    format_group_sdecimal.setPositivePrefix("+");
  }

  protected boolean showZero = true;
  protected boolean sign = false;
  protected String xtimes = "";

  protected String get_zero_decimal() {
    // if (sign) { // Why do we want +0. ??
    //    return zero_sdecimal;
    // }
    return zero_decimal;
  }

  // Analyzer double value
  public double doubleValue() {
    return 0.0;
  }

  // Analyzer printing format
  public abstract String toString();

  // Analyzer formatted printing format
  public String toFormString() {
    return toString();
  }

  // Time printing
  public String toTime(final double clock) {
    if (xtimes.compareTo("") != 0) {
      // for ratio comparison, don't rescale by clock
      return toString();
    } else {
      // is this code path ever reached? what is it for?
      return "";
    }
  }

  // Time formatted printing format
  public String toFormTime(final double clock) {
    return toTime(clock);
  }

  // Percent printing
  public String toPercent(final double total) {
    return "";
  }

  // Percent printing
  public final String percentToString(final double val) {
    if (val == 0.0) {
      return (showZero) ? zero_percent : quote_space;
    }
    return format_percent.format(val);
  }

  // Percent printing with quote
  public final String toPercentQuote(final double percent) {
    String s = toString();
    if (percent == 0.0) {
      if (showZero) {
        return String.format("%s (%6s%%)", toString(), zero_percent);
      }
      return toQuoteSpace();
    }
    return String.format("%s (%6.2f%%)", toString(), percent);
  }

  // Don't show percentage
  public final String toQuoteSpace() {
    String s = toString();
    return s + "          ";
  }

  // To show or not to show zero
  public final void showZero(boolean show) {
    showZero = show;
  }

  public final void showSign(boolean show) {
    sign = show;
  }

  public final void showXtimes(boolean show) {
    if (show) {
      xtimes = "x";
    } else {
      xtimes = "";
    }
  }

  /*
   * Updates Total and Max values.
   * Note: Total is not used, so only Maximum is updated.
   */
  public static Object[][] updateMaxValues(final Object[][] data, Object[][] maxValues) {
    if (maxValues == null) {
      maxValues = new Object[2][data.length];
    }
    if (maxValues[1].length != data.length) {
      maxValues = new Object[2][data.length];
    }

    String strTotal = "<Total>";
    String strLocalizedTotal = AnLocale.getString("<Total>");
    int totalColumn = -1;

    for (int i = 0; i < data.length; i++) {
      int maxlen = 0;
      Object obj = maxValues[1][i];
      if (obj != null) {
        maxlen = obj.toString().length();
      }
      Object[] d = data[i];
      for (int j = 0; j < d.length; j++) {
        int len = d[j].toString().length();
        if (maxlen < len) {
          maxlen = len;
          obj = d[j];
        }
      }
      maxValues[1][i] = obj;
      if (data[i] instanceof String[]) {
        String snames[] = (String[]) data[i];
        for (int j = 0; j < snames.length; j++) {
          if ((snames[j].equals(strLocalizedTotal)) || (snames[j].equals(strTotal))) {
            totalColumn = j;
            break;
          }
        }
      }
    }
    if ((totalColumn != -1)) {
      for (int i = 0; i < data.length; i++) {
        Object[] d = data[i];
        maxValues[0][i] = d[totalColumn];
      }
    }

    // make sure there are no null elements in maxValues
    for (int i = 0; i < data.length; i++) {
      if (maxValues[0][i] == null) {
        maxValues[0][i] = new AnDouble(0.0);
      }
      if (maxValues[1][i] == null) {
        maxValues[1][i] = "";
      }
    }
    return maxValues;
  }
}

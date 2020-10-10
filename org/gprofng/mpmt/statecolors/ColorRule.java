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

package org.gprofng.mpmt.statecolors;

import org.gprofng.mpmt.AnLocale;
import java.awt.Color;
import java.util.regex.Pattern;

// This class is immutable
public final class ColorRule {

  public static final String[] SETC_STR = { // YXXX indices must match SETC_* definitions below
    AnLocale.getString("Starts with"),
    AnLocale.getString("Contains"),
    AnLocale.getString("Ends with"),
    AnLocale.getString("Regular Exp."),
    AnLocale.getString("Matches exactly")
  };
  public static final int SETC_ALL = -1;
  public static final int SETC_START = 0;
  public static final int SETC_CONTAIN = 1;
  public static final int SETC_END = 2;
  public static final int SETC_REGEXP = 3;
  public static final int SETC_MATCHES = 4;

  private final int type; // SETC_* definition
  private final String text;
  private final Color color;
  private final boolean isDefault;
  protected final Pattern reg_exp; // may be null

  public ColorRule(Color _color, int _type, String _nametext, boolean _isDefault) {
    type = _type;
    text = _nametext;
    color = _color;
    isDefault = _isDefault;
    reg_exp = null;
  }

  public ColorRule(
      Color _color, int _type, String _nametext, boolean _isDefault, Pattern _reg_exp) {
    type = _type;
    text = _nametext;
    color = _color;
    isDefault = _isDefault;
    reg_exp = _reg_exp;
  }

  public int getType() {
    return type;
  }

  public String getText() {
    return text;
  }

  public Color getColor() {
    return color;
  }

  public boolean getIsDefault() {
    return isDefault;
  }

  public Pattern getRegExpr() {
    return reg_exp;
  }
}

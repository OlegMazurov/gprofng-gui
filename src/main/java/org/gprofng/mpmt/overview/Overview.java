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

package org.gprofng.mpmt.overview;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;

public class Overview {
  protected static final Color STATUS_BACKGROUND = Color.WHITE; // new Color(223,238,215);
  public static final Color METRICS_BACKGROUND = Color.WHITE; // new Color(241,237,186);
  protected static final Color VIEWS_BACKGROUND = Color.WHITE; // new Color(225,230,237);

  public static final Font defaultBoldFont = new JLabel().getFont().deriveFont(Font.BOLD);
  public static final Font defaultPlainFont = defaultBoldFont.deriveFont(Font.PLAIN);
}

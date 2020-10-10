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

package org.gprofng.mpmt.util.gui;

import javax.swing.*;

public final class AnTextIcon {
  private final String text;
  private final Icon icon;

  // Constructor
  public AnTextIcon(final String text, final Icon icon) {
    this.text = text;
    this.icon = icon;
  }

  // Get text
  public String getText() {
    return text;
  }

  // Get icon
  public Icon getIcon() {
    return icon;
  }
}

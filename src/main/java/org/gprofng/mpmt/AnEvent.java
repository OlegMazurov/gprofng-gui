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

import java.util.EventObject;

public final class AnEvent extends EventObject {
  public static final int EVT_COMPUTE =
      1; // AnTable, Settings: when double-clicking on already selected row in caller/callee,
  // Settings: when applying any change
  public static final int EVT_UPDATE = 2; // ColorMap: when channging colors, AnTable: when ????
  //    public final static int	EVT_NEWFMT  = 3;  // Settings: Formats have changed
  //    public final static int	EVT_NEWSRC  = 4;  // Settings: Search Path/Pathmap changed, AnTable:
  // when double-clicking in row
  //    public final static int	EVT_NEWDIS  = 5;  // Settings: Search Path/Pathmap changed
  //    public final static int	EVT_NEWTM   = 6;  // Settings: when timeline properties have changed
  //    public final static int	EVT_RUN     = 7;  // Settings: when applying any change
  public static final int EVT_SET = 8; // Antable: ?????
  public static final int EVT_SELECT = 9; // AnTable: when selecting row in AnTable (single/multi)
  public static final int EVT_SORT =
      10; // AnTable, Settings: when sorting column by clicking in table header
  //    public final static int	EVT_ORDER   = 11; // Settings: when switching column order in
  // AnTable
  public static final int EVT_SCROLL = 12; // AnTable: initial when Caller/Callee opened
  public static final int EVT_SWITCH = 13; // AnTable: when switching column order in AnTable
  public static final int EVT_RESET = 14; // AnTable: not used
  public static final int EVT_SETHEAD = 15; // AnTable: Set Head from Caller/Callee context menu
  public static final int EVT_SETTAIL = 16; // AnTable: Set Tail from Caller/Callee context menu
  public static final int EVT_BACK = 17; // AnTable: when clicking Forward
  public static final int EVT_FORWARD = 18; // AnTable: when clicking Backward
  public static final int EVT_COPY_ALL = 19; // AnTable: when clicking Copy All
  public static final int EVT_COPY_SEL = 20; // AnTable: when clicking Copy Selected
  public static final int EVT_RESIZE = 99; // Should not clash with display types

  private final int type, dsp_type;
  private final int value;
  private final Object aux;

  public AnEvent(final Object source, final int type, final int value, final Object aux) {
    super(source);

    this.type = type;
    this.value = value;
    this.dsp_type = AnDisplay.DSP_Null;
    this.aux = aux;
  }

  public AnEvent(
      final Object source, final int type, final int value, final int dsp_type, final Object aux) {
    super(source);

    this.type = type;
    this.dsp_type = dsp_type;
    this.value = value;
    this.aux = aux;
  }

  public int getType() {
    return type;
  }

  public int getDispType() {
    return dsp_type;
  }

  public int getValue() {
    return value;
  }

  public Object getAux() {
    return aux;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(type);

    return sb.toString();
  }
}

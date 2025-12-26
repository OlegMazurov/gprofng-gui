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


package org.gprofng.mpmt.event;

import java.util.EventObject;

public class AnSelectionEvent extends EventObject {
  private long[] selObj;
  private int type;
  private int subtype;
  private int version;

  public AnSelectionEvent(Object source, long[] selObj, int type, int subtype, int version) {
    super(source);
    this.selObj = selObj;
    this.type = type;
    this.subtype = subtype;
    this.version = version;
  }

  /**
   * @return the selObj
   */
  public long[] getSelObj() {
    return selObj;
  }

  /**
   * @return the type
   */
  public int getType() {
    return type;
  }

  /**
   * @return the subtype
   */
  public int getSubtype() {
    return subtype;
  }

  /**
   * @return the version
   */
  public int getVersion() {
    return version;
  }
}

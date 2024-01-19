/* Copyright (C) 2022-2024 Free Software Foundation

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

// Analyzer String object
public final class AnString extends AnObject implements Comparable {
  private String obj;

  // Constructor
  public AnString(String value) {
    obj = value;
  }

  // Analyzer String printing format
  public String toString() {
    return obj;
  }

  // As String.compareTo
  public int compareTo(Object o) {
    return obj.compareTo((String) o);
  }

  // Never used
  //    // Convert String[] to AnString[]
  //    public static AnString[] toArray(String[] list) {
  //	int		length   = list.length;
  //	AnString[]	new_list = new AnString[length];
  //
  //	for (int i = 0; i < length; i++)
  //	    new_list[i] = new AnString(list[i]);
  //
  //	return new_list;
  //    }
}

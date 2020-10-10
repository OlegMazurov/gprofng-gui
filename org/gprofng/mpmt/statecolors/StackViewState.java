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

// This class is immutable

// State represents each element in the callstack
public class StackViewState extends StackState {
  private final String pcname;
  private final long pcid; // Histable::DbeInstr* (NOT DbeInstr->id)

  // Constructor
  public StackViewState(String fname, String pcname, long fnumber, long pcid) {
    super(fname, fnumber);
    this.pcname = pcname;
    this.pcid = pcid;
  }

  // Gets pcname of function as shown in callstack
  public String getPCName() {
    return pcname;
  }

  public long getPC() {
    return pcid;
  }

  public Object clone() {
    StackViewState s = new StackViewState(super.getName(), pcname, super.getNumber(), pcid);
    return s;
  }

  public String toString() {
    return pcname;
  }
}

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


package org.gprofng.mpmt.picklist;

import java.util.ArrayList;
import java.util.List;

public class StringPickList extends PickList {
  /** Creates a new variable sized Picklist */
  public StringPickList() {
    super();
  }

  /** Creates a new Picklist with maximux 'max' elements. 'max' <= 0 means variable sized list. */
  public StringPickList(int max) {
    super(max);
  }

  public void addElement(String elem) {
    super.addElement(new StringPickListElement(elem));
  }

  public List<StringPickListElement> getStringElements() {
    List<StringPickListElement> list = new ArrayList<StringPickListElement>();
    List<PickListElement> mostRecentExperiments = getElements();
    for (PickListElement ple : mostRecentExperiments) {
      list.add((StringPickListElement) ple);
    }
    return list;
  }
}

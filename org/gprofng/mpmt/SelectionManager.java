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

import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.event.AnSelectionEvent;

public class SelectionManager {

  public void updateSelection(long[] selObjs, int type, int subtype, int version) {
    updateSummaryPanel(selObjs, type, subtype, version);
  }

  public void updateSelection(long selObj, int type, int subtype, int version) {
    long[] objs = new long[1];
    objs[0] = selObj;
    updateSummaryPanel(objs, type, subtype, version);
  }

  public void updateSelection(int[] selObj, int type, int subtype, int version) {
    int len = selObj.length;
    long[] objs = new long[len];
    for (int i = 0; i < len; i++) {
      objs[i] = (long) selObj[i];
    }
    updateSummaryPanel(objs, type, subtype, version);
  }

  public void updateSelection() {
    AnChangeEvent anChangeEvent = new AnChangeEvent(this, AnChangeEvent.Type.SELECTION_UPDATE);
    AnEventManager.getInstance().fireAnChangeEvent(anChangeEvent);
  }

  private void updateSummaryPanel(long[] selObjs, int type, int subtype, int version) {
    //        System.out.println("updateSummaryPanel: " + type + " " + subtype + " " + version);
    //        for (long l : selObjs) {
    //            System.out.println(" " + l);
    //        }
    AnSelectionEvent anSelectionEvent = new AnSelectionEvent(this, selObjs, type, subtype, version);
    AnChangeEvent anChangeEvent =
        new AnChangeEvent(anSelectionEvent, AnChangeEvent.Type.SELECTION_CHANGED);
    AnEventManager.getInstance().fireAnChangeEvent(anChangeEvent);
  }
}

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


package org.gprofng.mpmt.statecolors;

import org.gprofng.mpmt.AnWindow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Maintains a list of StackState. Given function id as input, performs IPC to look up function
 * names. This class can be used to cache function id names.
 */
public class StackStateList {
  private final AnWindow anWindow;
  private final HashMap<Long, StackState> function_htable; // unique StackStates; key=function_id

  public StackStateList(AnWindow _anWindow) {
    anWindow = _anWindow;
    function_htable = new HashMap();
  }

  public void clear() {
    synchronized (function_htable) {
      function_htable.clear();
    }
  }

  public StackState get(long fid) {
    synchronized (function_htable) {
      return function_htable.get(fid);
    }
  }

  public ArrayList<StackState> ipcAdd(Set<Long> newFids) { // IPC!
    ArrayList<Long> missing_fids = new ArrayList();

    synchronized (function_htable) {
      for (Long fid : newFids) {
        if (!function_htable.containsKey(fid)) {
          missing_fids.add(fid);
        }
      }
    }

    if (!missing_fids.isEmpty()) {
      // now go fetch the missing strings
      int size = missing_fids.size();
      final long[] fidArray = new long[size];
      int i = 0;
      for (Long iter : missing_fids) {
        fidArray[i] = iter.longValue();
        i++;
      }

      // get function names
      final String[] names = anWindow.getFuncNames(fidArray); // IPC!!

      ArrayList<StackState> missing = new ArrayList();
      i = 0;
      synchronized (function_htable) {
        for (Long iter : missing_fids) {
          long fid = iter.longValue();
          String name = names[i];
          StackState stackState = new StackState(name, fid);
          function_htable.put(fid, stackState);
          missing.add(stackState);
          i++;
        }
      }
      return missing;
    }
    return null;
  }
}

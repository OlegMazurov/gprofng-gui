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

import static java.util.concurrent.TimeUnit.*;

import org.gprofng.mpmt.AnEvent;
import org.gprofng.mpmt.AnListener;
import java.awt.Color;
import java.awt.Component;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

// Display alphabetically sorted StackStates as Colored boxes labeled with function names
public class ChooserStateView extends JList<StackState> implements AnListener {
  protected final StateColorMap color_map;
  private StateColorMap.ColorMapSnapshot stateColorSnapshot;
  private boolean show_duplicates; // one line for each func even if name is same
  private HashMap<Long, Integer> funcMap; // Maps Func #s to line in list
  private ArrayList<StackState> sortedData; // copy of original data
  private final Object updateLock = new Object();
  private boolean ignoreUpdates = false;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private List<Long> selectedFuncs;

  // Constructor
  public ChooserStateView(final StateColorMap color_map) {
    this.color_map = color_map;
    stateColorSnapshot = null;

    ListRenderer lrend;
    lrend = new ListRenderer();
    setCellRenderer(lrend);
    setBackground(lrend.getBackground());
    resetVals();
    color_map.addAnListener(this);
  }

  private void resetVals() {
    show_duplicates = false;
    funcMap = null;
    sortedData = null;
    selectedFuncs = new ArrayList<Long>();
  }

  // Set components (MUST BE CALLED ON AWT THREAD)
  private void setStackStates(Collection<StackState> states) {
    ArrayList<StackState> data = (states != null) ? new ArrayList(states) : new ArrayList();
    Collections.sort(
        data,
        new Comparator() {
          public int compare(Object a, Object b) {
            StackState aa, bb;
            aa = (StackState) a;
            bb = (StackState) b;
            return aa.getName().compareTo(bb.getName());
          }
        });
    sortedData = new ArrayList(data);

    Vector dataList = new Vector();
    funcMap = new HashMap(); // map func #s to dataList
    String recentName = null; // most recently seen name
    for (StackState state : sortedData) {
      if (show_duplicates || !state.getName().equals(recentName)) {
        dataList.add(state);
        recentName = state.getName();
      }
      funcMap.put(new Long(state.getNumber()), new Integer(dataList.size() - 1));
    }
    if (!isSelectionEmpty()) {
      List<StackState> list = getSelectedValuesList();
      selectedFuncs = new ArrayList();
      for (StackState state : list) {
        Long val = state.getNumber();
        selectedFuncs.add(val);
      }
    }
    setListData(dataList);
    if (selectedFuncs != null && !selectedFuncs.isEmpty()) {
      setSelectedFunctions(selectedFuncs);
    }
  }

  private void updateColors() {
    // only access stateColorSnapshot and setStackStates on AWT thread
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            StateColorMap.ColorMapSnapshot latest =
                color_map.checkForAllUpdates(stateColorSnapshot);
            if (stateColorSnapshot != latest) {
              stateColorSnapshot = latest;
              Collection<StackState> states = stateColorSnapshot.getStackStates();
              setStackStates(states);
              repaint();
            }
          }
        });
  }

  // color change(s); may be called extremely frequently
  @Override
  public void valueChanged(AnEvent ee) {
    synchronized (updateLock) {
      if (ignoreUpdates) {
        return;
      }
      // do an immediate update then ignore updates for a while
      ignoreUpdates = true;
      updateColors();
    }
    scheduler.schedule(
        new Runnable() {
          public void run() {
            synchronized (updateLock) {
              // after delay, do an update and reenable future updates
              ignoreUpdates = false;
              updateColors();
            }
          }
        },
        1000,
        MILLISECONDS); // max rate of refreshing is set here
  }

  // Set selected function (MUST BE CALLED ON AWT THREAD)
  public StackState getState(int ii) {
    return (StackState) this.getModel().getElementAt(ii);
  }

  // Set selected function (MUST BE CALLED ON AWT THREAD)
  public void setSelectedFunctions(List<Long> funcs) {
    selectedFuncs = funcs;
    if (funcMap == null) {
      return;
    }
    ListModel data = getModel();
    if (data == null) {
      return;
    }
    if (funcs.isEmpty()) {
      clearSelection();
      return;
    }
    int idxs[] = new int[funcs.size()];
    int ii = 0;
    for (Long func : funcs) {
      Integer idx = funcMap.get(func);
      if (idx == null) {
        continue;
      }
      idxs[ii++] = idx;
    }
    setSelectedIndices(idxs);
    if (idxs.length == 1) {
      ensureIndexIsVisible(idxs[0]);
    }
  }

  public void setSelectedFunction(long lfunc) {
    Long func = lfunc;
    List<Long> funcs = new ArrayList();
    funcs.add(func);
    setSelectedFunctions(funcs);
  }

  // Set selected functions (MUST BE CALLED ON AWT THREAD)
  public void setSelectionFunctions(HashSet<Long> funcList) {
    for (Long func : funcList) {
      setSelectedFunction(func);
    }
  }

  // List renderer
  protected class ListRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(
        JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      StackState state = (StackState) value;
      setText(state.getName());
      Color color = color_map.getFuncColor(state.getNumber());
      setIcon(new ImageIcon(StackState.createIcon(color)));
      return this;
    }
  }
}

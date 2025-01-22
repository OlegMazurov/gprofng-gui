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

package org.gprofng.mpmt.statecolors;

import org.gprofng.mpmt.AnEvent;
import org.gprofng.mpmt.AnListener;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.event.EventListenerList;

public abstract class ColorMap {

  public class ColorMapSnapshot {

    private final long versionNum;
    private final HashMap<Long, Color> colorMap;
    private final Collection<StackState> stackStates;

    ColorMapSnapshot(
        long _version, HashMap<Long, Color> _colorMap, HashMap<Long, StackState> _stackStateMap) {
      versionNum = _version;
      colorMap = (_colorMap != null) ? (HashMap<Long, Color>) _colorMap.clone() : null;
      stackStates = (_stackStateMap != null) ? new ArrayList(_stackStateMap.values()) : null;
    }

    protected long getVersion() {
      return versionNum;
    }

    public HashMap<Long, Color> getColorMap() {
      return colorMap;
    }

    public Collection<StackState> getStackStates() {
      return stackStates;
    }
  }

  private final ArrayList<ColorRule> activeRules;
  private final ArrayList<ColorRule> defaultRules;
  private final HashMap<Long, Color> colorMap; // Maps Func #s to Colors
  private final HashMap<Long, StackState> stackStateMap; // Maps Func #s to StackState (Names + ID)
  private long versionNumber; // tracks all changes

  // Constructor
  public ColorMap() {
    versionNumber = 0;
    colorMap = new HashMap();
    stackStateMap = new HashMap();
    activeRules = new ArrayList();
    defaultRules = new ArrayList();
  }

  public synchronized void reset() {
    updateVersion();
    colorMap.clear();
    stackStateMap.clear();
    activeRules.clear();
    defaultRules.clear();
  }

  protected abstract Color getBaseColor(String name);

  private Color computeColor(StackState state) {
    Color c = checkRules(state, activeRules);
    if (c == null) {
      c = checkRules(state, defaultRules);
    }
    if (c == null) {
      c = getBaseColor(state.getName());
    }
    return c;
  }

  private int addState(StackState state) {
    String name = state.getName();
    long number = state.getNumber();
    StackState tmp = stackStateMap.put(number, state);
    if (tmp != null) {
      if (tmp.getName().equals(name)) {
        return 0; // already seen this number/name combination; done!
      } else {
        ; // weird!  Someone used an alternate name for the function?
      }
    }
    Color c = computeColor(state);
    setFuncColor(state, c);
    return 1;
  }

  private Color checkRules(StackState state, List<ColorRule> rules) {
    for (int ii = rules.size() - 1; ii >= 0; ii--) {
      final ColorRule rule = rules.get(ii);
      final Pattern reg_exp = rule.reg_exp;
      final String str_class = rule.getText();
      final String name = state.getName();
      Color c = null;
      switch (rule.getType()) {
        case ColorRule.SETC_START:
          if (name.startsWith(str_class)) {
            c = rule.getColor();
          }
          break;
        case ColorRule.SETC_END:
          if (name.endsWith(str_class)) {
            c = rule.getColor();
          }
          break;
        case ColorRule.SETC_CONTAIN:
          if (name.indexOf(str_class) != -1) {
            c = rule.getColor();
          }
          break;
        case ColorRule.SETC_MATCHES:
          if (name.equals(str_class)) {
            c = rule.getColor();
          }
          break;
        case ColorRule.SETC_REGEXP:
          if (reg_exp != null && reg_exp.matcher(name).matches()) {
            c = rule.getColor();
          }
          break;
        case ColorRule.SETC_ALL:
          c = rule.getColor();
          break;
        default:
          return null; // weird!
      }

      if (c != null) {
        return c;
      }
    }
    return null;
  }

  // Set class to color: handles changes done through color chooser
  private void internalSetRule(ColorRule rule) {
    boolean clears_all = false;
    ColorRule newRule = rule;
    switch (rule.getType()) {
      case ColorRule.SETC_ALL:
        clears_all = true;
        break;
      case ColorRule.SETC_REGEXP:
        try {
          Pattern reg_exp = Pattern.compile(rule.getText());
          newRule =
              new ColorRule(
                  rule.getColor(), rule.getType(), rule.getText(), rule.getIsDefault(), reg_exp);
        } catch (PatternSyntaxException ex) {
          // YXXX warn user here
          return;
        }
        break;
      default:
        break;
    }
    ArrayList<ColorRule> selectedRules = rule.getIsDefault() ? defaultRules : activeRules;
    if (clears_all) {
      selectedRules.clear();
    }
    selectedRules.add(newRule);
  }

  // Set selected func to color
  private void setFuncColor(final StackState state, final Color color) {
    Color c = colorMap.put(state.getNumber(), color);
    if (c == null || !c.equals(color)) {
      ; // colors changed
    }
  }

  private void updateColors() {
    Collection<StackState> stackStates = stackStateMap.values();
    for (StackState state : stackStates) {
      Color color = computeColor(state);
      setFuncColor(state, color);
    }
  }

  private void updateVersion() { // may be called from any thread.
    versionNumber++;
    AnUtility.invokeLaterOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            fireAnEvent(new AnEvent(this, AnEvent.EVT_UPDATE, 0, null));
          }
        });
  }

  // =============== public functions, must be synchronized ===============
  public synchronized int addStates(Collection<StackState> states) {
    int numAdded = 0;
    for (StackState state : states) {
      numAdded += addState(state);
    }
    if (numAdded == 0) {
      return numAdded;
    }
    updateVersion();
    return numAdded;
  }

  public synchronized Color getFuncColor(long number) {
    Color c = colorMap.get(number);
    if (c == null) {
      c = Color.black; // weird, caller didn't add this function
    }
    return c;
  }

  public synchronized ColorMapSnapshot checkForColorUpdates(ColorMapSnapshot snapshot) {
    if (snapshot == null || snapshot.versionNum != versionNumber) {
      return new ColorMapSnapshot(versionNumber, colorMap, null);
    }
    return snapshot; // no changes
  }

  public synchronized ColorMapSnapshot checkForAllUpdates(ColorMapSnapshot snapshot) {
    if (snapshot == null || snapshot.versionNum != versionNumber) {
      return new ColorMapSnapshot(versionNumber, colorMap, stackStateMap);
    }
    return snapshot; // no changes
  }

  public synchronized List<ColorRule> getActiveRules() {
    return activeRules;
  }

  public synchronized void setRule(
      final Color color, final int type, String text, boolean is_default) {
    ColorRule rule = new ColorRule(color, type, text, is_default);
    internalSetRule(rule);
    updateColors();
    updateVersion();
  }

  /**
   * initialize color rules (no events)
   *
   * @param rules
   */
  public synchronized void initRules(final List<ColorRule> rules, boolean sendEvent) {
    activeRules.clear();
    for (ColorRule rule : rules) {
      internalSetRule(rule);
    }
    updateColors();
    if (sendEvent) {
      updateVersion();
    }
  }

  /**
   * set color rules and send events
   *
   * @param rules
   */
  public synchronized void setRules(final List<ColorRule> rules) {
    for (ColorRule rule : rules) {
      internalSetRule(rule);
    }
    updateColors();
    updateVersion();
  }

  // Set all to color
  public synchronized void setRuleAllFunctions(final Color color) {
    ColorRule rule = new ColorRule(color, ColorRule.SETC_ALL, null, false);
    internalSetRule(rule);
    updateColors();
    updateVersion();
  }

  // Reset all function color to default
  public synchronized void restoreToDefault() {
    activeRules.clear();
    updateColors();
    updateVersion();
  }

  // Listener to the event changed
  private final EventListenerList listenerList = new EventListenerList();

  // Add a listener to the list
  public void addAnListener(final AnListener listener) {
    listenerList.add(AnListener.class, listener);
  }

  // Fire AnEvent to the listener
  private void fireAnEvent(AnEvent event) {
    AnUtility.checkIfOnAWTThread(true);
    //        System.out.println("ColorMap fireAnEvent: " + event);
    Object[] listeners;
    listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == AnListener.class) {
        ((AnListener) listeners[i + 1]).valueChanged(event);
      }
    }
  }

  //    public void asyncUpdateFullColorMap() {
  //        AnUtility.dispatchOnAWorkerThread(new Runnable() {
  //            @Override
  //            public void run() {
  //                updateFullColorMapIPC();
  //            }
  //        }, "ColorMapUpdateFullColorMap");
  //    }

  public int updateFullColorMapIPC() {
    Object[] res = getCallTreeFuncsIPC();
    if (res == null) {
      return 0;
    }
    long[] funcs = (long[]) res[0];
    String[] names = (String[]) res[1];
    long[] functions = (long[]) res[2];
    List<StackState> states = new ArrayList();
    for (int ii = 0; ii < funcs.length; ii++) {
      if (names[ii] == null) {
        continue;
      }
      StackState state = new StackState(names[ii], functions[ii]); // name, Histable::Function*
      states.add(state);
    }
    int num_added = addStates(states);
    return num_added;
  }

  private Object[] getCallTreeFuncsIPC() {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getCallTreeFuncs");
      anWindow.IPC().send(0);
      Object[] res = (Object[]) anWindow.IPC().recvObject();
      return res;
    }
  }

  // <newFunctions> is Histable::Function*
  public List<StackState> addFunctionsIPC(Set<Long> newFunctions) { // IPC!
    Set<Long> missing_fids = new HashSet<>();

    synchronized (this) {
      for (Long fid : newFunctions) {
        if (!stackStateMap.containsKey(fid)) {
          missing_fids.add(fid);
        }
      }
    }
    if (missing_fids.isEmpty()) {
      return null;
    }
    // now go fetch the missing strings
    int size = missing_fids.size();
    final long[] funcs = new long[size];
    int i = 0;
    for (Long iter : missing_fids) {
      funcs[i++] = iter.longValue();
    }

    // get function names
    AnWindow anWindow = AnWindow.getInstance();
    final String[] names = anWindow.getFuncNames(funcs); // IPC!!

    ArrayList<StackState> missing = new ArrayList();
    for (int ii = 0; ii < funcs.length; ii++) {
      long func = funcs[ii];
      String name = names[ii];
      StackState stackState = new StackState(name, func);
      missing.add(stackState);
    }
    addStates(missing); // internally synchronized
    return missing;
  }
}

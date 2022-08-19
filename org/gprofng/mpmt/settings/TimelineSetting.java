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


package org.gprofng.mpmt.settings;

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.experiment_props.DataDescriptor;
import org.gprofng.mpmt.experiment_props.ExperimentProperties;
import org.gprofng.mpmt.experiment_props.Experiments;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;
import org.gprofng.mpmt.settings.Settings.TLStack_align;
import org.gprofng.mpmt.timeline2.TimelineDraw;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimelineSetting extends Setting {
  public class EntityProp {
    public final int prop_id; // PROP_* value
    public final String prop_name; // PROP_* name
    public final String prop_uname; // user name
    public final String prop_cmd; // .er.rc command

    public EntityProp(int id, String name, String uname, String cmd) {
      prop_id = id;
      prop_name = name;
      prop_uname = uname;
      prop_cmd = cmd;
    }
  }

  private static final String default_prop_name = "THRID"; // Default // FIXUP: UGLY!!!!

  private List<EntityProp> tl_entity_props = new ArrayList();
  private List settings;

  private long tldata_hidden_bitmask; // states of checkboxes
  private List<String> tldata_unames; // names shown for checkboxes
  private List<String> tldata_cnames; // .er.rc tldata cmds for checkboxes
  private Set<String> tldata_unused_hidden; // not-yet-used .er.rc fields
  private int tldata_names_version; // tracks changes to timeline Data option updates
  private String tldata_cmd; // .er.rc command line, not including "tldata"
  private static String tldata_cmd_default = "all";

  public TimelineSetting() {
    // XXX: why are PROP_* values hard coded here?  // tl_entity_prop_* should be set only
    // once per gp-display-text connect
    List<Integer> tl_entity_prop_ids =
        Arrays.asList(
            Settings.PROP_NONE,
            Settings.PROP_NONE,
            Settings.PROP_NONE,
            Settings.PROP_NONE); // dbe property ID for above entities
    List<String> tl_entity_prop_names =
        Arrays.asList("LWPID", "THRID", "CPUID", "EXPID"); // names for above entities (???)
    List<String> tl_entity_prop_unames =
        Arrays.asList("LWP", "Thread", "CPU", "Process"); // user-visible names for above entities
    List<String> tl_entity_prop_cmd_names =
        Arrays.asList(
            "lwp", "thread", "cpu", "experiment"); // .er.rc command name for above entities
    for (int ii = 0; ii < tl_entity_prop_ids.size(); ii++) {
      tl_entity_props.add(
          new EntityProp(
              tl_entity_prop_ids.get(ii),
              tl_entity_prop_names.get(ii),
              tl_entity_prop_unames.get(ii),
              tl_entity_prop_cmd_names.get(ii)));
    }
    tldata_hidden_bitmask = 0;
    tldata_unames = new ArrayList();
    tldata_cnames = new ArrayList();
    tldata_unused_hidden = new HashSet();
    tldata_names_version = 0;
    tldata_cmd = tldata_cmd_default;
  }

  private void init(Object originalSource, List settings) {
    // if called externally, add code to update tldata_* state
    this.settings = settings;
    fireChangeEvent(originalSource, settings);
  }

  /** Called from Settings Save */
  public void set(Object originalSource, List newSettings, boolean changed) {
    if (changed) {
      setValueAndFireChangeEvent(originalSource, this, newSettings);
    }
  }

  /*
   * Called from import settings
   */
  public void set(
      Object originalSource,
      String groupDataByButtonName,
      int stackAlignNo,
      int stackDepth,
      int stackFramePixels,
      boolean showEventStates,
      boolean showEventDensity) {
    List settingsCopy = (List) AnWindow.getInstance().getSettings().getTimelineSettings(true);

    //        setProps(tl_entprops);
    //        tldata_unused_hidden = parseTLDataCmd(in_tldata_cmd);
    //        resetTLDataNames(); // relies on tldata_unused_hidden

    // Find the button number
    int groupDataByButtonNum = 1; // FIXUP: why 1?

    for (int i = 0; i < tl_entity_props.size(); i++) {
      if (tl_entity_props.get(i).prop_name.equalsIgnoreCase(groupDataByButtonName)) {
        groupDataByButtonNum = i;
        break;
      }
    }
    // Construct the StackAlign
    Settings.TLStack_align stackAlign = Settings.TLStack_align.find(stackAlignNo);

    settingsCopy.set(0, groupDataByButtonName);
    settingsCopy.set(1, groupDataByButtonNum);
    settingsCopy.set(2, stackAlign);
    settingsCopy.set(3, stackDepth);
    settingsCopy.set(4, Long.valueOf(getTLDataHiddenMask()));
    settingsCopy.set(5, getTLDataCNames());
    settingsCopy.set(6, getTLDataUNames());
    settingsCopy.set(7, Integer.valueOf(getTLDataNamesVersion()));
    settingsCopy.set(8, showEventStates);
    settingsCopy.set(9, showEventDensity);
    settingsCopy.set(10, stackFramePixels);
    settingsCopy.set(11, tldata_cmd);

    set(originalSource, settingsCopy, true);
  }

  /** Called when new experiment has been loaded (after processing .er.rc) */
  private void setProps(Object[] tl_entprops) {
    int[] tl_entprop_ids;
    String[] tl_entprop_names;
    String[] tl_entprop_unames;
    String[] tl_entprop_cnames;

    tl_entprop_ids = (int[]) tl_entprops[0];
    tl_entprop_names = (String[]) tl_entprops[1];
    tl_entprop_unames = (String[]) tl_entprops[2];
    tl_entprop_cnames = (String[]) tl_entprops[3];

    // first item in list is "unknown" propId==0; skip it
    tl_entity_props = new ArrayList();
    for (int i = 1; tl_entprop_ids != null && i < tl_entprop_ids.length; i++) {
      int thisId = tl_entprop_ids[i];
      String thisName = tl_entprop_names[i];
      String thisUname = tl_entprop_unames[i];
      String thisCname = tl_entprop_cnames[i];
      tl_entity_props.add(new EntityProp(thisId, thisName, thisUname, thisCname));
    }
  }

  /** Called once right after new experiment has been loaded (after processing .er.rc) */
  private boolean resetTLDataNames() {
    Experiments experiments = AnWindow.getInstance().getExperimentProperties();
    // determine unique "data types" shown in Presentation Timeline Tab
    final HashSet<String> uniqueNames = new HashSet();
    final ArrayList<String> dataCNames = new ArrayList();
    final ArrayList<String> dataUNames = new ArrayList();
    for (ExperimentProperties exp : experiments.getAllExperimentProperties()) {
      for (DataDescriptor dataD : exp.getTstampDataDescriptors()) {
        int auxmax = dataD.getAuxCount();
        if (auxmax == 0) {
          auxmax = 1; // loop only once
        }
        for (int aux = 0; aux < auxmax; aux++) {
          String cname_tmp = dataD.getErRcTLDataCmdName(aux);
          String uname_tmp = dataD.getLongUName(aux);
          if (cname_tmp == null || cname_tmp.isEmpty()) {
            continue;
          }
          if (uname_tmp == null || uname_tmp.isEmpty()) {
            continue;
          }
          if (uniqueNames.add(uname_tmp)) {
            dataCNames.add(cname_tmp);
            dataUNames.add(uname_tmp);
          }
        }
      }
    }
    boolean names_changed = resetTLDataNames(dataCNames, dataUNames);
    return names_changed;
  }

  private boolean resetTLDataNames(
      final ArrayList<String> newCmdNames, final ArrayList<String> newUserNames) {
    boolean namesUpdated = false;

    {
      if (tldata_cnames.size() != newCmdNames.size()) {
        namesUpdated = true;
      } else {
        // old and new sizes are the same, maybe we have a perfect match
        for (int ii = 0; ii < newCmdNames.size(); ii++) {
          String newname = newCmdNames.get(ii);
          String oldname = tldata_cnames.get(ii);
          if (!newname.equals(oldname)) {
            // something changed
            namesUpdated = true;
            break;
          }
        }
      }

      final boolean bitmaskNeedsUpdate;
      if (tldata_hidden_bitmask == 0 && tldata_unused_hidden.isEmpty()) {
        bitmaskNeedsUpdate = false;
      } else if (!namesUpdated) {
        bitmaskNeedsUpdate = false;
      } else {
        bitmaskNeedsUpdate = true;
      }

      if (bitmaskNeedsUpdate) {
        // update tldata_unused_cnames in include all disabled items
        for (int jj = 0; jj < tldata_cnames.size(); jj++) {
          String currCmd = tldata_cnames.get(jj);
          long currBit = (tldata_hidden_bitmask & (1L << jj));
          if (currBit != 0) {
            tldata_unused_hidden.add(currCmd);
          }
        }

        // scan new names to see which should be disabled
        long new_hidden_mask = 0;
        for (int ii = 0; ii < newCmdNames.size(); ii++) {
          String newCname = newCmdNames.get(ii);
          for (String disabledName : tldata_unused_hidden) {
            if (newCname.equals(disabledName)) {
              new_hidden_mask |= (1L << ii);
              tldata_unused_hidden.remove(disabledName);
              break;
            }
          }
        }
        tldata_hidden_bitmask = new_hidden_mask;
      }
      if (namesUpdated) {
        tldata_cnames = Collections.unmodifiableList(newCmdNames);
        tldata_unames = Collections.unmodifiableList(newUserNames);
        tldata_names_version++;
      }
      if (namesUpdated || bitmaskNeedsUpdate) {
        updateTLDataCmd(tldata_hidden_bitmask);
      }
    }
    return namesUpdated;
  }

  private static Set<String> parseTLDataCmd(String new_tldata_cmd) {
    // <new_tldata_cmd> == text that follows the .er.rc "tldata" command
    Set<String> cnamesToHide = new HashSet();
    {
      if (new_tldata_cmd == null || new_tldata_cmd.length() == 0) {
        return cnamesToHide;
      }
      new_tldata_cmd = new_tldata_cmd.toLowerCase();
      String mcmds[] = new_tldata_cmd.split(":");
      if (mcmds == null) {
        return cnamesToHide;
      }

      for (int ii = 0; ii < mcmds.length; ii++) {
        String this_cmd = mcmds[ii];
        if (this_cmd.equals(Settings.TLData_type.TL_ALL.getTLDataBaseCmd())) {
          // nothing to do (only saving "!" items in list)
        } else if (this_cmd.startsWith("!")) {
          this_cmd = this_cmd.substring(1); // remove !
          if (this_cmd.length() > 0) {
            cnamesToHide.add(this_cmd);
          }
        }
      }
    }
    return cnamesToHide;
  }

  public long setTLDataVisibility_edt(Set<String> allCmds, Set<String> disabledCmds) {
    long specified_mask = 0;
    long new_mask = 0;
    int num_disabled = 0;
    for (int jj = 0; jj < tldata_cnames.size(); jj++) {
      String currCmd = tldata_cnames.get(jj);
      if (allCmds.contains(currCmd)) {
        specified_mask |= (1L << jj);
      }
    }
    for (int jj = 0; jj < tldata_cnames.size(); jj++) {
      String currCmd = tldata_cnames.get(jj);
      if (disabledCmds.contains(currCmd)) {
        new_mask |= (1L << jj);
        num_disabled++;
      }
    }
    if (num_disabled != disabledCmds.size()) {
      int ii = 0; // weird!
    }
    long original_bitmask = tldata_hidden_bitmask;
    tldata_hidden_bitmask &= ~specified_mask;
    tldata_hidden_bitmask |= new_mask;
    if (original_bitmask != tldata_hidden_bitmask) {
      updateTLDataCmd(tldata_hidden_bitmask);
    }
    return tldata_hidden_bitmask;
  }

  public long getTLDataHiddenMask() {
    return tldata_hidden_bitmask;
  }

  public void setTLDataHiddenMask(long newMask) {
    long old_mask = tldata_hidden_bitmask;
    tldata_hidden_bitmask = newMask;
    if (old_mask != newMask) {
      updateTLDataCmd(tldata_hidden_bitmask);
    }
  }

  public int getTLDataNamesVersion() {
    return tldata_names_version;
  }

  public List getTLDataUNames() {
    return tldata_unames;
  }

  public List getTLDataCNames() {
    return tldata_cnames;
  }

  // only called when there is a change:
  private String updateTLDataCmd(long hidden_mask) {
    // update tldata_cmd
    String new_tldata_cmd = tldata_cmd_default;
    for (int ii = 0; ii < tldata_cnames.size(); ii++) {
      long disable = ((1L << ii) & hidden_mask);
      if (disable != 0) {
        new_tldata_cmd += ":!" + tldata_cnames.get(ii);
      }
    }
    tldata_cmd = new_tldata_cmd;
    return tldata_cmd;
  }

  /*
   * Called when new experiment has been loaded (after processing .er.rc)
   */
  public void init(
      Object originalSource,
      Object[] tl_entprops,
      String in_tldata_cmd,
      String groupDataByButtonName,
      int stackAlignNo,
      int stackDepth,
      int stackFramePixels,
      boolean showEventStates,
      boolean showEventDensity) {
    List settingsCopy = (List) AnWindow.getInstance().getSettings().getTimelineSettings(true);

    setProps(tl_entprops);
    tldata_unused_hidden = parseTLDataCmd(in_tldata_cmd);
    resetTLDataNames(); // relies on tldata_unused_hidden

    // Find the button number
    int groupDataByButtonNum = 1; // FIXUP: why 1?

    for (int i = 0; i < tl_entity_props.size(); i++) {
      if (tl_entity_props.get(i).prop_name.equalsIgnoreCase(groupDataByButtonName)) {
        groupDataByButtonNum = i;
        break;
      }
    }
    // Construct the StackAlign
    Settings.TLStack_align stackAlign = Settings.TLStack_align.find(stackAlignNo);

    settingsCopy.set(0, groupDataByButtonName);
    settingsCopy.set(1, groupDataByButtonNum);
    settingsCopy.set(2, stackAlign);
    settingsCopy.set(3, stackDepth);
    settingsCopy.set(4, Long.valueOf(getTLDataHiddenMask()));
    settingsCopy.set(5, getTLDataCNames());
    settingsCopy.set(6, getTLDataUNames());
    settingsCopy.set(7, Integer.valueOf(getTLDataNamesVersion()));
    settingsCopy.set(8, showEventStates);
    settingsCopy.set(9, showEventDensity);
    settingsCopy.set(10, stackFramePixels);
    settingsCopy.set(11, tldata_cmd);

    init(originalSource, settingsCopy);
  }

  private List getSettings() {
    return settings;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.TIMELINE;
  }

  @Override
  Object getValue() {
    return settings;
  }

  @Override
  void setValue(Object newValue) { // might not be EDT!!
    this.settings = (List) newValue;
  }

  public TLStack_align getStack_align() {
    if (settings != null) {
      return (TLStack_align) settings.get(2);
    } else {
      return getDefaultStack_align();
    }
  }

  public int getTLStackDepth() {
    if (settings != null) {
      int stackDepth = (Integer) settings.get(3);
      return stackDepth;
    } else {
      return getDefaultStackDepth();
    }
  }

  public int getTLStackFramePixels() {
    if (settings != null) {
      int stackDepth = (Integer) settings.get(10);
      return stackDepth;
    } else {
      return getDefaultStackFramePixels();
    }
  }

  public int getStackAlign() {
    int value = getStack_align().getValue();
    return value;
  }

  public boolean getShowEventStates() {
    if (settings != null) {
      Boolean show = (Boolean) settings.get(8);
      return show;
    } else {
      return getTimelineShowEventStatesDefault();
    }
  }

  public boolean getShowEventDensity() {
    if (settings != null) {
      Boolean show = (Boolean) settings.get(9);
      return show;
    } else {
      return getTimelineShowEventDensityDefault();
    }
  }

  public static int getDefaultStackDepth() {
    return TimelineDraw.TIMELINE_DEFAULT_VZOOM_LEVEL; // Default
  }

  public static int getDefaultStackFramePixels() {
    return Settings.TIMELINE_DEFAULT_STACK_FRAME_PIXELS; // Default
  }

  public static boolean getTimelineShowEventStatesDefault() {
    return true; // Default
  }

  public static boolean getTimelineShowEventDensityDefault() {
    return false; // Default
  }

  public void setTLStackDepth(Object originalSource, int stackDepth) {
    List settingsCopy = (List) AnWindow.getInstance().getSettings().getTimelineSettings(true);

    if (stackDepth > TimelineDraw.TIMELINE_MAX_VZOOM_LEVEL) {
      stackDepth = TimelineDraw.TIMELINE_MAX_VZOOM_LEVEL;
    }
    boolean changed = false;
    if ((Integer) (settingsCopy.get(3)) != stackDepth) {
      changed = true;
    }
    settingsCopy.set(3, stackDepth);

    set(originalSource, settingsCopy, changed);
  }

  public void setTLStackFramePixels(Object originalSource, int stackFramePixels) {
    List settingsCopy = (List) AnWindow.getInstance().getSettings().getTimelineSettings(true);

    boolean changed = false;
    if ((Integer) (settingsCopy.get(10)) != stackFramePixels) {
      changed = true;
    }
    settingsCopy.set(10, stackFramePixels);

    set(originalSource, settingsCopy, changed);
  }

  public void setTLGroupDataBySelectedButtonIndex(
      Object originalSource, int new_tl_entity_button_num) {
    List settingsCopy = (List) AnWindow.getInstance().getSettings().getTimelineSettings(true);
    if (new_tl_entity_button_num < 0 || new_tl_entity_button_num >= tl_entity_props.size()) {
      return; // weird
    }
    String tl_entity_prop_name = tl_entity_props.get(new_tl_entity_button_num).prop_name;

    boolean changed = false;
    String oldName = (String) (settingsCopy.get(0));
    if (!(tl_entity_prop_name.equalsIgnoreCase(oldName))) {
      changed = true;
    } else if ((Integer) (settingsCopy.get(1)) != new_tl_entity_button_num) {
      changed = true;
    }

    settingsCopy.set(0, tl_entity_prop_name);
    settingsCopy.set(1, new_tl_entity_button_num);

    set(originalSource, settingsCopy, changed);
  }

  public static String getDefaultGroupDataByButtonName() {
    return default_prop_name;
  }

  public static String getDefaultTLDataCmd() {
    return tldata_cmd_default;
  }

  public static int getDefaultStackAlign() {
    return getDefaultStack_align().getValue();
  }

  private static TLStack_align getDefaultStack_align() {
    return TLStack_align.TLSTACK_ALIGN_ROOT;
  }

  public String getGroupDataByButtonName() {
    if (settings != null) {
      return (String) (settings.get(0));
    } else {
      return getDefaultGroupDataByButtonName();
    }
  }

  public String getTLDataCmd() {
    if (settings != null) {
      return tldata_cmd; // tldata_cmd gets updated dynamically...
      // YXXX FIXUP, not sure what to do - should we update tldata_cmd whenever an TL event updates
      // settings.get(11)?
      //            String tmp_tldata_cmd = (String)settings.get(11);
      //            return tmp_tldata_cmd;
    } else {
      return getDefaultTLDataCmd();
    }
  }

  public String getFullTLDataCmd() {
    // include old settings previously seen, but not applicable to this experiment
    String fullCmd = tldata_cmd;
    for (String unused_cname : tldata_unused_hidden) {
      fullCmd += ":!" + unused_cname;
    }
    return fullCmd;
  }

  public int getTl_entity_selected_btn() {
    if (settings != null) {
      return (Integer) (settings.get(1));
    } else {
      return 1; // Default // FIXUP: UGLY!!!!
    }
  }

  public EntityProp getTl_entity_prop(int buttonIdx) {
    if (buttonIdx < 0 || buttonIdx >= tl_entity_props.size()) {
      return null; // weird
    }
    return tl_entity_props.get(buttonIdx);
  }

  public static Object[] ipcGetTLValue() {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("getTLValue");
      window.IPC().send(0);
      return (Object[]) window.IPC().recvObject();
    }
  }
  /**
   * Send request to get TLValue. Non-blocking IPC call. Caller should call ipcResult.getObjects()
   * to get the result
   *
   * @return IPCResult
   */
  public static IPCResult ipcGetTLValueRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getTLValue");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // Object[] result = ipcResult.getObjects() // blocking
    return ipcResult;
  }

  // YXXX TBR?  Remember to clean back-end.
  private static void ipcSetTLValue(
      final String tldata_cmd, int entity_prop_id, int align, int depth) {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("setTLValue");
      window.IPC().send(0);
      window.IPC().send(tldata_cmd);
      window.IPC().send(entity_prop_id);
      window.IPC().send(align);
      window.IPC().send(depth);
      window.IPC().recvString(); // synchronize
    }
  }

  public static Object[] ipcGetEntityProps() {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("getEntityProps");
      window.IPC().send(0);
      return (Object[]) window.IPC().recvObject();
    }
  }
  /**
   * Send request to get EntityProps. Non-blocking IPC call. Caller should call
   * ipcResult.getObjects() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult ipcGetEntityPropsRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getEntityProps");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // Object[] result = ipcResult.getObjects() // blocking
    return ipcResult;
  }
}

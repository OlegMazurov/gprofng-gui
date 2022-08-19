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

import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.AnWindow.AnDispTab;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.MemoryIndexObjectView;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.metrics.MetricNode;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

public class ViewsSetting extends Setting {

  public class View {

    private AnDispTab anDispTab;
    private boolean selected;

    private View(AnDispTab anDispTab, boolean selected) {
      this.anDispTab = anDispTab;
      this.selected = selected;
    }

    /**
     * @return the anDispTab
     */
    public AnDispTab getAnDispTab() {
      return anDispTab;
    }

    /**
     * @return the selected
     */
    public boolean isSelected() {
      return selected;
    }

    /**
     * @param selected the selected to set
     */
    private void setSelected(boolean selected) {
      this.selected = selected;
    }
  }

  public class Setting {

    private List<View> viewList;
    private String preferredViewName;

    private Setting() {
      reset();
    }

    private void reset() {
      viewList = new ArrayList<View>();
      preferredViewName = null;
    }

    private void addView(AnDispTab anDispTab, boolean selected) {
      View view = new View(anDispTab, selected);
      getViewList().add(view);
    }

    private void addViews(List<AnDispTab> list, boolean[] selected) {
      for (int i = 0; i < list.size(); i++) {
        addView(list.get(i), selected[i]);
      }
    }

    private void addViews(List<View> list) {
      viewList.addAll(list);
    }

    private void setPreferredViewName(String selectedView) {
      this.preferredViewName = selectedView;
    }

    public String getPreferredViewName() {
      return preferredViewName;
    }

    private List<View> getViewList() {
      return viewList;
    }

    private void setSelected(String name, boolean selected) {
      View view = findView(name);
      if (view != null) {
        view.setSelected(selected);
      }
    }

    private View findView(String name) {
      for (View view : viewList) {
        if (view.getAnDispTab().getTCmd().equals(name)) {
          return view;
        }
      }
      return null;
    }

    private View findView(int type) {
      for (View view : viewList) {
        if (view.getAnDispTab().getTType() == type) {
          return view;
        }
      }
      return null;
    }

    private void toggleTab(String cmd) {
      View view = findView(cmd);
      if (view != null) {
        view.setSelected(!view.isSelected());
      }
    }

    private Setting copy() {
      Setting settingCopy = new Setting();
      for (View view : viewList) {
        settingCopy.addView(view.getAnDispTab(), view.isSelected());
      }
      settingCopy.setPreferredViewName(getPreferredViewName());
      return settingCopy;
    }

    private List<View> getSelectedViews() {
      List<View> list = new ArrayList<View>();
      for (View view : viewList) {
        if (view.isSelected()) {
          list.add(view);
        }
      }
      return list;
    }

    private List<View> getStaticViews() {
      List<View> list = new ArrayList<View>();
      for (View view : viewList) {
        if (view.getAnDispTab().getTType() == AnDisplay.DSP_Welcome
            || view.getAnDispTab().getTType() == AnDisplay.DSP_Overview) {
          list.add(view);
        }
      }
      return list;
    }

    private List<View> getStandardViews() {
      List<View> list = new ArrayList<View>();
      for (View view : viewList) {

        if (view.getAnDispTab().getTType() != AnDisplay.DSP_Welcome
            && view.getAnDispTab().getTType() != AnDisplay.DSP_Overview
            && view.getAnDispTab().getTType() != AnDisplay.DSP_IndexObject
            && view.getAnDispTab().getTType() != AnDisplay.DSP_MemoryObject) {
          list.add(view);
        }
      }
      return list;
    }

    private List<View> getIndexViews() {
      List<View> list = new ArrayList<View>();
      for (View view : viewList) {
        if (view.getAnDispTab().getTType() == AnDisplay.DSP_IndexObject) {
          list.add(view);
        }
      }
      return list;
    }

    private List<View> getMemoryViews() {
      List<View> list = new ArrayList<View>();
      for (View view : viewList) {
        if (view.getAnDispTab().getTType() == AnDisplay.DSP_MemoryObject) {
          list.add(view);
        }
      }
      return list;
    }

    public View getFirstDataView() {
      View dataView = null;
      for (View view : viewList) {
        if (view.getAnDispTab().getTComp().getType() > 0
            && view.getAnDispTab().getTComp().getType()
                != AnDisplay.DSP_Overview) { // <==== FIXUP: Is this the best check
          if (view.isSelected()) {
            dataView = view;
            break;
          }
        }
      }
      return dataView;
    }

    public void debug() {
      System.out.println("\nViewsSetting:");
      System.out.println("PreferredViewName: " + getPreferredViewName());
      int c = 0;
      for (View view : viewList) {
        System.out.println(
            c++
                + ": "
                + view.getAnDispTab().getTName()
                + " "
                + view.getAnDispTab().getTType()
                + " "
                + view.isSelected());
      }
    }
  }

  public static class CustomObject {

    private String name;
    private String formula;
    private String shortDesc;
    private String longDesc;

    public CustomObject(String name, String formula, String shortDesc, String longDesc) {
      this.name = name;
      this.formula = formula;
      this.shortDesc = shortDesc;
      this.longDesc = longDesc;
    }

    public String getName() {
      return name;
    }

    public String getFormula() {
      return formula;
    }

    public String getShortDesc() {
      return shortDesc;
    }

    public String getLongDesc() {
      return longDesc;
    }
  }

  private Setting setting = new Setting();
  private List<UserPref.ViewPanelOrder> savedViewOrderList;
  private String[] availableMachineModels = null;
  private String machineModel = null;
  private List<CustomObject> customIndexObjects = null;
  private List<CustomObject> customMemoryObjects = null;

  private int[] viewMetricListIdMap;

  public ViewsSetting() {
    // FIXUP: hardcode the table for now. Should come from backend
    viewMetricListIdMap = new int[AnDisplay.DSP_MAX + 1];
    for (int i = 0; i < viewMetricListIdMap.length; i++) {
      viewMetricListIdMap[i] = MetricNode.ALL_LIST_ID;
    }
    viewMetricListIdMap[AnDisplay.DSP_MemoryObject] = MetricNode.DATASPACE_HWC_LIST_ID;
    viewMetricListIdMap[AnDisplay.DSP_DataObjects] = MetricNode.DATASPACE_HWC_LIST_ID;
    viewMetricListIdMap[AnDisplay.DSP_DataLayout] = MetricNode.DATASPACE_HWC_LIST_ID;
    viewMetricListIdMap[AnDisplay.DSP_IO] = MetricNode.IO_LIST_ID;
    viewMetricListIdMap[AnDisplay.DSP_Heap] = MetricNode.HEAP_LIST_ID;
  }

  public int getViewMetricListIdMapping(int dtype) {
    if (dtype >= 0 && dtype <= AnDisplay.DSP_MAX) {
      return viewMetricListIdMap[dtype];
    } else {
      return MetricNode.ALL_LIST_ID;
    }
  }

  public void debug() {
    setting.debug();
  }

  private void set(Object originalSource, Setting newSetting) {
    boolean changed = false;
    List<View> oldList = setting.getViewList();
    List<View> newList = newSetting.getViewList();
    if (oldList.size() != newList.size()) {
      changed = true;
    } else {
      for (int i = 0; i < oldList.size(); i++) {
        if (oldList.get(i).isSelected() != newList.get(i).isSelected()) {
          changed = true;
        }
      }
    }
    if (changed) {
      setValueAndFireChangeEvent(originalSource, this, newSetting);
    }
  }

  /*
   * Called when custom index/memory objects added
   */
  public void init(
      Object originalSource, List<AnDispTab> list, boolean[] selected, String preferredViewName) {
    Setting newSetting = new Setting();
    newSetting.addViews(list, selected);
    newSetting.setPreferredViewName(preferredViewName);

    setting = newSetting;
  }

  /*
   * Called when experiment(s) loaded
   */
  public void init(
      Object originalSource,
      List<UserPref.ViewPanelOrder> viewOrderList,
      String[][] groups,
      Object[] standardViews,
      boolean[] standardViewsSelected,
      Object[] indexViews,
      boolean[] indexViewsSelected,
      Object[] memoryViews,
      boolean[] memoryViewsSelected,
      String[] availableMachineModels,
      String machineModel,
      List<CustomObject> customIndexObjects,
      List<CustomObject> customMemoryObjects) {
    AnWindow window = AnWindow.getInstance();
    this.savedViewOrderList = viewOrderList;
    this.availableMachineModels = availableMachineModels;
    this.machineModel = machineModel;
    this.customIndexObjects = customIndexObjects;
    this.customMemoryObjects = customMemoryObjects;
    String preferredViewName;
    AnDispTab[] staticObjs;
    boolean[] staticSelected;
    List<AnDispTab> listStatic;
    List<AnDispTab> listStandard;
    List<AnDispTab> listIndex;
    List<AnDispTab> listMemory;

    if (groups.length == 0) {
      staticObjs =
          new AnDispTab[] {window.new AnDispTab(AnDisplay.DSP_Welcome, "welcome", null, null)};
      staticSelected = new boolean[] {true};
      listStatic = addStaticObjects(staticObjs);
      listStandard = new ArrayList<AnDispTab>();
      standardViewsSelected = new boolean[0];
      listIndex = new ArrayList<AnDispTab>();
      indexViewsSelected = new boolean[0];
      listMemory = new ArrayList<AnDispTab>();
      memoryViewsSelected = new boolean[0];
      preferredViewName = staticObjs[0].getTCmd();
    } else {
      staticObjs =
          new AnDispTab[] {
            window.new AnDispTab(AnDisplay.DSP_Welcome, "welcome", null, null),
            window.new AnDispTab(AnDisplay.DSP_Overview, "overview", null, null)
          };
      staticSelected = new boolean[] {true, true};
      listStatic = addStaticObjects(staticObjs);
      listStandard = addStandardObjects(standardViews);
      listIndex = addIndexObjects(indexViews);
      listMemory = addMemoryObjects(memoryViews);
      preferredViewName = staticObjs[1].getTCmd();
    }

    // Hack to enable Heap view if available. Should be done in the backend.
    int index = 0;
    for (AnDispTab adt : listStandard) {
      if (adt.getTType() == AnDisplay.DSP_Heap) {
        if (!standardViewsSelected[index]) {
          standardViewsSelected[index] = true;
        }
        break;
      }
      index++;
    }
    // Hack end

    Setting newSetting = new Setting();
    newSetting.addViews(listStatic, staticSelected);
    newSetting.addViews(listStandard, standardViewsSelected);
    newSetting.addViews(listIndex, indexViewsSelected);
    newSetting.addViews(listMemory, memoryViewsSelected);
    newSetting.setPreferredViewName(preferredViewName);
    if (viewOrderList != null) {
      for (UserPref.ViewPanelOrder viewPanelOrder : viewOrderList) {
        newSetting.setSelected(viewPanelOrder.getName(), viewPanelOrder.isShown());
      }
    }

    setting = newSetting;

    fireChangeEvent(originalSource, setting);

    //        for (AnDispTab adt : listStandard) {
    //            int dtype = adt.getTType();
    //            int mtype = MetricsSetting.dtype2mtype(dtype);
    //            System.out.println(String.format("%2d", dtype) + " " + mtype + " " +
    // adt.getTName() + " " + adt.getTCmd());
    //        }
  }

  /** Called from import settings... */
  public void set(
      final Object originalSource,
      final List<UserPref.ViewPanelOrder> viewOrderList,
      final String machineModel,
      final List<CustomObject> customIndexObjects,
      final List<CustomObject> customMemoryObjects) {
    AnUtility.checkIfOnAWTThread(true);
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            synchronized (AnVariable.mainFlowLock) {
              if (machineModel == null) {
                if (ViewsSetting.this.machineModel != null) {
                  ViewsSetting.loadMachineModelIPC("");
                }
              } else if (!machineModel.equals(ViewsSetting.this.machineModel)) {
                ViewsSetting.this.machineModel = machineModel;
                ViewsSetting.loadMachineModelIPC(machineModel);
              }
              if (customIndexObjects != null) {
                for (CustomObject customObject : customIndexObjects) {
                  ViewsSetting.defineIndxObjIPC(
                      customObject.getName(),
                      customObject.getFormula(),
                      customObject.getShortDesc(),
                      customObject.getLongDesc());
                }
              }
              if (customMemoryObjects != null) {
                for (CustomObject customObject : customMemoryObjects) {
                  ViewsSetting.defineMemObjIPC(
                      customObject.getName(),
                      customObject.getFormula(),
                      customObject.getShortDesc(),
                      customObject.getLongDesc());
                }
              }
              final Object[] standardViews = getTabListInfoIPC();
              final boolean[] standardViewsSelected = ViewsSetting.getTabSelectionStateIPC();
              final Object[] indexViews = ViewsSetting.getIndxObjDescriptionsIPC();
              final boolean[] indexViewsSelected = ViewsSetting.getIndxTabSelectionStateIPC();
              final Object[] memoryViews = ViewsSetting.getMemObjectsIPC();
              final boolean[] memoryViewsSelected = ViewsSetting.getMemTabSelectionStateIPC();
              AnUtility.dispatchOnSwingThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      init(
                          originalSource,
                          viewOrderList,
                          AnWindow.getInstance().getExperimentGroups(),
                          standardViews,
                          standardViewsSelected,
                          indexViews,
                          indexViewsSelected,
                          memoryViews,
                          memoryViewsSelected,
                          availableMachineModels,
                          machineModel,
                          customIndexObjects,
                          customMemoryObjects);
                    }
                  });
            }
          }
        },
        "Set Views Thread");
  }

  public void set(Object originalSource, boolean[] selected) {
    Setting settingCopy = setting.copy();
    List<View> list = settingCopy.getViewList();
    assert selected.length == list.size();
    for (int i = 0; i < list.size(); i++) {
      list.get(i).setSelected(selected[i]);
    }
    settingCopy.setPreferredViewName(AnWindow.getInstance().getViews().getCurrentViewName());

    set(originalSource, settingCopy);
  }

  public void toggleTab(String cmd, String preferredViewName) {
    Setting settingCopy = setting.copy();
    settingCopy.toggleTab(cmd);
    settingCopy.setPreferredViewName(preferredViewName);
    set(this, settingCopy);
  }

  public String getPreferredViewName() {
    return setting.getPreferredViewName();
  }

  public List<UserPref.ViewPanelOrder> getSavedViewOrderList() {
    return savedViewOrderList;
  }

  public String[] getAvailableMachineMsodel() {
    return availableMachineModels;
  }

  public String getMachineModel() {
    return machineModel;
  }

  public void setMachineModel(String loadedMachineModel) {
    this.machineModel = loadedMachineModel;
  }

  public void addCustomIndexObject(String name, String command, String shortDesc, String ldesc) {
    if (customIndexObjects == null) {
      customIndexObjects = new ArrayList<CustomObject>();
    }
    customIndexObjects.add(new CustomObject(name, command, shortDesc, ldesc));
  }

  public List<CustomObject> getCustomIndexObjects() {
    return customIndexObjects;
  }

  public void addCustomMemoryObject(
      String name, String command, String shortDesc, String longDesc) {
    if (customMemoryObjects == null) {
      customMemoryObjects = new ArrayList<CustomObject>();
    }
    customMemoryObjects.add(new CustomObject(name, command, shortDesc, longDesc));
  }

  public List<CustomObject> getCustomMemoryObjects() {
    return customMemoryObjects;
  }

  private List<AnDispTab> addStaticObjects(AnDispTab[] staticObjs) {
    List<AnDispTab> list = new ArrayList<AnDispTab>();

    for (int i = 0; i < staticObjs.length; i++) {
      AnDispTab anDispTab = staticObjs[i];
      list.add(anDispTab);
    }

    return list;
  }

  private List<AnDispTab> addStandardObjects(Object[] tabObjs) {
    List<AnDispTab> list = new ArrayList<AnDispTab>();

    AnWindow window = AnWindow.getInstance();
    int[] ids = (int[]) tabObjs[0];
    String[] cmds = (String[]) tabObjs[1];
    String[] shortDescs = null; // FIXUP: need short/long descr from gp-display-text
    String[] longDescs = null; // FIXUP: need short/long descr from gp-display-text
    for (int i = 0; i < ids.length; i++) {
      AnDispTab anDispTab =
          window
          .new AnDispTab(ids[i], cmds[i], null, null); // FIXUP: need short/long descr from gp-display-text
      list.add(anDispTab);
    }

    return list;
  }

  private List<AnDispTab> addIndexObjects(Object[] indxObjs) {
    List<AnDispTab> list = new ArrayList<AnDispTab>();

    AnWindow window = AnWindow.getInstance();
    int[] ids = (int[]) indxObjs[0];
    String[] cmds = (String[]) indxObjs[1];
    char[] mnemonics = (char[]) indxObjs[2];
    String[] i18nids = (String[]) indxObjs[3];
    String[] shortDescs = (String[]) indxObjs[6];
    String[] longDescs = (String[]) indxObjs[7];
    for (int i = 0; i < ids.length; i++) {
      String s = (i18nids[i] == null) ? cmds[i] : i18nids[i];
      String shortDesc = shortDescs[i];
      String longDesc = longDescs[i];
      AnDispTab anDispTab =
          window
          .new AnDispTab(
              AnDisplay.DSP_IndexObject, ids[i], s, mnemonics[i], cmds[i], shortDesc, longDesc);
      list.add(anDispTab);
    }

    return list;
  }

  private List<AnDispTab> addMemoryObjects(Object[] memObjs) {
    List<AnDispTab> list = new ArrayList<AnDispTab>();

    AnWindow window = AnWindow.getInstance();
    int[] ids = (int[]) memObjs[0];
    String[] cmds = (String[]) memObjs[1];
    char[] mnemonics = (char[]) memObjs[2];
    String[] i18nids = (String[]) memObjs[3];
    String[] shortDescs = (String[]) memObjs[6];
    String[] longDescs = (String[]) memObjs[7];
    for (int i = 0; i < ids.length; i++) {
      String shortDesc = shortDescs[i];
      String longDesc = longDescs[i];
      AnDispTab anDispTab =
          window
          .new AnDispTab(
              AnDisplay.DSP_MemoryObject,
              ids[i],
              cmds[i],
              mnemonics[i],
              cmds[i],
              shortDesc,
              longDesc);
      list.add(anDispTab);
    }

    return list;
  }

  public void updateIndexObjects(Object originalSource, Object[] indxObjs, boolean[] indxSelected) {
    List<View> staticList2 = setting.getStaticViews();
    List<View> standardList2 = setting.getStandardViews();
    List<View> indexList2 = setting.getIndexViews();
    List<View> memoryList2 = setting.getMemoryViews();
    cleanUpOldObjects2(indexList2);
    List<AnDispTab> indxList = addIndexObjects(indxObjs);
    Setting settingCopy = new Setting();
    settingCopy.addViews(staticList2);
    settingCopy.addViews(standardList2);
    settingCopy.addViews(indxList, indxSelected);
    settingCopy.addViews(memoryList2);
    settingCopy.setPreferredViewName(AnWindow.getInstance().getViews().getCurrentViewName());
    set(originalSource, settingCopy);
  }

  public void updateMemoryObjects(Object originalSource, Object[] memObjs, boolean[] memSelected) {
    List<View> staticList2 = setting.getStaticViews();
    List<View> standardList2 = setting.getStandardViews();
    List<View> indexList2 = setting.getIndexViews();
    List<View> memoryList2 = setting.getMemoryViews();
    cleanUpOldObjects2(memoryList2);
    List<AnDispTab> memList = addMemoryObjects(memObjs);
    Setting settingCopy = new Setting();
    settingCopy.addViews(staticList2);
    settingCopy.addViews(standardList2);
    settingCopy.addViews(indexList2);
    settingCopy.addViews(memList, memSelected);
    settingCopy.setPreferredViewName(AnWindow.getInstance().getViews().getCurrentViewName());
    set(originalSource, settingCopy);
  }

  public void updateIndexMemoryObjects(
      Object originalSource,
      Object[] indxObjs,
      boolean[] indxSelected,
      Object[] memObjs,
      boolean[] memSelected) {
    List<View> staticList2 = setting.getStaticViews();
    List<View> standardList2 = setting.getStandardViews();
    List<View> indexList2 = setting.getIndexViews();
    List<View> memoryList2 = setting.getMemoryViews();
    cleanUpOldObjects2(indexList2);
    List<AnDispTab> indxList = addIndexObjects(indxObjs);
    cleanUpOldObjects2(memoryList2);
    List<AnDispTab> memList = addMemoryObjects(memObjs);
    Setting settingCopy = new Setting();
    settingCopy.addViews(staticList2);
    settingCopy.addViews(standardList2);
    settingCopy.addViews(indxList, indxSelected);
    settingCopy.addViews(memList, memSelected);
    settingCopy.setPreferredViewName(AnWindow.getInstance().getViews().getCurrentViewName());
    set(originalSource, settingCopy);
  }

  public List<View> getStaticViews() {
    List<View> list = new ArrayList<View>();
    for (View view : setting.getStaticViews()) {
      list.add(view);
    }
    return list;
  }

  public List<View> getStandardViews() {
    List<View> list = new ArrayList<View>();
    for (View view : setting.getStandardViews()) {
      list.add(view);
    }
    return list;
  }

  public List<View> getIndexViews() {
    List<View> list = new ArrayList<View>();
    for (View view : setting.getIndexViews()) {
      list.add(view);
    }
    return list;
  }

  public List<View> getMemoryViews() {
    List<View> list = new ArrayList<View>();
    for (View view : setting.getMemoryViews()) {
      list.add(view);
    }
    return list;
  }

  public List<AnDispTab> getSelectedViews() {
    List<AnDispTab> list = new ArrayList<AnDispTab>();
    for (View view : setting.getSelectedViews()) {
      list.add(view.getAnDispTab());
    }
    return list;
  }

  public View findView(String cmd) {
    return setting.findView(cmd);
  }

  public View findView(int type) {
    return setting.findView(type);
  }

  public boolean isViewAvailable(String cmd) {
    return findView(cmd) != null;
  }

  public boolean isAvailableAndShowing(String cmd) {
    View view = setting.findView(cmd);
    if (view == null) {
      return false;
    } else {
      return view.isSelected();
    }
  }

  public View getFirstDataView() {
    View dataView = setting.getFirstDataView();
    return dataView;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.VIEWS;
  }

  @Override
  Object getValue() {
    return setting;
  }

  @Override
  void setValue(Object newValue) {
    Setting newSetting = (Setting) newValue;
    setting = newSetting;
    //        setting.debug();
  }

  private void cleanUpOldObjects2(List<View> list) {
    if (list != null) {
      for (int i = 0; i < list.size(); i++) {
        View view = (View) list.get(i);
        AnDisplay anDisplay = view.getAnDispTab().getTComp();

        if (anDisplay instanceof MemoryIndexObjectView) {
          ((MemoryIndexObjectView) anDisplay).toBeRemoved();
        }
      }
    }
  }

  public static boolean viewNameChanged(
      ViewsSetting.Setting oldSetting, ViewsSetting.Setting newSetting) {
    boolean changed = false;

    String oldViewName = oldSetting.getPreferredViewName();
    String newViewName = newSetting.getPreferredViewName();
    if (newViewName != null) {
      if (oldViewName == null) {
        changed = true;
      } else {
        changed = !newViewName.equals(oldViewName);
      }
    }
    return changed;
  }

  public static boolean viewsChanged(
      ViewsSetting.Setting oldSetting, ViewsSetting.Setting newSetting) {
    boolean changed = false;

    if (oldSetting == null) {
      changed = true;
    } else {
      List<View> oldList = oldSetting.getSelectedViews();
      List<View> newList = newSetting.getSelectedViews();
      if (oldList.size() != newList.size()) {
        changed = true;
      } else {
        for (int i = 0; i < oldList.size(); i++) {
          View oldView = oldList.get(i);
          View newView = newList.get(i);
          if (oldView.isSelected() != newView.isSelected()
              || oldView.getAnDispTab() != newView.getAnDispTab()) {
            changed = true;
            break;
          }
        }
      }
    }
    return changed;
  }

  public List<JComponent> createViewsSettingsSelector() {
    List<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(AnWindow.getInstance().getViewsSettingsAction().getMenuItem());
    return componentList;
  }

  // IPC Calls
  // ------------------------------------------------------------------------------------------------------------
  public static Object[] getTabListInfoIPC() {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getTabListInfo");
      anWindow.IPC().send(0);
      return (Object[]) anWindow.IPC().recvObject();
    }
  }

  /**
   * Send request to get Tab List Info. Non-blocking IPC call. Caller should call
   * ipcResult.getObjects() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getTabListInfoIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getTabListInfo");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // Object[] result = ipcResult.getObjects() // blocking
    return ipcResult;
  }

  public static boolean[] getTabSelectionStateIPC() {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("getTabSelectionState");
      window.IPC().send(0);
      return (boolean[]) window.IPC().recvObject();
    }
  }

  /**
   * Send request to get Tab Selection State. Non-blocking IPC call. Caller should call
   * ipcResult.getBooleans() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getTabSelectionStateIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getTabSelectionState");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // boolean[] result = ipcResult.getBooleans() // blocking
    return ipcResult;
  }

  //    private void setTabSelectionState(final boolean[] selected) {
  //        synchronized (IPC.lock) {
  //            m_window.IPC().send("setTabSelectionState");
  //            m_window.IPC().send(m_window.getWindowID());
  //            m_window.IPC().send(selected);
  //            m_window.IPC().recvString(); //synchronize
  //        }
  //    }
  public static boolean[] getMemTabSelectionStateIPC() {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("getMemTabSelectionState");
      window.IPC().send(0);
      return (boolean[]) window.IPC().recvObject();
    }
  }

  /**
   * Send request to get Mem Tab Selection State. Non-blocking IPC call. Caller should call
   * ipcResult.getBooleans() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getMemTabSelectionStateIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getMemTabSelectionState");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // boolean[] result = ipcResult.getBooleans() // blocking
    return ipcResult;
  }

  //    private void setMemTabSelectionState(final boolean[] selected) {
  //        synchronized (IPC.lock) {
  //            m_window.IPC().send("setMemTabSelectionState");
  //            m_window.IPC().send(m_window.getWindowID());
  //            m_window.IPC().send(selected);
  //            m_window.IPC().recvString(); //synchronize
  //        }
  //    }
  public static Object[] getMemObjectsIPC() {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("getMemObjects");
      window.IPC().send(0);
      return (Object[]) window.IPC().recvObject();
    }
  }

  /**
   * Send request to get Mem Objects. Non-blocking IPC call. Caller should call
   * ipcResult.getObjects() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getMemObjectsIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getMemObjects");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // Object[] result = ipcResult.getObjects() // blocking
    return ipcResult;
  }

  //    private String[] getCPUVerMachineModel() {
  //        synchronized (IPC.lock) {
  //            m_window.IPC().send("getCPUVerMachineModel");
  //            m_window.IPC().send(m_window.getWindowID());
  //            return (String[]) m_window.IPC().recvObject();
  //        }
  //    }
  public static String getMachineModelIPC() {
    synchronized (IPC.lock) {
      AnWindow m_window = AnWindow.getInstance();
      m_window.IPC().send("getMachineModel");
      return m_window.IPC().recvString();
    }
  }

  /**
   * Send request to get Machine Model. Non-blocking IPC call. Caller should call
   * ipcResult.getString() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getMachineModelIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getMachineModel");
    IPCResult ipcResult = ipcHandle.sendRequest();
    // String result = ipcResult.getString() // blocking
    return ipcResult;
  }

  public static String loadMachineModelIPC(final String name) {
    synchronized (IPC.lock) {
      AnWindow m_window = AnWindow.getInstance();
      m_window.IPC().send("loadMachineModel");
      m_window.IPC().send(name);
      return m_window.IPC().recvString();
    }
  }

  public static String[] listMachineModelsIPC() {
    synchronized (IPC.lock) {
      AnWindow m_window = AnWindow.getInstance();
      m_window.IPC().send("listMachineModels");
      return (String[]) m_window.IPC().recvObject();
    }
  }

  public static String defineMemObjIPC(
      final String name, final String formula, final String sdesc, final String ldesc) {
    synchronized (IPC.lock) {
      AnWindow m_window = AnWindow.getInstance();
      m_window.IPC().send("defineMemObj");
      m_window.IPC().send(name);
      m_window.IPC().send(formula);
      m_window.IPC().send(sdesc);
      m_window.IPC().send(ldesc);
      return m_window.IPC().recvString();
    }
  }

  public static boolean[] getIndxTabSelectionStateIPC() {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("getIndxTabSelectionState");
      window.IPC().send(0);
      return (boolean[]) window.IPC().recvObject();
    }
  }

  /**
   * Send request to get Index Tab Selection State. Non-blocking IPC call. Caller should call
   * ipcResult.getBooleans() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getIndxTabSelectionStateIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getIndxTabSelectionState");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // boolean[] result = ipcResult.getBooleans() // blocking
    return ipcResult;
  }

  //    private void setIndxTabSelectionState(final boolean[] selected) {
  //        synchronized (IPC.lock) {
  //            m_window.IPC().send("setIndxTabSelectionState");
  //            m_window.IPC().send(m_window.getWindowID());
  //            m_window.IPC().send(selected);
  //            m_window.IPC().recvString(); //synchronize
  //        }
  //    }
  public static Object[] getIndxObjDescriptionsIPC() {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("getIndxObjDescriptions");
      window.IPC().send(0);
      return (Object[]) window.IPC().recvObject();
    }
  }

  /**
   * Send request to get Index Object Descriptions. Non-blocking IPC call. Caller should call
   * ipcResult.getObjects() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getIndxObjDescriptionsIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getIndxObjDescriptions");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // Object[] result = ipcResult.getObjects() // blocking
    return ipcResult;
  }

  public static String defineIndxObjIPC(
      final String name, final String formula, final String sdesc, final String ldesc) {
    synchronized (IPC.lock) {
      AnWindow m_window = AnWindow.getInstance();
      m_window.IPC().send("defineIndxObj");
      m_window.IPC().send(name);
      m_window.IPC().send(formula);
      m_window.IPC().send(sdesc);
      m_window.IPC().send(ldesc);
      return m_window.IPC().recvString();
    }
  }

  public static Object[] getCustomIndxObjectsIPC() {
    synchronized (IPC.lock) {
      AnWindow m_window = AnWindow.getInstance();
      m_window.IPC().send("getCustomIndxObjects");
      m_window.IPC().send(0);
      return (Object[]) m_window.IPC().recvObject();
    }
  }
}

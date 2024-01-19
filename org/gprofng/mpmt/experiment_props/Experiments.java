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


package org.gprofng.mpmt.experiment_props;

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ExperimentProperties sync'd with back-end experiments.

// NOTE: Synchronization on all public interfaces is required because calls may come from
// more than one thread.

public final class Experiments {
  private final AnWindow w_IPC;
  private final ArrayList<ExperimentProperties> experiments;

  // Constructor
  public Experiments(final AnWindow awindow) {
    this.w_IPC = awindow;
    experiments = new ArrayList();
  }

  // --- public experiment interface ----

  public synchronized ExperimentProperties getExperimentProperties(int expId) {
    if (expId < 0 || expId >= experiments.size()) {
      return null;
    }
    return experiments.get(expId);
  }

  public synchronized List<ExperimentProperties> getAllExperimentProperties() {
    return Collections.unmodifiableList(experiments);
  }

  // Drop all experiments
  public synchronized void resetAll() {
    dropAllExps();
  }

  public synchronized void ipcUpdateExps() { // IPC!
    dropAllExps(); // YXXX might much better to check for changes

    boolean[] enable = w_IPC.getExpEnable(); // IPC!!
    if (enable == null) {
      return;
    }
    final int size = enable.length;
    final int experimentIds[] = new int[size];
    for (int i = 0; i < size; i++) {
      experimentIds[i] = i;
    }

    // Use "async" IPC calls to minimize the "high latency" effect
    IPCResult ipcr_founderExpIds = getFounderExpIdIPCRequest(experimentIds); // ASYNC IPC
    IPCResult ipcr_userExpIds = getUserExpIdIPCRequest(experimentIds); // ASYNC IPC
    IPCResult ipcr_names = getExpVerboseNameIPCRequest(experimentIds); // ASYNC IPC
    IPCResult ipcr_groupIds = getExpGroupIdIPCRequest(experimentIds); // ASYNC IPC
    IPCResult ipcr_timeObject = getExperimentTimeInfoIPCRequest(experimentIds); // ASYNC IPC
    IPCResult ipcr_ddscrObject = getExperimentDataDescriptorsIPCRequest(experimentIds); // ASYNC IPC
    // Get the results fron those "async" IPC calls
    // Old code used "sync" IPC calls
    int[] founderExpIds = ipcr_founderExpIds.getInts();
    // w_IPC.getFounderExpId(experimentIds); // SYNC IPC
    int[] userExpIds = ipcr_userExpIds.getInts();
    // w_IPC.getUserExpId(experimentIds); // SYNC IPC
    String[] names = ipcr_names.getStrings();
    // w_IPC.getExpVerboseName(experimentIds); // SYNC IPC
    int[] groupIds = ipcr_groupIds.getInts();
    // w_IPC.getExpGroupId(experimentIds); // SYNC IPC
    Object[] timeObject = ipcr_timeObject.getObjects();
    // ipcGetExperimentTimeInfo(experimentIds); // SYNC IPC
    List<ExperimentProperties.BasicInfo> basicInfoList =
        processGetExperimentBasicInfo(
            experimentIds, founderExpIds, userExpIds, names, groupIds, timeObject);

    final Object[] ddscrObject = ipcr_ddscrObject.getObjects();
    // ipcGetExperimentDataDescriptors(experimentIds); // SYNC IPC
    List<List<DataDescriptor>> dataDescriptorsList =
        processGetExperimentDataDescriptors(ddscrObject);

    if (basicInfoList == null || dataDescriptorsList == null) {
      return; // weird
    }
    for (int i = 0; i < size; i++) {
      ExperimentProperties.BasicInfo metaData = basicInfoList.get(i);
      List<DataDescriptor> dataDescriptors = dataDescriptorsList.get(i);
      ExperimentProperties expProps = new ExperimentProperties(metaData, dataDescriptors);
      experiments.add(expProps);
    }
  }

  // --- internal stuff

  private void dropAllExps() {
    experiments.clear();
  }

  // ---IPC--- (Native methods from liber_dbe.so)
  private List<ExperimentProperties.BasicInfo> processGetExperimentBasicInfo(
      final int[] expIds,
      final int[] founderExpIds,
      final int[] userExpIds,
      final String[] names,
      final int[] groupIds,
      final Object[] timeObject) {
    if (timeObject == null
        || expIds == null
        || founderExpIds == null
        || userExpIds == null
        || groupIds == null) {
      return null;
    }
    final List<ExperimentProperties.BasicInfo> basicInfoList = new ArrayList();
    final long[] offset_time = (long[]) timeObject[0]; // ipcGetRelativeStartTime(exp_id);
    final long[] start_time = (long[]) timeObject[1]; // ipcGetStartTime(exp_id);
    final long[] end_time = (long[]) timeObject[2]; // ipcGetEndTime  (exp_id);
    final long[] start_wall_sec = (long[]) timeObject[3]; // ipcGetWallStartSec(exp_id);
    final String[] hostname = (String[]) timeObject[4]; //
    final int[] cpu_freq = (int[]) timeObject[5]; // ipcGetClock    (exp_id);

    final int size = expIds.length;
    if (size != founderExpIds.length
        || size != userExpIds.length
        || size != names.length
        || size != groupIds.length
        || size != start_time.length
        || size != offset_time.length
        || size != end_time.length
        || size != start_wall_sec.length
        || size != hostname.length
        || size != cpu_freq.length) {
      return null; // weird
    }

    for (int i = 0; i < size; i++) {
      // loop for each experiment
      ExperimentProperties.BasicInfo metaData =
          new ExperimentProperties.BasicInfo(
              expIds[i],
              founderExpIds[i],
              userExpIds[i],
              names[i],
              groupIds[i],
              offset_time[i],
              start_time[i],
              end_time[i],
              start_wall_sec[i],
              hostname[i],
              cpu_freq[i]);
      basicInfoList.add(metaData);
    }
    return basicInfoList;
  }

  private List<List<DataDescriptor>> processGetExperimentDataDescriptors(final Object[] list) {
    if (list == null) {
      return null;
    }
    final List<List<DataDescriptor>> dataDescriptorList = new ArrayList();
    final Object[] dataDescrsInfo = (Object[]) (list[0]);
    final Object[] dataDescrsProps = (Object[]) (list[1]); // array of dbeGetDataPropertiesV2()
    final int size = dataDescrsInfo.length;

    for (int i = 0; i < size; i++) {
      // loop for each experiment
      final List<DataDescriptor> exp_descrs =
          processGetExperimentDataDescriptors_experiment(
              (Object[]) dataDescrsInfo[i], (Object[]) dataDescrsProps[i]);
      dataDescriptorList.add(exp_descrs);
    }
    return dataDescriptorList;
  }

  private List<DataDescriptor> processGetExperimentDataDescriptors_experiment(
      final Object[] dataDescrsInfo, // ipcGetDataDescriptorsV2(exp_id);
      final Object[] dataDescrsProps) {
    final ArrayList<DataDescriptor> dataDescriptors = new ArrayList();
    if (dataDescrsProps == null || dataDescrsInfo == null) {
      return dataDescriptors;
    }
    final Object[] list = dataDescrsInfo;
    if (list.length == 0) {
      return dataDescriptors;
    }

    final int[] data_id = (int[]) list[0];
    final String[] name = (String[]) list[1];
    final String[] uname = (String[]) list[2];
    final int[] aux_prop_id = (int[]) list[3];
    int size = data_id.length;
    if (size != dataDescrsProps.length) {
      return dataDescriptors; // weird
    }

    for (int i = 0; i < size; i++) {
      final ArrayList<PropDescriptor> props =
          processDataPropertiesV2((Object[]) dataDescrsProps[i]);
      DataDescriptor ddscr =
          new DataDescriptor(name[i], uname[i], data_id[i], props, aux_prop_id[i]);
      dataDescriptors.add(ddscr);
    }
    return dataDescriptors;
  }

  private ArrayList<PropDescriptor> processDataPropertiesV2(final Object[] list) {
    final ArrayList<PropDescriptor> propDescriptors = new ArrayList();
    if (list == null || list.length == 0) {
      return propDescriptors;
    }

    final int[] propId = (int[]) list[0];
    final String[] propUName = (String[]) list[1];
    final int[] propTypeId = (int[]) list[2];
    final String[] propTypeName = (String[]) list[3]; // YXXX add this
    final int[] propFlags = (int[]) list[4];
    final String[] propName = (String[]) list[5];
    final Object[] propStates = (Object[]) list[6];
    final Object[] propUStates = (Object[]) list[7];

    int size = propId.length;

    for (int i = 0; i < size; i++) {
      String[] states = (String[]) propStates[i];
      String[] ustates = (String[]) propUStates[i];
      String name = propName[i];
      String uname = propUName[i];
      int id = propId[i];
      PropDescriptor propDscr = new PropDescriptor(name, uname, id, states, ustates);
      propDescriptors.add(propDscr);
    }
    return propDescriptors;
  }

  //    private Object[] ipcGetExperimentTimeInfo(final int experimentIds[]){
  //	synchronized( IPC.lock ) {
  //	    w_IPC.IPC().send("getExperimentTimeInfo");
  //	    // w_IPC.IPC().send(w_IPC.getWindowID());
  //	    w_IPC.IPC().send(experimentIds);
  //	    return (Object[])w_IPC.IPC().recvObject();
  //	}
  //    }
  //
  //    private Object[] ipcGetExperimentDataDescriptors(final int experimentIds[]){
  //	synchronized( IPC.lock ) {
  //	    w_IPC.IPC().send("getExperimentDataDescriptors");
  //	    // w_IPC.IPC().send(w_IPC.getWindowID());
  //	    w_IPC.IPC().send(experimentIds);
  //	    return (Object[])w_IPC.IPC().recvObject();
  //	}
  //    }

  /**
   * Send request to get corresponding Exp Verbose Names. Non-blocking IPC call. Caller should call
   * ipcResult.getObject() to get the result @Parameters: expIds - experimentsDisp indexes
   *
   * @return IPCResult
   */
  public IPCResult getExpVerboseNameIPCRequest(final int[] expIds) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getExpVerboseName");
    ipcHandle.append(expIds);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // String[] result = ipcResult.getObject() // blocking
    return ipcResult;
  }

  /**
   * Send request to get corresponding Exp Group IDs. Non-blocking IPC call. Caller should call
   * ipcResult.getObject() to get the result @Parameters: expIds - experimentsDisp indexes
   *
   * @return IPCResult
   */
  public IPCResult getExpGroupIdIPCRequest(final int[] expIds) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getExpGroupId");
    ipcHandle.append(expIds);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // int[] result = ipcResult.getObject() // blocking
    return ipcResult;
  }

  /**
   * Send request to get corresponding founderExpIds. Non-blocking IPC call. Caller should call
   * ipcResult.getObject() to get the result @Parameters: expIds - experimentsDisp indexes
   *
   * @return IPCResult
   */
  public IPCResult getFounderExpIdIPCRequest(final int[] expIds) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getFounderExpId");
    ipcHandle.append(expIds);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // int[] result = ipcResult.getObject() // blocking
    return ipcResult;
  }

  /**
   * Send request to get corresponding userExpIds. Non-blocking IPC call. Caller should call
   * ipcResult.getObject() to get the result @Parameters: expIds - experimentsDisp indexes
   *
   * @return IPCResult
   */
  public IPCResult getUserExpIdIPCRequest(final int[] expIds) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getUserExpId");
    ipcHandle.append(expIds);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // int[] result = ipcResult.getObject() // blocking
    return ipcResult;
  }

  /**
   * Send request to get corresponding Experiment Time Info. Non-blocking IPC call. Caller should
   * call ipcResult.getObject() to get the result @Parameters: expIds - experimentsDisp indexes
   *
   * @return IPCResult
   */
  public IPCResult getExperimentTimeInfoIPCRequest(final int[] expIds) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getExperimentTimeInfo");
    ipcHandle.append(expIds);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // int[] result = ipcResult.getObject() // blocking
    return ipcResult;
  }

  /**
   * Send request to get corresponding Experiment Data Descriptors. Non-blocking IPC call. Caller
   * should call ipcResult.getObject() to get the result @Parameters: expIds - experimentsDisp
   * indexes
   *
   * @return IPCResult
   */
  public IPCResult getExperimentDataDescriptorsIPCRequest(final int[] expIds) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getExperimentDataDescriptors");
    ipcHandle.append(expIds);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // int[] result = ipcResult.getObject() // blocking
    return ipcResult;
  }
}

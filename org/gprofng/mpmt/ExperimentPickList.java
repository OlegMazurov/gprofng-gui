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


package org.gprofng.mpmt;

import org.gprofng.mpmt.picklist.PickList;
import org.gprofng.mpmt.picklist.PickListElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExperimentPickList extends PickList {

  public ExperimentPickList(int max) {
    super(max);
  }

  public List<ExperimentPickListElement> getRecentExperiments() {
    List<ExperimentPickListElement> list = new ArrayList<ExperimentPickListElement>();
    List<PickListElement> mostRecentExperiments = getElements();
    for (PickListElement ple : mostRecentExperiments) {
      list.add((ExperimentPickListElement) ple);
    }
    return list;
  }

  public List<ExperimentPickListElement> getRecentExperiments(int max) {
    int numberAdded = 0;
    List<ExperimentPickListElement> list = new ArrayList<ExperimentPickListElement>();
    List<PickListElement> mostRecentExperiments = getElements();
    for (PickListElement ple : mostRecentExperiments) {
      list.add((ExperimentPickListElement) ple);
      numberAdded++;
      if (max > 0 && numberAdded >= max) {
        break;
      }
    }
    return list;
  }

  public List<ExperimentPickListElement> getRecentValidExperiments(int max) {
    int numberAdded = 0;
    List<ExperimentPickListElement> list = new ArrayList<ExperimentPickListElement>();
    List<PickListElement> mostRecentExperiments = getElements();
    for (PickListElement ple : mostRecentExperiments) {
      if (exists(((ExperimentPickListElement) ple).getPath())) {
        list.add((ExperimentPickListElement) ple);
        numberAdded++;
        if (max > 0 && numberAdded >= max) {
          break;
        }
      }
    }
    return list;
  }

  public void removeExperiment(String path) {
    List<ExperimentPickListElement> list = getRecentExperiments();
    for (ExperimentPickListElement experimentPickListElement : list) {
      if (experimentPickListElement.getPath().equals(path)) {
        removeElement(experimentPickListElement);
        break;
      }
    }
  }

  private boolean exists(String path) {
    // FIXUP: need to validate on remote host in case of remote
    String host = Analyzer.getInstance().getHost();
    if (host == null || !host.equals("localhost")) {
      return true; // FIXUP: need to validate on remote host in case of remote
    }
    FileExists fileExists = new FileExists(path);
    fileExists.start();
    try {
      fileExists.join(100); // Wait at most .1 sec for result. Interrupt if hanging.
      fileExists.interrupt();
    } catch (InterruptedException ie) {
    }
    //        System.out.println(path + "  " + exists.existsResult.exists);
    return fileExists.getExistsResult().exists;
  }

  private class ExistsResult {

    public boolean exists = false;
  }

  private class FileExists extends Thread {

    private final String path;
    private final ExistsResult existsResult = new ExistsResult();

    public FileExists(String path) {
      this.path = path;
    }

    // @Override
    @Override
    public void run() {
      //            System.out.println("Checking " + path);
      existsResult.exists = new File(path).exists();
    }

    /**
     * @return the existsResult
     */
    public ExistsResult getExistsResult() {
      return existsResult;
    }
  }
}

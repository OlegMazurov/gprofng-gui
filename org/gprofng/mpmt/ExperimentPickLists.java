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
import org.gprofng.mpmt.picklist.PickListEvent;
import org.gprofng.mpmt.picklist.PickListListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentPickLists implements PickListListener {
  private int MAX = 25;
  private Map<String, ExperimentPickList> picklistMap = new HashMap<String, ExperimentPickList>();

  public ExperimentPickList getPicklist() {
    String host = Analyzer.getInstance().getHost();
    ExperimentPickList instance = getPicklistMap().get(host);
    if (instance == null) {
      instance = new ExperimentPickList(MAX);
      getPicklistMap().put(host, instance);
      instance.addPicklistListener(this);
    }
    return instance;
  }

  public void add(String host, List<ExperimentPickListElement> list) {
    ExperimentPickList instance = new ExperimentPickList(MAX);
    if (list != null) {
      for (int i = list.size() - 1; i >= 0; i--) {
        instance.addElement(list.get(i));
      }
    }
    getPicklistMap().put(host, instance);
    instance.addPicklistListener(this);
  }

  @Override
  public void contentsChanged(PickListEvent e) {
    fireContentsChanged(this);
  }

  private void fireContentsChanged(Object source) {
    AnChangeEvent anChangeevent =
        new AnChangeEvent(this, AnChangeEvent.Type.MOST_RECENT_EXPERIMENT_LIST_CHANGED);
    AnEventManager.getInstance().fireAnChangeEvent(anChangeevent);
  }

  /**
   * @return the picklistMap
   */
  public Map<String, ExperimentPickList> getPicklistMap() {
    return picklistMap;
  }

  /**
   * @param picklistMap the picklistMap to set
   */
  public void setPicklistMap(Map<String, ExperimentPickList> picklistMap) {
    this.picklistMap = picklistMap;
  }
}

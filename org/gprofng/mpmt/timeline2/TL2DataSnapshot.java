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

package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.settings.TimelineSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The class represents storage for the timeline data for a particular
// set of "entities" and enabled charts.  It should have a rowDefinition for
// every row, including those that might not be visible on the screen.
// The time range supplied should be for maximum zoom-out across all rows.
//
// The same instance of a snapshot can be reused for all levels
// of scroll, as well as display options such as stack height and orientation.
//
// A new instance of this class is needed when options change the actual rows
// that can be displayed:  Add/Drop experiment, Presentation Data Type
// selections, and Filters.
//
// Portions of this class are immutable:
//
// The row definitions are stored in:
//    rowDefinitions/timeStart/TimeEnd
// The array for storing results, rowResults, is allocated only once and
//     has a fixed size and a fixed set of RowData objects.
// Note, however, that components within RowData
//     are modified as data gets populated from gp-display-text.

public class TL2DataSnapshot {
  public static class EntityData {
    public final List<RowData> entityRows;

    public EntityData(List<RowData> entityRows) {
      this.entityRows = Collections.unmodifiableList(entityRows);
    }
  }

  public static class EntityDefinitions {
    public final List<RowDefinition> entityRows;

    public EntityDefinitions(List<RowDefinition> entityRows) {
      this.entityRows = Collections.unmodifiableList(entityRows);
    }
  }

  public static class ExperimentData {
    public final List<EntityData> experimentEntities;

    public ExperimentData(List<EntityData> experimentEntities) {
      this.experimentEntities = Collections.unmodifiableList(experimentEntities);
    }
  }

  public static class ExperimentDefinitions {
    public final List<EntityDefinitions> experimentEntities;

    public ExperimentDefinitions(List<EntityDefinitions> experimentEntities) {
      this.experimentEntities = Collections.unmodifiableList(experimentEntities);
    }
  }

  private final TimelineSetting.EntityProp entityProp;
  private final List<RowData> allRowData; // data organized by row
  private final List<EntityData> allEntityData; // data organized by entity
  private final List<EntityDefinitions> allEntityDefs; // defs organized by entity
  private final List<ExperimentData> experimentData; // data organized by experiment
  private final List<ExperimentDefinitions> experimentDefs; // defs organized by experiment
  private final long absoluteTimeStart, absoluteTimeEnd;
  private final Map<Integer, ExperimentDefinitions> experimentDefsByExpid;

  // RowDefinition data is expected to be immutable
  public TL2DataSnapshot(
      TimelineSetting.EntityProp entityProp,
      List<ExperimentDefinitions> expList,
      long absoluteTimeStart,
      long absoluteTimeEnd) {
    this.entityProp = entityProp;
    this.absoluteTimeStart = absoluteTimeStart;
    this.absoluteTimeEnd = absoluteTimeEnd;
    this.experimentDefs = Collections.unmodifiableList(expList);
    experimentDefsByExpid = new HashMap();
    int ii = 0;
    ArrayList<ExperimentData> newExpDataList = new ArrayList();
    ArrayList<RowData> newAllRowData = new ArrayList();
    ArrayList<EntityData> newAllEntityData = new ArrayList();
    ArrayList<EntityDefinitions> newAllEntityDefs = new ArrayList();
    for (ExperimentDefinitions expDef : expList) {
      ArrayList<EntityData> newEntityDataList = new ArrayList();
      for (EntityDefinitions entityDef : expDef.experimentEntities) {
        // process entity
        ArrayList<RowData> newEntityData = new ArrayList();
        for (RowDefinition rowDef : entityDef.entityRows) {
          int expid = rowDef.getUserExpID();
          ExperimentDefinitions tmp = experimentDefsByExpid.put(expid, expDef);
          if (tmp != null && tmp != expDef) {
            int jj = 1; // weird~
          }
          RowData newRow = new RowData(rowDef, ii);
          newAllRowData.add(newRow);
          newEntityData.add(newRow);
          ii++;
        }
        EntityData newEntity = new EntityData(newEntityData);
        newEntityDataList.add(newEntity);
        newAllEntityData.add(newEntity);
        newAllEntityDefs.add(entityDef);
      }
      newExpDataList.add(new ExperimentData(newEntityDataList));
    }
    allRowData = Collections.unmodifiableList(newAllRowData);
    allEntityData = Collections.unmodifiableList(newAllEntityData);
    allEntityDefs = Collections.unmodifiableList(newAllEntityDefs);
    experimentData = Collections.unmodifiableList(newExpDataList);
  }

  public TimelineSetting.EntityProp getEntityProp() {
    return entityProp;
  }

  public long getAbsoluteTimeEnd() {
    return absoluteTimeEnd;
  }

  public long getAbsoluteTimeStart() {
    return absoluteTimeStart;
  }

  public List<RowData> getRowData() {
    return allRowData;
  }

  public List<EntityData> getEntityData() {
    return allEntityData;
  }

  public List<EntityDefinitions> getEntityDefinitions() {
    return allEntityDefs;
  }

  public List<ExperimentData> getExperimentData() {
    return experimentData;
  }

  public List<ExperimentDefinitions> getExperimentDefinitions() {
    return experimentDefs;
  }

  public ExperimentDefinitions getExperimentDefinitions(int expid) {
    return experimentDefsByExpid.get(expid);
  }
}

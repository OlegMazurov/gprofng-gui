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


package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.experiment_props.*;
import org.gprofng.mpmt.settings.Settings;
import java.util.ArrayList;
import java.util.List;

public class TLExperiment { // YXXX refactor or rename.  Sole remaining method is createRows()
  private final PropDescriptor PROP_EVT_COUNT = // used to get packet count
      new PropDescriptor(
          "EVT_COUNT",
          "Event Count", // Should be I18N'D YXXX
          Settings.PROP_NONE,
          null,
          null);

  private boolean descriptorIsEnabled(
      final String nameWithAux, final long tldata_hidden_mask, final List<String> tldata_cnames) {
    if (tldata_hidden_mask == 0 || tldata_cnames == null || tldata_cnames.isEmpty()) {
      return true;
    }

    for (int ii = 0; ii < tldata_cnames.size(); ii++) {
      long bit = tldata_hidden_mask & 1L << ii;
      if (bit != 0) {
        String hiddenName = tldata_cnames.get(ii);
        if (nameWithAux.equals(hiddenName)) {
          return false;
        }
      }
    }
    return true;
  }

  // routine to discover the Experiment's rows
  // returns: RowDefinitions grouped by entity)
  public List<TL2DataSnapshot.EntityDefinitions> createEntityRowDefinitions(
      final ExperimentProperties experimentProperties,
      final List<Entity> ents,
      final int entity_prop_id,
      final long tldata_hidden_mask,
      final List<String> tldata_cnames,
      final boolean show_states,
      final boolean show_charts,
      final long time_origin, // timestamp at left origin (nanoseconds)
      final boolean comparing) {
    List<TL2DataSnapshot.EntityDefinitions> entityList = new ArrayList();

    if (ents == null || ents.isEmpty()) {
      return entityList;
    }

    // add rows that aren't associated with an entity
    List<DataDescriptor> data_descs_with_ent = new ArrayList();
    List<DataDescriptor> data_descs_without_ent = new ArrayList();

    final Entity ent0 = ents.get(0);
    final int ent_prop_id = ent0.getPropId();
    final String ent_prop_id_name = ent0.getPropIdName();
    final boolean prop_is_expid = ent_prop_id_name.equalsIgnoreCase("EXPID");
    List<DataDescriptor> descriptors = experimentProperties.getTstampDataDescriptors();
    for (DataDescriptor dataD : descriptors) {
      if (dataD.findProp("TSTAMP") == null) { // YXXX redundant??
        continue; // only draw entities that have time
      }
      if (dataD.getTLDataType().equals(Settings.TLData_type.TL_HEAPSZ)
          || dataD.getTLDataType().equals(Settings.TLData_type.TL_GCEVENT)) { // CXXX Bug 20801848
        // YXXX see if we can avoid special case by adding property to DataDescriptor
        data_descs_without_ent.add(dataD);
        continue;
      }
      if (prop_is_expid) {
        data_descs_without_ent.add(dataD);
        continue;
      }
      if (dataD.findProp(ent_prop_id) != null) {
        data_descs_with_ent.add(dataD);
      } else {
        data_descs_without_ent.add(dataD);
      }
    }

    List<RowDefinition> entityRowDefs =
        newEntityRowDefs(
            experimentProperties,
            data_descs_without_ent,
            null,
            tldata_hidden_mask,
            tldata_cnames,
            show_states,
            show_charts,
            time_origin,
            comparing);
    entityList.add(new TL2DataSnapshot.EntityDefinitions(entityRowDefs));

    // add rows for entities
    for (Entity ent : ents) {
      entityRowDefs =
          newEntityRowDefs(
              experimentProperties,
              data_descs_with_ent,
              ent,
              tldata_hidden_mask,
              tldata_cnames,
              show_states,
              show_charts,
              time_origin,
              comparing);
      entityList.add(new TL2DataSnapshot.EntityDefinitions(entityRowDefs));
    }
    return entityList;
  }

  private boolean tryAdd(
      List<PropDescriptor> chartProps, final DataDescriptor dataD, final String propName) {
    PropDescriptor prop = dataD.findProp(propName);
    if (prop != null) {

      String[] stateNames = prop.getStateNames();
      if (stateNames == null) {
        // value chart
        chartProps.add(prop);
        return true;
      } else {
        // state chart
        // ... do not chart items that have only one state
        int nStates = 0;
        int maxstates = prop.getStateNames().length;
        for (int ii = 0; ii < maxstates; ii++) {
          if (stateNames[ii] != null) {
            nStates++;
          }
        }
        if (nStates > 1) {
          chartProps.add(prop);
          return true;
        }
      }
    }
    return false;
  }

  // TBR? transfer this logic into event details classes?
  private List<PropDescriptor> getChartProps(
      final DataDescriptor dataD, final boolean show_states, final boolean show_charts) {
    List<PropDescriptor> chartProps = new ArrayList();

    Settings.TLData_type tltype = dataD.getTLDataType();
    if (tltype.equals(Settings.TLData_type.TL_SAMPLE)) {
      if (show_charts) {
        chartProps.add(PROP_EVT_COUNT);
      }
    } else if (tltype.equals(Settings.TLData_type.TL_CLOCK)) {
      if (show_states) {
        tryAdd(chartProps, dataD, "MSTATE"); // See MSTATE_COLORS_BY_OVERVIEW_IDX
        // tryAdd( chartProps, dataD, "OMPSTATE" );// See <TBD>TL_UNKNOWN_GENERIC_COLOR_TABLE
        // tryAdd( chartProps, dataD, "MPISTATE" );// See <TBD>TL_UNKNOWN_GENERIC_COLOR_TABLE
      }
      if (show_charts) {
        chartProps.add(PROP_EVT_COUNT);
      }
    } else if (tltype.equals(Settings.TLData_type.TL_SYNC)) {
      if (show_charts) {
        tryAdd(chartProps, dataD, "EVT_TIME");
      }
    } else if (tltype.equals(Settings.TLData_type.TL_IOTRACE)) {
      if (show_states) {
        tryAdd(chartProps, dataD, "IOTYPE"); // See TL_IOTRACE_IOTYPE_COLOR_TABLE
      }
      if (show_charts) {
        tryAdd(chartProps, dataD, "EVT_TIME");
      }
    } else if (tltype.equals(Settings.TLData_type.TL_HEAP)) {
      if (show_states) {
        tryAdd(chartProps, dataD, "HTYPE"); // See TL_HEAP_HTYPE_COLOR_TABLE
      }
      if (show_charts) {
        tryAdd(chartProps, dataD, "HSIZE");
      }
    } else if (tltype.equals(Settings.TLData_type.TL_HEAPSZ)) {
      tryAdd(chartProps, dataD, "HCUR_ALLOCS");
      tryAdd(chartProps, dataD, "HCUR_LEAKS");
      //            if(show_states){
      //                tryAdd( chartProps, dataD, "HTYPE" );// See TL_HEAP_HTYPE_COLOR_TABLE
      //            }
    } else if (tltype.equals(Settings.TLData_type.TL_MPI)) {
      if (show_states) {
        // tryAdd( chartProps, dataD, "MPITYPE" );// See <TBD>TL_UNKNOWN_GENERIC_COLOR_TABLE
      }
      if (show_charts) {
        // tryAdd( chartProps, dataD, "MPISCOUNT" );
        // tryAdd( chartProps, dataD, "MPIRCOUNT" );
        // tryAdd( chartProps, dataD, "MPISBYTES" );
        // tryAdd( chartProps, dataD, "MPIRBYTES" );
        chartProps.add(PROP_EVT_COUNT);
      }
    } else if (tltype.equals(Settings.TLData_type.TL_RACES)) {
      if (show_states) {
        // tryAdd( chartProps, dataD, "RTYPE" );// See <TBD>TL_UNKNOWN_GENERIC_COLOR_TABLE
      }
      if (show_charts) {
        chartProps.add(PROP_EVT_COUNT);
      }
    } else if (tltype.equals(Settings.TLData_type.TL_GCEVENT)) { // CXXX Bug 20801848
      if (show_charts) {
        chartProps.add(PROP_EVT_COUNT);
      }
    } else {
      if (show_charts) {
        chartProps.add(PROP_EVT_COUNT);
      }
    }

    return chartProps;
  }

  private List<RowDefinition> newEntityRowDefs(
      final ExperimentProperties experimentProperties,
      final List<DataDescriptor> dscrList,
      final Entity ent,
      final long tldata_hidden_mask,
      final List<String> tldata_cnames,
      final boolean show_states,
      final boolean show_charts,
      final long time_origin,
      final boolean comparing) {
    final List<RowDefinition> rawDataRows = new ArrayList();
    for (DataDescriptor dataD : dscrList) {
      int auxmax = dataD.getAuxCount();
      if (auxmax == 0) { // descriptor doesn't contain multiple types
        auxmax = 1;
      }
      for (int aux = 0; aux < auxmax; aux++) {
        String tldata_cmd = dataD.getErRcTLDataCmdName(aux);
        if (!descriptorIsEnabled(tldata_cmd, tldata_hidden_mask, tldata_cnames)) {
          continue;
        }
        List<PropDescriptor> chartProps = getChartProps(dataD, show_states, show_charts);
        RowDefinition row =
            new RowDefinition(
                experimentProperties, ent, dataD, aux, chartProps, time_origin, comparing);
        rawDataRows.add(row);
      }
    }
    return rawDataRows;
  }
}

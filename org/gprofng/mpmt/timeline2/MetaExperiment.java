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

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.experiment_props.ExperimentProperties;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.settings.TimelineSetting;
import org.gprofng.mpmt.timeline2.TL2DataSnapshot.EntityDefinitions;
import org.gprofng.mpmt.timeline2.TL2DataSnapshot.ExperimentDefinitions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MetaExperiment is a container for holding multiple Experiments.

// NOTE: Synchronization on this object is required because calls may come from
// more than one thread.

public final class MetaExperiment {
  // set by constructor parameters
  private final AnWindow w_IPC;

  // created and owned by this class:
  private final TL2DataFetcher tl2DataFetcher;
  private final TLExperiment tlExperiment;

  // timeline fetch settings
  private TimelineSetting.EntityProp entity_prop = null; // Row mode: CPU, THREAD, etc.
  private long tldata_hidden_mask;
  private boolean tldata_show_states;
  private boolean tldata_show_charts;
  private List<String> tldata_cnames;
  private List<String> tldata_unames;
  private int tldata_unames_version;

  // Constructor
  public MetaExperiment(
      final AnWindow awindow,
      final TimelineView timelineView) { // TimelineView only for doRepaint()
    this.w_IPC = awindow;
    this.tl2DataFetcher = new TL2DataFetcher(timelineView, awindow);
    this.tlExperiment = new TLExperiment();
    init_tldata_settings();
  }

  private void init_tldata_settings() {
    entity_prop = null; // Row mode: CPU, THREAD, etc.
    tldata_hidden_mask = 0;
    tldata_show_states = false;
    tldata_show_charts = false;
    tldata_cnames = new ArrayList();
    tldata_unames = new ArrayList();
    tldata_unames_version = -1;
  }

  public TL2DataFetcher getDataFetcher() {
    return tl2DataFetcher;
  }

  // --- public experiment interface ----

  // clear(): old data is invalid so stop fetching and wait for *Update*() call
  public synchronized void resetCurrentActivity() { // action that precedes Update actions
    resetTimeData();
    tl2DataFetcher.resetSnapshot();
  }

  public synchronized void edt_resetAll() { // a fresh start e.g. "open experiment"
    //        experiments.resetAll();
    resetTimeData();
    tl2DataFetcher.resetAll(); // YXXX: refactor: also clears color chooser colors
    init_tldata_settings();
  }

  // Presentation row settings changed
  public synchronized boolean edt_updatePresentationFetchOptions(
      final TimelineSetting.EntityProp entity_prop,
      final long _tldata_hidden_mask,
      final boolean show_states,
      final boolean show_charts,
      final List<String> new_tldata_cnames,
      final List<String> new_tldata_unames,
      final int new_tldata_names_version) {
    boolean changed = false;
    if (this.entity_prop == null || this.entity_prop.prop_id != entity_prop.prop_id) {
      this.entity_prop = entity_prop;
      changed = true;
    }
    if (this.tldata_hidden_mask != _tldata_hidden_mask) {
      this.tldata_hidden_mask = _tldata_hidden_mask;
      changed = true;
    }
    if (this.tldata_show_states != show_states) {
      this.tldata_show_states = show_states;
      changed = true;
    }
    if (this.tldata_show_charts != show_charts) {
      this.tldata_show_charts = show_charts;
      changed = true;
    }
    if (this.tldata_unames_version != new_tldata_names_version) {
      this.tldata_unames_version = new_tldata_names_version;
      this.tldata_cnames = new ArrayList(new_tldata_cnames);
      this.tldata_unames = new ArrayList(new_tldata_unames);
      changed = true;
    }

    return changed;
  }

  public synchronized void ipcUpdateRows() { // generates a new TL2DataSnapshot //IPC!
    resetTimeData();
    if (entity_prop == null) {
      tl2DataFetcher.setTL2DataSnapshot(null);
      return;
    }
    final int prop_id = entity_prop.prop_id;

    final calcExperimentTimeOriginsRC origins = calcExperimentTimeOrigins(false, true, true);

    List<ExperimentDefinitions> experimentList = new ArrayList();
    final boolean comparing =
        w_IPC.getSettings().getCompareModeSetting().comparingExperiments(); // does not perform IPC

    List<ExperimentProperties> exps = w_IPC.getExperimentProperties().getAllExperimentProperties();
    final int[] expIds = new int[exps.size()];
    for (int ii = 0; ii < exps.size(); ii++) {
      expIds[ii] = exps.get(ii).getID();
    }
    List<List<Entity>> entsList = ipcGetEntitiesV2(expIds, prop_id);
    for (int ii = 0; ii < entsList.size(); ii++) {
      ExperimentProperties expProps = exps.get(ii);
      List<Entity> ents = entsList.get(ii);
      int exp_id = expProps.getID();
      List<EntityDefinitions> entityList =
          tlExperiment.createEntityRowDefinitions(
              expProps,
              ents,
              prop_id,
              tldata_hidden_mask,
              tldata_cnames,
              tldata_show_states,
              tldata_show_charts,
              origins.timeOrigins.get(exp_id),
              comparing);
      experimentList.add(new ExperimentDefinitions(entityList));
    }
    experimentList = ipcPruneEmptyRows(experimentList); // IPC!
    TL2DataSnapshot tl2DataSnapshot;
    tl2DataSnapshot = new TL2DataSnapshot(entity_prop, experimentList, 0, origins.maxDuration);
    tl2DataFetcher.setTL2DataSnapshot(tl2DataSnapshot);
  }

  // --- internal stuff ----

  private void resetTimeData() {}

  private List<ExperimentDefinitions> ipcPruneEmptyRows( // IPC!
      List<ExperimentDefinitions> experimentList) {
    List<ExperimentDefinitions> pruned = Collections.unmodifiableList(new ArrayList());
    if (experimentList == null) {
      return pruned;
    }

    int sz = 0;
    for (ExperimentDefinitions expDef : experimentList) {
      for (EntityDefinitions entityDef : expDef.experimentEntities) {
        sz += entityDef.entityRows.size();
      }
    }
    if (sz == 0) {
      return pruned;
    }

    int exp_ids[] = new int[sz];
    int data_ids[] = new int[sz];
    int entity_prop_ids[] = new int[sz];
    int entity_prop_vals[] = new int[sz];
    int hwctags[] = new int[sz];
    int ii = 0;
    for (ExperimentDefinitions expDef : experimentList) {
      for (EntityDefinitions entityDef : expDef.experimentEntities) {
        for (RowDefinition row : entityDef.entityRows) {
          exp_ids[ii] = row.getExpID();
          data_ids[ii] = row.getDataDescriptor().getDataId();
          entity_prop_ids[ii] = row.getEntity().getPropId();
          entity_prop_vals[ii] = row.getEntity().getPropValue();
          hwctags[ii] = row.getAux();
          ii++;
        }
      }
    }

    boolean[] hasData =
        ipcHasTLData(
            exp_ids,
            data_ids, // IPC!
            entity_prop_ids,
            entity_prop_vals,
            hwctags);
    if (hasData == null || hasData.length != sz) {
      return pruned; // weird
    }

    // only add rows for Entities that have data
    // note: each rowBlock corresponds to an entity
    ii = 0;
    ArrayList<ExperimentDefinitions> newExpList = new ArrayList();
    for (ExperimentDefinitions expDef : experimentList) {
      ArrayList<EntityDefinitions> newEntityList = new ArrayList();
      for (EntityDefinitions entityDef : expDef.experimentEntities) {
        boolean entityHasData = false;
        for (RowDefinition row : entityDef.entityRows) {
          if (hasData[ii]) {
            entityHasData = true;
            // break; can't break here because we have to increment ii
          }
          ii++;
        }
        if (entityHasData) {
          newEntityList.add(entityDef);
        }
      }
      if (!newEntityList.isEmpty()) {
        newExpList.add(new ExperimentDefinitions(newEntityList));
      }
    }
    pruned = Collections.unmodifiableList(newExpList);
    return pruned;
  }

  // --- internal stuff ----

  public static final long NANOSEC = 1000000000L;

  private class TimeConversion {
    // timestamps relating wall time to HRT nanoseconds:
    private final long ref_hrt_ns; // reference hrt snapshot
    // Time distance from reference to global (and recent) point in time
    private final long global_offset_ns; // nanosecond distance from ref to global point

    public TimeConversion(long ref_hrt_ns, long global_offset_ns) {
      this.ref_hrt_ns = ref_hrt_ns;
      this.global_offset_ns = global_offset_ns;
    }

    public long hrtToNormalizedWallNanoSec(long hrt) {
      final long hrt_delta = hrt - ref_hrt_ns;
      final long normalized_wall_ns = global_offset_ns + hrt_delta;
      return normalized_wall_ns;
    }

    public long normalizedWallNanoSecToHrt(long normalized_wall_ns) {
      final long hrt_delta = normalized_wall_ns - global_offset_ns;
      return hrt_delta + ref_hrt_ns;
    }
  }

  private class calcExperimentTimeOriginsRC {
    final Long maxDuration;
    final Map<Integer, Long> timeOrigins; // expId, computed timestamp @ origin

    calcExperimentTimeOriginsRC(Long maxDuration, Map<Integer, Long> timeOrigins) {
      this.maxDuration = maxDuration;
      this.timeOrigins = timeOrigins;
    }
  }

  private calcExperimentTimeOriginsRC calcExperimentTimeOrigins(
      final boolean forceAllLeft, // force founders and descendents left
      final boolean forceFoundersLeft, // force founders but not descedents left
      final boolean forceNonConcurrentLeft // force standalone founders left
      //            final Map<Integer, Long> customOffsets
      ) {
    List<ExperimentProperties> expProps =
        w_IPC.getExperimentProperties().getAllExperimentProperties();
    final Map<Integer, Long> rcTimeOrigins = new HashMap(); // key: expId, value: hrt of origin
    long rcMaxDuration = 0L;
    if (expProps == null || expProps.isEmpty()) {
      return new calcExperimentTimeOriginsRC(rcMaxDuration, rcTimeOrigins);
    }

    // loop thru all exps, save subexperiment-to-founder offsets
    if (forceFoundersLeft || forceAllLeft) {

      final Map<Integer, Long> offsets = new HashMap();
      long minOffset = 0;
      for (ExperimentProperties exp : expProps) {
        final int expId = exp.getID();
        long offset;
        if (forceAllLeft) {
          offset = 0; // start at experiment's origin
        } else {
          offset = exp.getTimeOffset(); // founder start
        }
        //                if (customOffsets != null) {
        //                    final Long customOffset = customOffsets.get(expId);
        //                    if (customOffset != null) {
        //                        offset += customOffset;
        //                    }
        //                }
        offsets.put(expId, offset);
        if (minOffset > offset) {
          // find the most negative offset
          minOffset = offset;
        }
      }

      //        if(forceFoundersLeft || forceAllLeft){
      // save per-experiment origin timestamp, as well as longest duration
      for (ExperimentProperties exp : expProps) {
        final int expId = exp.getID();
        // normalize to minOffset so that leftmost start is at origin
        final long offset = offsets.get(expId) - minOffset;
        final long expOrigin = exp.getStartTime() - offset;
        final long expDuration = exp.getEndTime() - expOrigin;
        rcTimeOrigins.put(expId, expOrigin);
        rcMaxDuration = Math.max(expDuration, rcMaxDuration);
      }
      return new calcExperimentTimeOriginsRC(rcMaxDuration, rcTimeOrigins);
    }

    // Scan for founder experiments that are concurrent in time
    // We will left-align any founders that are not concurrent with others.

    // The following holds wall-to-hrtime reference values.
    // For a given host "epoch" (e.g. between reboots),
    // there should be only one pair of references used.
    // This is because the HRT and time of day ticks may not be synchronized
    class HostnameTimeRef {
      public final String hostname;
      private long wall_seconds;
      private long hrt;

      HostnameTimeRef(String hostname, long wall_seconds, long hrt) {
        this.hostname = hostname;
        this.wall_seconds = wall_seconds;
        this.hrt = hrt;
      }

      public boolean sameEpoch(HostnameTimeRef other) {
        if (!hostname.equals(other.hostname)) {
          return false;
        }
        final long wall_delta = (wall_seconds - other.wall_seconds) * NANOSEC;
        final long hrt_delta = hrt - other.hrt;
        final long delta = wall_delta - hrt_delta;
        if (Math.abs(delta) > 2 * NANOSEC) {
          return false;
        }
        return true;
      }

      public boolean isBetterRef(HostnameTimeRef other) {
        if (!sameEpoch(other)) {
          return false;
        }
        if (wall_seconds < other.wall_seconds) {
          return false;
        }
        if (hrt < other.hrt) {
          return false;
        }
        return true;
      }

      public void update(HostnameTimeRef other) {
        wall_seconds = other.wall_seconds;
        hrt = other.hrt;
      }
    }

    // Represents a blob of experiments that overlap in time:
    class ConcurrentExperiments {
      private final Map<Integer, ExperimentProperties> subexps; // expId key
      private final Map<Integer, HostnameTimeRef> hostInfo; // expId key
      private final Map<Integer, TimeConversion> timeInfo; // expId key
      private long start;
      private long end;
      private final long wallNormalizeSeconds;

      public ConcurrentExperiments(long wallNormalizeSeconds) {
        subexps = new HashMap();
        hostInfo = new HashMap();
        timeInfo = new HashMap();
        start = Long.MAX_VALUE;
        end = Long.MIN_VALUE;
        this.wallNormalizeSeconds = wallNormalizeSeconds;
      }

      public boolean overlaps(ConcurrentExperiments other) {
        if (other == null) {
          return false;
        }
        if (start == Long.MAX_VALUE || end == Long.MIN_VALUE) {
          // anything overlaps with new object
          return true; // weird
        }
        if (other.start > end) {
          return false;
        }
        if (other.end < start) {
          return false;
        }
        return true;
      }

      private void updateRange(long start, long end) {
        if (this.start > start) {
          this.start = start;
        }
        if (this.end < end) {
          this.end = end;
        }
      }

      public void add(ExperimentProperties exp, HostnameTimeRef ref) {
        int expId = exp.getID();
        long globalOffset = (ref.wall_seconds - wallNormalizeSeconds) * NANOSEC;
        // globalOffset += nanoSecCorrection;
        TimeConversion timeConv = new TimeConversion(ref.hrt, globalOffset);
        final long tmpStart = exp.getStartTime();
        final long tmpEnd = exp.getEndTime();
        final long localRefToStart = timeConv.hrtToNormalizedWallNanoSec(tmpStart);
        final long localRefToEnd = timeConv.hrtToNormalizedWallNanoSec(tmpEnd);
        updateRange(localRefToStart, localRefToEnd);
        subexps.put(expId, exp);
        hostInfo.put(expId, ref);
        timeInfo.put(expId, timeConv);
      }

      public void add(ConcurrentExperiments other) {
        if (other == null) {
          return;
        }
        updateRange(other.start, other.end);
        subexps.putAll(other.subexps);
        hostInfo.putAll(other.hostInfo);
        timeInfo.putAll(other.timeInfo);
      }

      public long getDuration() {
        if (start == Long.MAX_VALUE || end == Long.MIN_VALUE) {
          return 0; // weird
        }
        return end - start;
      }

      public long getExpHrtAtConcurrentStart(int expId) {
        if (start == Long.MAX_VALUE || end == Long.MIN_VALUE) {
          return 0; // weird
        }
        TimeConversion timeConv = timeInfo.get(expId);
        final long origin = timeConv.normalizedWallNanoSecToHrt(start);
        return origin;
      }
    }

    // create hostMap; maps experiments to canonical host wall-to-hrt reference values:
    long min_wall = Long.MAX_VALUE;
    long max_wall = Long.MIN_VALUE;
    Map<String, List<HostnameTimeRef>> hostMap = new HashMap(); // key hostname
    Map<Integer, HostnameTimeRef> expTimeRef = new HashMap(); // key expId
    for (ExperimentProperties exp : expProps) {
      String hostname = exp.getHostname();
      if (min_wall > exp.getStartWallSeconds()) {
        min_wall = exp.getStartWallSeconds();
      }
      if (max_wall < exp.getStartWallSeconds()) {
        max_wall = exp.getStartWallSeconds();
      }
      HostnameTimeRef tmpRef =
          new HostnameTimeRef(hostname, exp.getStartWallSeconds(), exp.getStartTime());
      List<HostnameTimeRef> timeRefs = hostMap.get(hostname);
      boolean found = false;
      if (timeRefs == null) {
        timeRefs = new ArrayList();
        hostMap.put(hostname, timeRefs);
      } else {
        for (HostnameTimeRef hostRef : timeRefs) {
          if (hostRef.sameEpoch(tmpRef)) {
            if (hostRef.isBetterRef(tmpRef)) {
              hostRef.update(tmpRef);
            }
            tmpRef = hostRef;
            found = true;
            break;
          }
        }
      }
      if (!found) {
        timeRefs.add(tmpRef);
      }
      expTimeRef.put(exp.getID(), tmpRef);
    }

    if (max_wall - min_wall > Integer.MAX_VALUE) {
      int ii = 1; // weird!  Will overflow when converted to nanoseconds
    }
    final long wallNormalizeSeconds = min_wall;

    // first, store all descendents as concurrent
    final Map<Integer, ConcurrentExperiments> founders = new HashMap(); // founderExpId key
    for (ExperimentProperties exp : expProps) {
      final int expId = exp.getID();
      final Integer founderId = exp.getFounderID();

      // update founder ConcurrentExperiments
      ConcurrentExperiments timeInfo = founders.get(founderId);
      if (timeInfo == null) {
        timeInfo = new ConcurrentExperiments(wallNormalizeSeconds);
        founders.put(founderId, timeInfo);
      }
      timeInfo.add(exp, expTimeRef.get(expId));
    }

    // next, any founders that are concurrent get merged
    List<ConcurrentExperiments> blobs = new ArrayList(founders.values());
    for (int ii = 0; ii < blobs.size(); ii++) {
      ConcurrentExperiments base = blobs.get(ii);
      if (base == null) {
        continue;
      }
      for (int jj = ii + 1; jj < blobs.size(); jj++) {
        ConcurrentExperiments cmp = blobs.get(jj);
        if (base.overlaps(cmp)) {
          base.add(cmp);
          blobs.set(jj, null); // mark assimilated items as null
        }
      }
    }

    // next, prune assimilated items
    List<ConcurrentExperiments> liveBlobs = new ArrayList();
    for (ConcurrentExperiments blob : blobs) {
      if (blob == null) {
        continue;
      }
      liveBlobs.add(blob);
    }

    // scan for leftmost founders, durations
    for (ConcurrentExperiments blob : liveBlobs) {
      final long blob_offset = forceNonConcurrentLeft ? 0 : blob.start;
      Long duration = blob.getDuration() + blob_offset;
      if (rcMaxDuration < duration) {
        rcMaxDuration = duration;
      }
      for (Map.Entry<Integer, ExperimentProperties> entry : blob.subexps.entrySet()) {
        final ExperimentProperties exp = entry.getValue();
        final int expId = exp.getID();
        long origin = blob.getExpHrtAtConcurrentStart(expId) - blob_offset;
        //                final long debug = exp.getStartTime() - origin;
        rcTimeOrigins.put(expId, origin);
      }
    }
    return new calcExperimentTimeOriginsRC(rcMaxDuration, rcTimeOrigins);
  }

  // ---IPC--- (Native methods from liber_dbe.so)

  private boolean[] ipcHasTLData(
      int exp_ids[], int data_ids[], int entity_prop_ids[], int entity_prop_vals[], int hwctags[]) {
    synchronized (IPC.lock) {
      w_IPC.IPC().send("hasTLData");
      w_IPC.IPC().send(0);
      w_IPC.IPC().send(exp_ids);
      w_IPC.IPC().send(data_ids);
      w_IPC.IPC().send(entity_prop_ids);
      w_IPC.IPC().send(entity_prop_vals);
      w_IPC.IPC().send(hwctags);
      return (boolean[]) w_IPC.IPC().recvObject();
    }
  }

  private List<Entity> processGetEntities(final Object[] objs, final int entity_prop_id) {
    if (entity_prop_id == Settings.PROP_NONE) {
      return null;
    }
    if (objs == null) {
      return null;
    }

    ArrayList<Entity> ents = new ArrayList();
    if (objs.length == 0) {
      return ents;
    }

    final int prop_vals[] = (int[]) objs[0];
    final String names[] = (String[]) objs[1];
    final String g_names[] = (String[]) objs[2];
    final String p_names[] = (String[]) objs[3];
    final String propNameV[] = (String[]) objs[4]; // only 1 element expected
    final int[] ent_values;
    final int size = prop_vals.length;
    final String entity_prop_id_name = propNameV[0];

    for (int i = 0; i < size; i++) {
      int prop_val = prop_vals[i];
      String name = names[i];
      String g_name = g_names[i];
      String p_name = p_names[i];
      Entity newEnt =
          new Entity(entity_prop_id, entity_prop_id_name, prop_val, name, g_name, p_name);
      ents.add(newEnt);
    }

    return ents;
  }

  private List<List<Entity>> ipcGetEntitiesV2(final int[] expIds, final int entity_prop_id) {
    List<List<Entity>> entsList = new ArrayList();
    if (expIds.length == 0) {
      return entsList;
    }
    final Object[] expEntities = ipcRawGetEntitiesV2(expIds, entity_prop_id);
    if (expEntities == null) {
      return entsList; // weird;
    }
    for (int ii = 0; ii < expEntities.length; ii++) {
      final Object[] objs = (Object[]) expEntities[ii];
      List<Entity> entities = processGetEntities(objs, entity_prop_id);
      entsList.add(entities);
    }
    return entsList;
  }

  private Object[] ipcRawGetEntitiesV2(final int[] expIds, final int ekind) {
    synchronized (IPC.lock) {
      w_IPC.IPC().send("getEntitiesV2");
      w_IPC.IPC().send(0);
      w_IPC.IPC().send(expIds);
      w_IPC.IPC().send(ekind);
      return (Object[]) w_IPC.IPC().recvObject();
    }
  }
}

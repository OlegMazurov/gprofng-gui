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

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.experiment_props.ExperimentProperties;
import org.gprofng.mpmt.experiment_props.PropDescriptor;
import org.gprofng.mpmt.settings.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Application metrics of the loaded experiments: the metric tree (fetched from the engine on
 * demand) and which metrics are shown, per engine experiment id.
 */
final class AppMetricsModel {
  /** Comma separated metric names shown by default; the first varying metric if unset. */
  private static final String DEFAULT_PROPERTY = "gprofng.appmetrics";

  private static final Preferences PREFS =
      Preferences.userNodeForPackage(AppMetricsModel.class).node("appmetrics");

  private final AnWindow w_IPC;
  private final Map<Integer, AppMetricTree> trees = new HashMap<>(); // null value: no metrics
  private final Map<Integer, List<String>> selections = new HashMap<>();
  private final Map<Integer, Layout> layouts = new HashMap<>();

  /** How the range of the charts is chosen. */
  enum ScaleMode {
    VISIBLE("Visible range"),
    EXPERIMENT("Whole experiment"),
    SHARED("Same for all rows");

    final String label;

    ScaleMode(String label) {
      this.label = label;
    }
  }

  /** How the selected metrics are laid out. */
  static final class Layout {
    final boolean sameRow; // all the metrics in one row, else a row for each
    final boolean stacked; // in one row: stacked, else drawn over each other (unused otherwise)
    final ScaleMode scale;

    Layout(boolean sameRow, boolean stacked, ScaleMode scale) {
      this.sameRow = sameRow;
      this.stacked = stacked;
      this.scale = scale;
    }
  }

  /** Chart properties of an experiment's metrics row, with their scales. */
  static final class Charts {
    final List<PropDescriptor> props = new ArrayList<>();
    final List<ChartStyle> scales = new ArrayList<>();
  }

  AppMetricsModel(final AnWindow awindow) {
    this.w_IPC = awindow;
  }

  /** Forget the fetched trees (experiments may have been reloaded); selections are kept. */
  synchronized void reset() {
    trees.clear();
  }

  /** @return the experiment's metrics, or null if it has none */
  synchronized AppMetricTree tree(final int expId) { // IPC!
    if (!trees.containsKey(expId)) {
      final Object[] raw;
      synchronized (IPC.lock) {
        w_IPC.IPC().send("getAppMetricTree");
        w_IPC.IPC().send(expId);
        raw = (Object[]) w_IPC.IPC().recvObject();
      }
      trees.put(expId, AppMetricTree.fromIpc(raw));
    }
    return trees.get(expId);
  }

  /** The charts of an experiment's metrics row: one per metric, or a single one with all. */
  synchronized Charts charts(final int expId) { // IPC!
    final Charts charts = new Charts();
    final AppMetricTree tree = tree(expId);
    final List<String> names = selection(expId);
    if (tree == null || names.isEmpty()) {
      return charts;
    }
    final Layout layout = layout(expId);
    // experiment ranges, from zero unless negative; a stack adds up its members
    final int n = names.size();
    final double[] lo = new double[n];
    final double[] hi = new double[n];
    for (int i = 0; i < n; i++) {
      final int m = tree.indexOf(names.get(i));
      lo[i] = Math.min(0, tree.mins[m]);
      hi[i] = Math.max(0, tree.maxs[m]);
    }
    if (layout.sameRow && n > 1) {
      double min = 0;
      double max = 0;
      for (int i = 0; i < n; i++) {
        if (layout.stacked) {
          min += lo[i];
          max += hi[i];
        } else {
          min = Math.min(min, lo[i]);
          max = Math.max(max, hi[i]);
        }
      }
      final String key = String.join(String.valueOf(STACK_SEPARATOR), names);
      final String title = String.join(layout.stacked ? " + " : ", ", names);
      charts.props.add(new PropDescriptor(key, title, Settings.PROP_NONE, null, null));
      charts.scales.add(
          (layout.scale == ScaleMode.VISIBLE ? ChartStyle.VISIBLE : ChartStyle.fixed(min, max))
              .withStacked(layout.stacked));
      return charts;
    }
    double sharedMin = 0;
    double sharedMax = 0;
    for (int i = 0; i < n; i++) {
      sharedMin = Math.min(sharedMin, lo[i]);
      sharedMax = Math.max(sharedMax, hi[i]);
    }
    for (int i = 0; i < n; i++) {
      charts.props.add(new PropDescriptor(names.get(i), names.get(i), Settings.PROP_NONE, null, null));
      switch (layout.scale) {
        case VISIBLE -> charts.scales.add(ChartStyle.VISIBLE);
        case EXPERIMENT -> charts.scales.add(ChartStyle.fixed(lo[i], hi[i]));
        case SHARED -> charts.scales.add(ChartStyle.fixed(sharedMin, sharedMax));
      }
    }
    return charts;
  }

  /** Joins the metric names of a stacked chart in its property name (same as the engine's). */
  static final char STACK_SEPARATOR = AppMetricTree.STACK_SEPARATOR;

  synchronized Layout layout(final int expId) {
    Layout layout = layouts.get(expId);
    if (layout == null) {
      layout = loadLayout(expId);
      layouts.put(expId, layout);
    }
    return layout;
  }

  synchronized void setLayout(final int expId, final Layout layout) {
    layouts.put(expId, layout);
    final String key = prefsKey(expId);
    if (key != null) {
      PREFS.putBoolean(key + ".samerow", layout.sameRow);
      PREFS.putBoolean(key + ".stacked", layout.stacked);
      PREFS.put(key + ".scale", layout.scale.name());
    }
  }

  private Layout loadLayout(final int expId) {
    final String key = prefsKey(expId);
    boolean sameRow = false;
    boolean stacked = true;
    ScaleMode scale = ScaleMode.VISIBLE;
    if (key != null) {
      // ".stacked" used to mean "one stacked row"
      sameRow = PREFS.getBoolean(key + ".samerow", PREFS.getBoolean(key + ".stacked", false));
      stacked = PREFS.getBoolean(key + ".stacked", true);
      try {
        scale = ScaleMode.valueOf(PREFS.get(key + ".scale", scale.name()));
      } catch (IllegalArgumentException e) {
        // keep the default
      }
    }
    return new Layout(sameRow, stacked, scale);
  }

  /** Names of the metrics to show for an experiment, in display order. */
  synchronized List<String> selection(final int expId) { // IPC!
    final AppMetricTree tree = tree(expId);
    final List<String> result = new ArrayList<>();
    if (tree == null) {
      return result;
    }
    List<String> chosen = selections.get(expId);
    if (chosen == null) {
      chosen = loadSelection(expId);
      if (chosen == null) {
        chosen = defaultSelection(tree);
      }
      selections.put(expId, chosen);
    }
    for (String name : chosen) {
      if (tree.indexOf(name) >= 0) {
        result.add(name);
      }
    }
    return result;
  }

  /** Engine ids of the experiments whose metrics are known (fetched while building the rows). */
  synchronized List<Integer> expIdsWithMetrics() {
    final List<Integer> ids = new ArrayList<>();
    for (Map.Entry<Integer, AppMetricTree> e : trees.entrySet()) {
      if (e.getValue() != null) {
        ids.add(e.getKey());
      }
    }
    java.util.Collections.sort(ids);
    return ids;
  }

  /** The experiment's metrics if already fetched; never performs IPC. */
  synchronized AppMetricTree cachedTree(final int expId) {
    return trees.get(expId);
  }

  synchronized void setSelection(final int expId, final List<String> names) {
    selections.put(expId, new ArrayList<>(names));
    final String key = prefsKey(expId);
    if (key != null) {
      final String value = String.join("\n", names);
      if (value.length() <= Preferences.MAX_VALUE_LENGTH) {
        PREFS.put(key, value);
      }
    }
  }

  // The choice is remembered across sessions per experiment (by its name, which is its path).
  private String prefsKey(final int expId) {
    for (ExperimentProperties props : w_IPC.getExperimentProperties().getAllExperimentProperties()) {
      if (props.getID() == expId) {
        return "exp." + Integer.toHexString(props.getName().hashCode());
      }
    }
    return null;
  }

  private List<String> loadSelection(final int expId) {
    final String key = prefsKey(expId);
    final String value = key == null ? null : PREFS.get(key, null);
    if (value == null) {
      return null;
    }
    final List<String> names = new ArrayList<>();
    for (String name : value.split("\n")) {
      if (!name.isEmpty()) {
        names.add(name);
      }
    }
    return names;
  }

  private static List<String> defaultSelection(final AppMetricTree tree) {
    final List<String> names = new ArrayList<>();
    final String configured = System.getProperty(DEFAULT_PROPERTY);
    if (configured != null) {
      for (String name : configured.split(",")) {
        if (tree.indexOf(name.trim()) >= 0) {
          names.add(name.trim());
        }
      }
    }
    for (int i = 0; names.isEmpty() && i < tree.size(); i++) {
      if (tree.maxs[i] > tree.mins[i]) { // not constant
        names.add(tree.names[i]);
      }
    }
    return names;
  }
}

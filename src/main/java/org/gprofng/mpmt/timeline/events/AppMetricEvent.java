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


package org.gprofng.mpmt.timeline.events;

import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.timeline2.AppMetricTree;
import org.gprofng.mpmt.timeline2.TimelineVariable;
import java.util.List;

/**
 * A selected application metrics sample for the Selection Details panel: its timestamp and the
 * values of the metrics shown in the selected row.
 */
public final class AppMetricEvent extends EventDetail {
  /** Rows the details panel can show (EventDetails.NUM_EVT_SPC_LBL); the timestamp is one. */
  public static final int MAX_ROWS = 40;

  private final String[] labels;
  private final String[] info;

  /**
   * @param time absolute timestamp of the sample (ns)
   * @param names metric names, in row order
   * @param values their values at this sample; AppMetricTree.MISSING_VALUE if missing
   */
  public AppMetricEvent(
      final long time, final long time_adjust, final String[] names, final double[] values) {
    super(time, time_adjust);
    final int shown = Math.min(names.length, MAX_ROWS - 1);
    final boolean truncated = shown < names.length;
    labels = new String[1 + shown + (truncated ? 1 : 0)];
    info = new String[labels.length];
    labels[0] = "Timestamp (sec.):";
    info[0] = TimelineVariable.strTimestamp(getTimestamp());
    for (int i = 0; i < shown; i++) {
      labels[1 + i] = names[i] + ":";
      info[1 + i] =
          values[i] == AppMetricTree.MISSING_VALUE ? "n/a" : AppMetricTree.format(values[i]);
    }
    if (truncated) {
      labels[labels.length - 1] = "...";
      info[info.length - 1] = (names.length - shown) + " more";
    }
  }

  @Override
  public long getStack() {
    return DetailsIPC.INVALID_STACK_ID;
  }

  @Override
  public List<StackState> getStackStates() {
    return null;
  }

  // fresh arrays each time: the panel re-applies labels only when the array differs
  @Override
  public String[] getEventSpecificLabels() {
    return labels.clone();
  }

  @Override
  public String[] getEventSpecificInfo() {
    return info.clone();
  }
}

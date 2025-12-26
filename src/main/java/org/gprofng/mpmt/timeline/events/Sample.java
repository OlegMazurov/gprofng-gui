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

import org.gprofng.mpmt.AnDouble;
import org.gprofng.mpmt.AnInteger;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.timeline2.TimelineVariable;
import java.awt.Color;
import java.util.List;
import javax.swing.*;

public class Sample extends EventDetail implements DurationEvent {
  private static final double DIV = 1000000000.0;
  private static ImageIcon[] allIcons;
  private static Object allIconsLock = new Object();
  private static String[] allLabels;
  private static Object allLabelsLock = new Object();

  private static final String[] LABELS = {
    AnLocale.getString("Process:"), // NOI18N
    AnLocale.getString("Sample Number:"), // NOI18N
    AnLocale.getString("Sample Start Label:"), // NOI18N
    AnLocale.getString("Sample End Label:"), // NOI18N
    AnLocale.getString("Start Time (sec.):"), // NOI18N
    AnLocale.getString("End Time (sec.):"), // NOI18N
    AnLocale.getString("Duration (sec.):"), // NOI18N
  };
  //    private final static char[] MNEMONICS = {
  //            AnLocale.getString('E', "MNEM_SAMPLE_EXPERIMENT_NAME"), // NOI18N
  //            AnLocale.getString('m', "MNEM_SAMPLE_SAMPLE_NUMBER"), // NOI18N
  //            AnLocale.getString('L', "MNEM_SAMPLE_SAMPLE_START_LABEL"), // NOI18N
  //            AnLocale.getString('b', "MNEM_SAMPLE_SAMPLE_END_LABEL"), // NOI18N
  //            AnLocale.getString('a', "MNEM_SAMPLE_START_TIME"), // NOI18N
  //            AnLocale.getString('n', "MNEM_SAMPLE_END_TIME"), // NOI18N
  //            AnLocale.getString('D', "MNEM_SAMPLE_DURATION"), // NOI18N
  //    };

  // passed to constructor
  private final int number;
  private final String start_label, end_label;
  private final long duration; // in nanosec
  private final long total; // resource usage in nanosec
  private final long[] mstates; // resource usage in nanosec
  private final String expname;
  private final String[] eventSpecificInfo;
  private final String[] eventSpecificLabels;

  // Constructor
  public Sample(
      int number,
      String start_label,
      String end_label,
      long start_time,
      long end_time,
      long total,
      long[] mstates,
      String expname,
      long time_adjust) {
    super(end_time, time_adjust); // event timestamps are end times, not start times

    this.number = number;
    this.start_label = start_label;
    this.end_label = end_label;
    this.duration = end_time - start_time;
    this.total = total;
    this.mstates = mstates;
    this.expname = expname;
    this.eventSpecificInfo = initEventSpecificInfo();
    this.eventSpecificLabels = initEventSpecificLabels();
  }

  public long getStack() {
    return DetailsIPC.INVALID_STACK_ID;
  }

  public final List<StackState> getStackStates() {
    return null;
  }

  public String getStartLabel() {
    return start_label;
  }

  public String getEndLabel() {
    return end_label;
  }

  public long getTotalTime() {
    return total;
  }

  public long getDuration() {
    return duration;
  }

  public Object getData() {
    return mstates;
  }

  public long[] getMStates() {
    return mstates;
  }

  public String[] getEventSpecificLabels() {
    String[] rc = this.eventSpecificLabels.clone();
    return rc;
  }

  private String[] initEventSpecificLabels() {
    synchronized (allLabelsLock) {
      if (allLabels == null) {
        int idx = 0;
        allLabels = new String[LABELS.length + TimelineVariable.NUM_SOLARIS_MSTATES];
        for (int ii = 0; ii < LABELS.length; ii++) {
          allLabels[idx++] = LABELS[ii];
        }
        for (int ii = 0; ii < TimelineVariable.NUM_SOLARIS_MSTATES; ii++) {
          allLabels[idx++] = TimelineVariable.mstate_info.getNameByDisplayIdx(ii);
        }
      }
      return allLabels;
    }
  }

  public String[] getEventSpecificInfo() {
    String[] rc = eventSpecificInfo.clone();
    return rc;
  }

  private String[] initEventSpecificInfo() {
    String[] strings;
    strings = new String[LABELS.length + TimelineVariable.NUM_SOLARIS_MSTATES];
    strings[0] = expname;
    strings[1] = new AnInteger(number).toString();
    strings[2] = start_label;
    strings[3] = end_label;
    strings[4] = TimelineVariable.strTimestamp(super.getTimestamp() - duration);
    strings[5] = TimelineVariable.strTimestamp(super.getTimestamp());
    strings[6] = TimelineVariable.strTimestamp(duration);
    //        int numLabels = LABELS.length + TimelineVariable.NUM_SOLARIS_MSTATES;
    for (int ii = 0; ii < mstates.length; ii++) {
      int idx = TimelineVariable.mstate_info.getDisplayIdx(ii);
      if (idx >= 0 && idx < TimelineVariable.NUM_SOLARIS_MSTATES) {
        double time = mstates[ii];
        double percent = total == 0 ? total : (time / total) * 100;
        int labelIdx = LABELS.length + idx; // numLabels-1-idx;
        strings[labelIdx] = new AnDouble(time / DIV).toPercentQuote(percent);
      } else {
        ; // unexpected
      }
    }
    return strings;
  }

  public ImageIcon[] getEventSpecificIcons() {
    synchronized (allIconsLock) {
      if (allIcons == null) {
        allIcons = new ImageIcon[LABELS.length + TimelineVariable.NUM_SOLARIS_MSTATES];
        int idx = LABELS.length;
        for (int ii = 0; ii < TimelineVariable.NUM_SOLARIS_MSTATES; ii++) {
          Color c = TimelineVariable.mstate_info.getColorByDisplayIdx(ii);
          allIcons[idx++] = new ImageIcon(StackState.createIcon(c));
        }
      }
      return allIcons;
    }
  }

  //    public char[] getEventSpecificMnemonics() {
  //	return MNEMONICS;
  //    }
}

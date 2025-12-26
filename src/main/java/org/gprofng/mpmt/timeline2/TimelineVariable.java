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

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.metrics.MetricColors;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public final class TimelineVariable {

  public static final Color SELECT_COLOR = new Color(98, 98, 98);
  private static final double TIMESTAMP_MULT = .000000001;
  private static final NumberFormat TIMESTAMP_FMT;
  private static final Object syncFormat = new Object();
  private static final Color unknownColor = Color.BLACK;

  static {
    TIMESTAMP_FMT = new DecimalFormat();
    TIMESTAMP_FMT.setMaximumFractionDigits(6);
    TIMESTAMP_FMT.setMinimumFractionDigits(6);
    TIMESTAMP_FMT.setGroupingUsed(false);
  }

  public static String strTimestamp(final long time) {
    String t;
    synchronized (syncFormat) { // begin critical section: format is not thread safe
      t = TIMESTAMP_FMT.format(time * TIMESTAMP_MULT);
    } // end of critical section
    return t;
  }

  // ----- mapping states to colors and names -----
  public static class StatesDisplayInfo {
    public final String statePropName;
    private final String[] displayNames;
    private final Color[] displayColors;
    private final int[] raw2displayIdxTable;

    public StatesDisplayInfo(
        String statePropName,
        String[] displayNames,
        Color[] displayColors,
        int[] raw2displayIdxTable) {
      this.statePropName = statePropName;
      this.displayNames = displayNames;
      this.displayColors = displayColors;
      this.raw2displayIdxTable = raw2displayIdxTable;
    }

    public final long[] raw2displayValues(final long[] rawValues) {
      if (raw2displayIdxTable == null) {
        return rawValues;
      }
      final long displayValues[] = new long[raw2displayIdxTable.length];
      for (int i = 0; i < rawValues.length; i++) {
        int idx = getDisplayIdx(i);
        displayValues[idx] += rawValues[i];
      }
      return displayValues;
    }

    public final int getDisplayIdx(int rawIdx) {
      if (raw2displayIdxTable == null) {
        return rawIdx;
      }
      if (rawIdx < 0 || rawIdx >= raw2displayIdxTable.length) {
        return raw2displayIdxTable.length - 1; // <unknown> bucket
      }
      return raw2displayIdxTable[rawIdx];
    }

    public final String getNameByRawIdx(int rawIdx) {
      int idx = getDisplayIdx(rawIdx);
      if (displayNames == null || idx < 0 || idx >= displayNames.length) {
        return Integer.toString(rawIdx);
      }
      return displayNames[idx];
    }

    public final String getNameByDisplayIdx(int dispIdx) {
      if (displayNames == null || dispIdx < 0 || dispIdx >= displayNames.length) {
        return Integer.toString(dispIdx) + "?"; // weird
      }
      return displayNames[dispIdx];
    }

    public final Color getColorByRawIdx(int rawIdx) {
      int idx = getDisplayIdx(rawIdx);
      if (displayColors == null || idx < 0 || idx >= displayColors.length) {
        return unknownColor;
      }
      return displayColors[idx];
    }

    public final Color getColorByDisplayIdx(int dispIdx) {
      if (displayColors == null || dispIdx < 0 || dispIdx >= displayColors.length) {
        return unknownColor; // weird
      }
      return displayColors[dispIdx];
    }

    public final int getNumNames() {
      if (displayNames == null) {
        return 0;
      }
      return displayNames.length;
    }
  }

  // Remap the microstates to display categories and order.
  // Note: use same display order of LMS_* in: er.rc, TimelineVariable.java,
  // Ovw_data.h, BaseMetricTreeNode.cc and Experiment.cc metric registration
  public static final int NUM_SOLARIS_MSTATES = 10; // Solaris Microstates are 0-9
  private static final int[ /*lms*/]
      MSTATE_TO_OVERVIEW_IDX = { // supply LMS value as index, returns display idx
    0, // 0 LMS_USER
    1, // 1 LMS_SYSTEM
    2, // 2 LMS_TRAP
    4, // 3 LMS_TFAULT
    3, // 4 LMS_DFAULT
    5, // 5 LMS_KFAULT
    9, // 6 LMS_USER_LOCK
    8, // 7 LMS_SLEEP
    7, // 8 LMS_WAIT_CPU
    6, // 9 LMS_STOPPED
    10, // 10 LMS_LINUX_CPU
    11, // 11 LMS_KERNEL_CPU
    12, // 12 <unknown>
  };

  // Names sorted by display order (not LMS_* order)
  private static final String[] MSTATE_DESC = { // YXXX should come from property names
    AnLocale.getString("User CPU"),
    AnLocale.getString("System CPU"),
    AnLocale.getString("Trap CPU"),
    AnLocale.getString("Data Page Fault"),
    AnLocale.getString("Text Page Fault"),
    AnLocale.getString("Kernel Page Fault"),
    AnLocale.getString("Stopped"),
    AnLocale.getString("Wait CPU"),
    AnLocale.getString("Sleep"),
    AnLocale.getString("User Lock"),
    AnLocale.getString("User+System CPU"), // NOT a Solaris microstate
    AnLocale.getString("Kernel CPU"), // NOT a Solaris microstate
    AnLocale.getString("<Unknown>"), // NOT a Solaris microstate
  };

  // Colors sorted by display order (not LMS_* order)
  private static final Color[ /*ovw idx*/] MSTATE_COLORS = {
    MetricColors.getColor("user"), // 0: 0 LMS_USER
    MetricColors.getColor("system"), // 1: 1 LMS_SYSTEM
    MetricColors.getColor("trap"), // 2: 2 LMS_TRAP
    MetricColors.getColor("datapfault"), // 3: 4 LMS_DFAULT
    MetricColors.getColor("textpfault"), // 4: 3 LMS_TFAULT
    MetricColors.getColor("kernelpfault"), // 5: 5 LMS_KFAULT
    MetricColors.getColor("stop"), // 6: 9 LMS_STOPPED
    MetricColors.getColor("wait"), // 7: 8 LMS_WAIT_CPU
    MetricColors.getColor("sleep"), // 8: 7 LMS_SLEEP
    MetricColors.getColor("lock"), // 9: 6 LMS_USER_LOCK
    MetricColors.getColor("user"), // 10:10 LMS_LINUX_CPU
    MetricColors.getColor("user"), // 11:11 LMS_KERNEL_CPU
    Color.black, // 12:12 <unknown>
  };

  public static final StatesDisplayInfo mstate_info =
      new StatesDisplayInfo("MSTATE", MSTATE_DESC, MSTATE_COLORS, MSTATE_TO_OVERVIEW_IDX);

  // YXXX maybe we should get these from properties
  private static final String[] TL_HEAP_HTYPE_DESCS = {
    AnLocale.getString("Allocation"),
    AnLocale.getString("Free"),
    AnLocale.getString("Reallocation"),
    AnLocale.getString("MemoryMap"),
    AnLocale.getString("MemoryUnmap"),
  };

  private static final Color[] TL_HEAP_HTYPE_COLOR_TABLE = {
    MetricColors.BLUE_VIOLET, // malloc
    MetricColors.CYAN, // free
    MetricColors.ORANGE, // realloc
    MetricColors.RED, // mmap
    MetricColors.DARK_GREY, // munmap
  };

  public static final StatesDisplayInfo htype_info =
      new StatesDisplayInfo("HTYPE", TL_HEAP_HTYPE_DESCS, TL_HEAP_HTYPE_COLOR_TABLE, null);

  // YXXX maybe we should get these from properties
  private static final String[] TL_IOTRACE_IOTYPE_DESCS = {
    AnLocale.getString("Read"),
    AnLocale.getString("Write"),
    AnLocale.getString("Open"),
    AnLocale.getString("Close"),
    AnLocale.getString("Other I/O"),
    AnLocale.getString("Read error"),
    AnLocale.getString("Write error"),
    AnLocale.getString("Open error"),
    AnLocale.getString("Close error"),
    AnLocale.getString("Other I/O error"),
  };

  private static final Color[] TL_IOTRACE_IOTYPE_COLOR_TABLE = {
    // read, write, open, close, otherio, readerror, writeerror, openerror, closeerror, otherioerror
    MetricColors.RED, // read
    MetricColors.ORANGE, // write
    MetricColors.CYAN, // open
    MetricColors.DARK_GREY, // close
    MetricColors.BLUE_VIOLET, // otherio
    MetricColors.BLACK, // read error
    MetricColors.BLACK, // write error
    MetricColors.BLACK, // open error
    MetricColors.BLACK, // close error
    MetricColors.BLACK, // other io  error
  };

  public static final StatesDisplayInfo iotype_info =
      new StatesDisplayInfo("IOTYPE", TL_IOTRACE_IOTYPE_DESCS, TL_IOTRACE_IOTYPE_COLOR_TABLE, null);

  private static final Color[] TL_UNKNOWN_GENERIC_COLOR_TABLE = {
    MetricColors.BLUE_VIOLET,
    MetricColors.GOLDEN_YELLOW,
    MetricColors.COBALT,
    MetricColors.LIGHT_GREEN,
    MetricColors.FUSCHIA,
    MetricColors.GREEN,
    MetricColors.ORANGE,
    MetricColors.DARK_GREY,
    MetricColors.RED,
    MetricColors.CYAN,
    MetricColors.GRAY,
    MetricColors.BLUE,
    MetricColors.BLACK,
  };

  private static final StatesDisplayInfo generic_info =
      new StatesDisplayInfo("", null, TL_UNKNOWN_GENERIC_COLOR_TABLE, null);

  private static final StatesDisplayInfo[] info_list = {mstate_info, htype_info, iotype_info};

  public static final StatesDisplayInfo getStateInfo(final String propName) {
    for (int ii = 0; ii < info_list.length; ii++) {
      if (propName.equals(info_list[ii].statePropName)) {
        return info_list[ii];
      }
    }
    return generic_info;
  }
}

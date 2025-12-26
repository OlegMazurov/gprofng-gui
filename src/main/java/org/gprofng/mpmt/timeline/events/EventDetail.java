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

// @(#)EventDetail.java 1.21 11/06/09

package org.gprofng.mpmt.timeline.events;

import org.gprofng.mpmt.statecolors.StackState;
import java.util.List;
import javax.swing.ImageIcon;

public abstract class EventDetail {
  public static final int NO_CPU_INFO = -1;
  private static final String[] DUMMY_STRINGS = {};
  //    private final static char[]		DUMMY_MNEMONICS = { };
  private static final ImageIcon[] DUMMY_ICONS = {};

  // TODO: adjust should be moved to DBE so filters work as expected for time-adjust experiments
  protected final long timestamp; // adjusted timestamp, e.g. as viewed in timeline
  private final long org_time; // original timestamp

  public EventDetail(final long original_time, final long time_adjust) {
    timestamp = original_time - time_adjust;
    org_time = original_time;
  }

  public final long getTimestamp() {
    return timestamp;
  }

  public abstract long getStack(); // for none, DetailsIPC.INVALID_STACK_ID

  public abstract List<StackState> getStackStates(); // for none, returns NULL

  public String[] getEventSpecificLabels() {
    return DUMMY_STRINGS;
  }

  public String[] getEventSpecificInfo() {
    return DUMMY_STRINGS;
  }

  //    public char[] getEventSpecificMnemonics() {
  //	return DUMMY_MNEMONICS;
  //    }

  public ImageIcon[] getEventSpecificIcons() {
    return DUMMY_ICONS;
  }
}

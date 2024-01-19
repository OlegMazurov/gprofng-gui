/* Copyright (C) 2022-2024 Free Software Foundation

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

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnLong;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.timeline2.TimelineVariable;
import java.awt.Color;
import java.util.List;
import javax.swing.ImageIcon;

public final class ProfileEvent extends ExtendedEvent implements DurationEvent, StateEvent {
  private final String[] LABELS = {
    AnLocale.getString("Duration (msec.):"), // NOI18N
    AnLocale.getString("Thread State:") // NOI18N
  };

  private final int mstate;
  private final long interval; // in nanosec
  private final long duration; // visual duration, in nanosec
  private final int ompstate;
  private final String[] eventSpecificInfo;

  public ProfileEvent(
      int lwpid,
      int thrid,
      int cpuid,
      long ts,
      long stack,
      long func,
      List<StackState> stackStates,
      int mstate,
      long interval,
      int ompstate,
      long duration,
      long time_adjust) {
    super(lwpid, thrid, cpuid, ts, stack, func, time_adjust, stackStates);

    this.mstate = mstate;
    this.ompstate = ompstate; // YXXX unused for now
    this.interval = interval;
    this.duration = duration;
    this.eventSpecificInfo = initEventSpecificInfo();
  }

  public String[] getEventSpecificInfo() {
    String[] rc = this.eventSpecificInfo.clone();
    return rc;
  }

  private String[] initEventSpecificInfo() {
    String state = TimelineVariable.mstate_info.getNameByRawIdx(mstate);
    return new String[] {
      new AnLong(interval).toTime(0.000001), state,
    };
  }

  public String[] getEventSpecificLabels() {
    return LABELS;
  }

  public ImageIcon[] getEventSpecificIcons() {
    Color color = TimelineVariable.mstate_info.getColorByRawIdx(mstate);
    ImageIcon icon = new ImageIcon(StackState.createIcon(color));
    return new ImageIcon[] {
      null, icon,
    };
  }

  public long getDuration() {
    return duration;
  }

  public int getState() {
    return mstate;
  }
}

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

import org.gprofng.mpmt.AnAddress;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnLong;
import org.gprofng.mpmt.statecolors.StackState;
import java.util.List;

public class SyncEvent extends ExtendedEvent implements DurationEvent {
  protected final String[] LABELS = {
    AnLocale.getString("Duration (msec.):"), // NOI18N
    AnLocale.getString("Lock Address:") // NOI18N
  };

  public final long duration; // in nanosec
  public final long sobj; // synchronization object (f.e. mutex)
  private final String[] eventSpecificInfo;

  public SyncEvent(
      int lwpid,
      int thrid,
      int cpuid,
      long ts,
      long stack,
      long func,
      List<StackState> stackStates,
      long duration,
      long sobj,
      long time_adjust) {
    super(lwpid, thrid, cpuid, ts, stack, func, time_adjust, stackStates);

    this.duration = duration;
    this.sobj = sobj;
    this.eventSpecificInfo = initEventSpecificInfo();
  }

  public String[] getEventSpecificInfo() {
    String[] rc = this.eventSpecificInfo.clone();
    return rc;
  }

  private String[] initEventSpecificInfo() {

    return new String[] {new AnLong(duration).toTime(.000001), AnAddress.toHexString(sobj)};
  }

  public String[] getEventSpecificLabels() {
    return LABELS;
  }

  public long getDuration() {
    return duration;
  }
}

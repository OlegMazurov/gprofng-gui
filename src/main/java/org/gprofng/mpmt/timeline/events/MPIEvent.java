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

import org.gprofng.mpmt.AnInteger;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnLong;
import org.gprofng.mpmt.statecolors.StackState;
import java.util.List;

public final class MPIEvent extends ExtendedEvent implements DurationEvent {
  private final String[] LABELS = {
    AnLocale.getString("Duration (msec.):"), // NOI18N
    AnLocale.getString("Sub-Type:"), // NOI18N
    AnLocale.getString("Message Count:"), // NOI18N
    AnLocale.getString("Bytes:") // NOI18N
  };

  // This order has to match what's in data_pckts.h for MPI stuff
  private static final String[] MPITYPES = {
    "mpisend", // NOI18N
    "mpireceive", // NOI18N
    "mpisendrecv", // NOI18N
    "mpiother" // NOI18N
  };

  // passed to constructor
  private final long duration; // in nanosec
  private final int sub_type, msg_cnt;
  private final long msg_bytes;
  private final String[] eventSpecificInfo;

  public MPIEvent(
      int lwpid,
      int thrid,
      int cpuid,
      long ts,
      long stack,
      long func,
      List<StackState> stackStates,
      long duration,
      int msg_cnt,
      int sub_type,
      long msg_bytes,
      long time_adjust) {
    super(lwpid, thrid, cpuid, ts, stack, func, time_adjust, stackStates);

    this.duration = duration;
    this.sub_type = sub_type;
    this.msg_cnt = msg_cnt;
    this.msg_bytes = msg_bytes;
    this.eventSpecificInfo = initEventSpecificInfo();
  }

  public String[] getEventSpecificInfo() {
    String[] rc = this.eventSpecificInfo.clone();
    return rc;
  }

  private String[] initEventSpecificInfo() {
    return new String[] {
      new AnLong(duration).toTime(.000001),
      MPITYPES[sub_type],
      new AnInteger(msg_cnt).toString(),
      new AnLong(msg_bytes).toString()
    };
  }

  public String[] getEventSpecificLabels() {
    return LABELS;
  }

  public long getDuration() {
    return duration;
  }
}

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

import org.gprofng.mpmt.AnAddress;
import org.gprofng.mpmt.AnInteger;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.statecolors.StackState;
import java.util.List;

public final class DeadlockEvent extends ExtendedEvent {
  private static final String[] TYPES = {
    AnLocale.getString("Lock Held"), // NOI18N
    AnLocale.getString("Lock Requested"), // NOI18N
    AnLocale.getString("<Unknown>"), // NOI18N
  };
  private static final String[] DLTYPES = {
    AnLocale.getString("Potential Deadlock"), // NOI18N
    AnLocale.getString("Actual Deadlock"), // NOI18N
    AnLocale.getString("<Unknown>"), // NOI18N
  };

  private static final String TYPE = AnLocale.getString("Lock Event:"); // NOI18N
  private static final String DLTYPE = AnLocale.getString("Deadlock Type:"); // NOI18N
  private static final String DEADLOCK_ID = AnLocale.getString("Deadlock ID:"); // NOI18N
  private static final String ADDRESS = AnLocale.getString("Address:"); // NOI18N

  private static final String[] GENERIC_RACE_LABELS = {TYPE, DLTYPE, ADDRESS, DEADLOCK_ID};

  private final int type;
  private final int dltype;
  private final int deadlock_id;
  private final long vaddr;

  public DeadlockEvent(
      final int lwpid,
      final int thrid,
      final int cpuid,
      final long ts,
      final long stack,
      final long func,
      List<StackState> stackStates,
      final int type,
      final int dltype,
      final int deadlock_id,
      final long vaddr,
      long time_adjust) {
    super(lwpid, thrid, cpuid, ts, stack, func, time_adjust, stackStates);
    this.type = (type >= 0 && type < TYPES.length) ? type : TYPES.length - 1;
    this.dltype = (dltype >= 0 && dltype < DLTYPES.length) ? dltype : DLTYPES.length - 1;
    this.deadlock_id = deadlock_id;
    this.vaddr = vaddr;
  }

  public String[] getEventSpecificInfo() {
    return new String[] {
      TYPES[type],
      DLTYPES[dltype],
      AnAddress.toHexString(vaddr),
      new AnInteger(deadlock_id).toString()
    };
  }

  public String[] getEventSpecificLabels() {
    return GENERIC_RACE_LABELS;
  }
}

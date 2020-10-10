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

package org.gprofng.mpmt.timeline.events;

import org.gprofng.mpmt.AnAddress;
import org.gprofng.mpmt.AnInteger;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.statecolors.StackState;
import java.util.List;

public final class RaceEvent extends ExtendedEvent {
  private static final int WRITE_RACE = 0;
  private static final int WRITE_RACE_RED = 1;
  private static final int READ_RACE = 2;
  private static final int READ_RACE_RED = 3;

  private static final String[] TYPES = {
    AnLocale.getString("Write"), // NOI18N
    AnLocale.getString("Write Red"), // NOI18N
    AnLocale.getString("Read"), // NOI18N
    AnLocale.getString("Read Red") // NOI18N
  };

  private static final String TYPE = AnLocale.getString("Type:"); // NOI18N
  private static final String RACE_ID = AnLocale.getString("Race ID:"); // NOI18N
  private static final String ADDRESS = AnLocale.getString("Address:"); // NOI18N

  private static final String[] GENERIC_RACE_LABELS = {TYPE, ADDRESS, RACE_ID};

  private final int type;
  private final int race_id;
  private final long vaddr;

  public RaceEvent(
      final int lwpid,
      final int thrid,
      final int cpuid,
      final long ts,
      final long stack,
      final long func,
      List<StackState> stackStates,
      final int type,
      final int race_id,
      final long vaddr,
      long time_adjust) {
    super(lwpid, thrid, cpuid, ts, stack, func, time_adjust, stackStates);
    this.type = type;
    this.race_id = race_id;
    this.vaddr = vaddr;
  }

  public String[] getEventSpecificInfo() {
    // fixme what about redundancy flag bit?
    switch (type) {
      case WRITE_RACE:
      case WRITE_RACE_RED:
      case READ_RACE:
      case READ_RACE_RED:
        return new String[] {
          TYPES[type], AnAddress.toHexString(vaddr), new AnInteger(race_id).toString()
        };
    }

    return null;
  }

  public String[] getEventSpecificLabels() {
    switch (type) {
      case WRITE_RACE:
      case WRITE_RACE_RED:
      case READ_RACE:
      case READ_RACE_RED:
        return GENERIC_RACE_LABELS;
    }

    return null;
  }
}

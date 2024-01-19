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
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnLong;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.timeline2.TimelineVariable;
import java.awt.Color;
import java.util.List;
import javax.swing.ImageIcon;

public final class HeapEvent extends ExtendedEvent implements StateEvent {
  private static final int MALLOC = 0;
  private static final int FREE = 1;
  private static final int REALLOC = 2;
  private static final int MMAP = 3;
  private static final int MUNMAP = 4;

  private static final String TYPE = AnLocale.getString("Type:"); // NOI18N
  private static final String ALLOCATED = AnLocale.getString("Event Bytes Allocated:"); // NOI18N
  private static final String ADDRESS = AnLocale.getString("Address:"); // NOI18N
  private static final String OADDRESS = AnLocale.getString("Old Address:"); // NOI18N
  private static final String LEAKED = AnLocale.getString("Event Bytes Leaked:"); // NOI18N
  private static final String FREED = AnLocale.getString("Event Bytes Freed:"); // NOI18N
  private static final String HEAPSZ = AnLocale.getString("Net Bytes Allocated:"); // NOI18N
  private static final String LEAKSZ = AnLocale.getString("Net Bytes Leaked:"); // NOI18N

  private static final String[] MALLOC_LABELS = {
    TYPE, ALLOCATED, LEAKED, ADDRESS, HEAPSZ, LEAKSZ,
  };

  private static final String[] FREE_LABELS = {
    TYPE, FREED, ADDRESS, HEAPSZ, LEAKSZ,
  };

  private static final String[] REALLOC_LABELS = {
    TYPE, ALLOCATED, FREED, LEAKED, ADDRESS, OADDRESS, HEAPSZ, LEAKSZ,
  };

  private static final String[] MMAP_LABELS = {
    TYPE, ALLOCATED, FREED, LEAKED, ADDRESS, HEAPSZ, LEAKSZ,
  };

  private static final String[] MUNMAP_LABELS = {
    TYPE, ALLOCATED, FREED, ADDRESS, HEAPSZ, LEAKSZ,
  };

  private final int type;
  private final long allocated;
  private final long vaddr;
  private final long ovaddr;
  private final long leaked;
  private final long freed;
  private final long heapsz;
  private final long leaksz;

  private final String[] eventSpecificInfo;

  public HeapEvent(
      final int lwpid,
      final int thrid,
      final int cpuid,
      final long ts,
      final long stack,
      final long func,
      List<StackState> stackStates,
      final int type,
      final long allocated,
      final long vaddr,
      final long ovaddr,
      long leaked,
      long freed,
      long heapsz,
      long leaksz,
      long time_adjust) {
    super(lwpid, thrid, cpuid, ts, stack, func, time_adjust, stackStates);
    this.type = type;
    this.allocated = allocated;
    this.vaddr = vaddr;
    this.ovaddr = ovaddr;
    this.leaked = leaked;
    this.freed = freed;
    this.heapsz = heapsz;
    this.leaksz = leaksz;
    this.eventSpecificInfo = initEventSpecificInfo();
  }

  public String[] getEventSpecificInfo() {
    String[] rc = this.eventSpecificInfo.clone();
    return rc;
  }

  private String[] initEventSpecificInfo() {
    final String name = TimelineVariable.htype_info.getNameByRawIdx(type);
    switch (type) {
      case MALLOC:
        return new String[] {
          name,
          new AnLong(allocated).toString(),
          new AnLong(leaked).toString(),
          AnAddress.toHexString(vaddr),
          new AnLong(heapsz).toString(),
          new AnLong(leaksz).toString(),
        };
      case MMAP:
        return new String[] {
          name,
          new AnLong(allocated).toString(),
          new AnLong(freed).toString(),
          new AnLong(leaked).toString(),
          AnAddress.toHexString(vaddr),
          new AnLong(heapsz).toString(),
          new AnLong(leaksz).toString(),
        };
      case MUNMAP:
        return new String[] {
          name,
          new AnLong(allocated).toString(),
          new AnLong(freed).toString(),
          AnAddress.toHexString(vaddr),
          new AnLong(heapsz).toString(),
          new AnLong(leaksz).toString(),
        };
      case FREE:
        return new String[] {
          name,
          new AnLong(freed).toString(),
          AnAddress.toHexString(vaddr),
          new AnLong(heapsz).toString(),
          new AnLong(leaksz).toString(),
        };
      case REALLOC:
        return new String[] {
          name,
          new AnLong(allocated).toString(),
          new AnLong(freed).toString(),
          new AnLong(leaked).toString(),
          AnAddress.toHexString(vaddr),
          AnAddress.toHexString(ovaddr),
          new AnLong(heapsz).toString(),
          new AnLong(leaksz).toString(),
        };
    }

    return null;
  }

  public String[] getEventSpecificLabels() {
    switch (type) {
      case MALLOC:
        return MALLOC_LABELS;
      case FREE:
        return FREE_LABELS;
      case REALLOC:
        return REALLOC_LABELS;
      case MMAP:
        return MMAP_LABELS;
      case MUNMAP:
        return MUNMAP_LABELS;
    }

    return null;
  }

  public ImageIcon[] getEventSpecificIcons() {
    Color color = TimelineVariable.htype_info.getColorByRawIdx(type);
    ImageIcon icon = new ImageIcon(StackState.createIcon(color));
    return new ImageIcon[] {
      icon, null, null, null,
    };
  }

  public int getState() {
    return type;
  }
}

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

import org.gprofng.mpmt.AnInteger;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnLong;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.timeline2.TimelineVariable;
import java.awt.Color;
import java.util.List;
import javax.swing.ImageIcon;

public class IOEvent extends ExtendedEvent implements DurationEvent, StateEvent {
  protected final String[] LABELS = {
    AnLocale.getString("IO Type:"), // NOI18N
    AnLocale.getString("IO File Descriptor:"), // NOI18N
    AnLocale.getString("IO Bytes:"), // NOI18N
    AnLocale.getString("Duration (msec.):") // NOI18N
  };

  public final int type; // IO Type
  public final int fd; // File Descriptor
  public final long bytes; // Number of bytes
  public final long duration; // in nanosec
  private final String[] eventSpecificInfo;

  public IOEvent(
      int lwpid,
      int thrid,
      int cpuid,
      long ts,
      long stack,
      long func,
      List<StackState> stackStates,
      int type,
      int fd,
      long bytes,
      long duration,
      long time_adjust) {
    super(lwpid, thrid, cpuid, ts, stack, func, time_adjust, stackStates);

    this.type = type;
    this.fd = fd;
    this.bytes = bytes;
    this.duration = duration;
    this.eventSpecificInfo = initEventSpecificInfo();
  }

  public String[] getEventSpecificInfo() {
    String[] rc = this.eventSpecificInfo.clone();
    return rc;
  }

  private String[] initEventSpecificInfo() {
    String typename;
    typename = TimelineVariable.iotype_info.getNameByRawIdx(type);
    return new String[] {
      typename,
      new AnInteger(fd).toString(),
      new AnLong(bytes).toString(),
      new AnLong(duration).toTime(.000001)
    };
  }

  public String[] getEventSpecificLabels() {
    return LABELS;
  }

  public long getDuration() {
    return duration;
  }

  public ImageIcon[] getEventSpecificIcons() {
    Color color = TimelineVariable.iotype_info.getColorByRawIdx(type);
    ImageIcon icon = new ImageIcon(StackState.createIcon(color));
    return new ImageIcon[] {
      icon, null, null, null,
    };
  }

  public int getState() {
    return type;
  }
}

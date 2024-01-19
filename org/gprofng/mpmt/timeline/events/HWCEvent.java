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
import org.gprofng.mpmt.AnLong;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.statecolors.StackState;
import java.util.List;

public final class HWCEvent extends ExtendedEvent {
  private static final String INTERVAL = AnLocale.getString("Interval:"); // NOI18N
  private static final String FREQ = AnLocale.getString("Clock Freq (MHz):"); // NOI18N
  private static final String DATAVADDR = AnLocale.getString("Data Virtual Addr:"); // NOI18N
  private static final String DATAPADDR = AnLocale.getString("Data Physical Addr:"); // NOI18N
  private static final String PCVADDR = AnLocale.getString("PC Virtual Addr:"); // NOI18N
  private static final String PCPADDR = AnLocale.getString("PC Physical Addr:"); // NOI18N
  private static final String DATADESC = AnLocale.getString("Data Descriptor:"); // NOI18N
  private static final String[] LABELS = {INTERVAL};
  private static final String[] LABELS2 = {INTERVAL, FREQ};

  private static final String[] XLABELS = {
    INTERVAL, DATAVADDR, DATAPADDR, PCVADDR, PCPADDR, DATADESC
  };
  private static final String[] XLABELS2 = {
    INTERVAL, FREQ, DATAVADDR, DATAPADDR, PCVADDR, PCPADDR, DATADESC
  };

  // ABS encodings must match definitions in ABS.h
  private static final long ABS_NULL = 0x00L;
  private static final long ABS_CODE_RANGE = 0xFFL;
  private static final long ABS_RT_CODEMASK = 0x0FL;
  private static final long ABS_PP_CODEMASK = 0xF0L;

  private static final String[] ABS_RT_CODES = {
    "", // NOI18N
    AnLocale.getString("(Internal error)"), // NOI18N
    AnLocale.getString("(Backtracking blocked)"), // NOI18N
    AnLocale.getString("(Backtracking incomplete)"), // NOI18N
    AnLocale.getString("(Register lost)"), // NOI18N
    AnLocale.getString("(Invalid address)"), // NOI18N
    AnLocale.getString("(UNKNOWN)") // NOI18N
  };

  private static final String[] ABS_PP_CODES = {
    "", // NOI18N
    AnLocale.getString("(No Symbolic Information)"), // NOI18N
    AnLocale.getString("(Backtracking failed)"), // NOI18N
    AnLocale.getString("(Blocked by branch target)"), // NOI18N
    AnLocale.getString("(<POST-PROCESSING ERROR>)") // NOI18N
  };

  // passed to constructor
  private final AnWindow m_window;
  private final long interval;
  private final long eventEA, eventPA, eventVPC, eventPPC;
  private final int cpu_freq;
  private final String[] eventSpecificInfo;
  private final String[] eventSpecificLabels;

  public HWCEvent(
      final AnWindow awindow,
      int lwpid,
      int thrid,
      int cpuid, // IPC!!
      long ts,
      long stack,
      long func,
      List<StackState> stackStates,
      long interval,
      long eventEA,
      long eventPA,
      long eventVPC,
      long eventPPC,
      int cpu_freq,
      long time_adjust) {
    super(lwpid, thrid, cpuid, ts, stack, func, time_adjust, stackStates);

    this.m_window = awindow;
    this.interval = interval;
    this.eventEA = eventEA;
    this.eventPA = eventPA;
    this.eventVPC = eventVPC;
    this.eventPPC = eventPPC;
    this.cpu_freq = cpu_freq;
    this.eventSpecificInfo = ipcInitEventSpecificInfo();
    this.eventSpecificLabels = initEventSpecificLabels();
  }

  private String ipcGetDataspaceTypeDesc() {
    long stack = super.getStack();
    return ipcGetDataspaceTypeDesc(stack);
  }

  private String getDataAddress() {
    int rt_code, pp_code;

    if (0 > eventEA || eventEA > ABS_CODE_RANGE) return AnAddress.toHexString(eventEA);
    else {
      rt_code = (int) (eventEA & ABS_RT_CODEMASK);
      pp_code = (int) (eventEA & ABS_PP_CODEMASK) / 0xF;
      if (rt_code > ABS_RT_CODES.length) rt_code = ABS_RT_CODES.length - 1;

      if (pp_code > ABS_PP_CODES.length) pp_code = ABS_PP_CODES.length - 1;

      return ABS_PP_CODES[pp_code] + ABS_RT_CODES[rt_code];
    }
  }

  private String getPhysicalAddress() {
    if (eventPA != ABS_NULL) return AnAddress.toHexString(eventPA);
    else return "";
  }

  private String getVirtualPC() {
    if (eventVPC != ABS_NULL) return AnAddress.toHexString(eventVPC);
    else return "";
  }

  private String getPhysicalPC() {
    if (eventPPC != ABS_NULL) return AnAddress.toHexString(eventPPC);
    else return "";
  }

  public String[] getEventSpecificInfo() {
    String[] rc = eventSpecificInfo.clone();
    return rc;
  }

  // eventEA is only defined for XHWC records
  private String[] ipcInitEventSpecificInfo() {
    if ((cpu_freq == 0) && (eventEA == ABS_NULL))
      return new String[] {new AnLong(interval).toString()};
    else if (eventEA == ABS_NULL)
      return new String[] {new AnLong(interval).toString(), new AnInteger(cpu_freq).toString()};
    else if (cpu_freq == 0)
      return new String[] {
        new AnLong(interval).toString(),
        getDataAddress(),
        getPhysicalAddress(),
        getVirtualPC(),
        getPhysicalPC(),
        ipcGetDataspaceTypeDesc()
      };
    else
      return new String[] {
        new AnLong(interval).toString(),
        new AnInteger(cpu_freq).toString(),
        getDataAddress(),
        getPhysicalAddress(),
        getVirtualPC(),
        getPhysicalPC(),
        ipcGetDataspaceTypeDesc()
      };
  }

  public String[] getEventSpecificLabels() {
    String[] rc = this.eventSpecificLabels.clone();
    return rc;
  }

  private String[] initEventSpecificLabels() {
    if ((cpu_freq == 0) && (eventEA == ABS_NULL)) return LABELS;
    else if (eventEA == ABS_NULL) return LABELS2;
    else if (cpu_freq == 0) return XLABELS;
    else return XLABELS2;
  }

  // Native methods from liber_dbe.so
  private String ipcGetDataspaceTypeDesc(long stack) {
    synchronized (IPC.lock) {
      m_window.IPC().send("getDataspaceTypeDesc"); // NOI18N
      m_window.IPC().send(stack);
      return m_window.IPC().recvString();
    }
  }
}

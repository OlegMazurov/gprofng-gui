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

import org.gprofng.mpmt.statecolors.StackState;
import java.util.Collections;
import java.util.List;

public abstract class ExtendedEvent extends EventDetail {
  private final int lwpid;
  private final int thrid;
  private final int cpuid;

  private final Long stack;
  private final List<StackState> stackStates;
  private final long func; // leaf function

  public ExtendedEvent(
      final int lwpid,
      final int thrid,
      final int cpuid,
      final long ts,
      final long stack,
      final long func,
      long time_adjust,
      final List<StackState> stackStates) {
    super(ts, time_adjust);

    this.lwpid = lwpid;
    this.thrid = thrid;
    this.cpuid = cpuid;

    if (stack != 0)
      this.stack = stack;
    else this.stack = null;
    this.stackStates = Collections.unmodifiableList(stackStates);
    this.func = func;
  }

  public final int getLWP() {
    return lwpid;
  }

  public final int getThread() {
    return thrid;
  }

  public final int getCPU() {
    return cpuid;
  }

  @Override
  public final long getStack() {
    return stack.longValue();
  }

  @Override
  public final List<StackState> getStackStates() {
    return stackStates;
  }
}

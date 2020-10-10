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

package org.gprofng.mpmt.settings;

import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;

public class CallTreeSetting extends Setting {

  private static final int defaultThreshold = 1;

  private int threshold;

  public CallTreeSetting() {
    threshold = UserPref.getInstance().getCallStackThreshold();
  }

  public boolean validValue(int value) {
    return value >= 0 && value <= 100;
  }

  public boolean setThreshold(Object originalSource, int newThreshold) {
    if (!validValue(newThreshold)) {
      return false;
    }
    if (newThreshold != threshold) {
      setValueAndFireChangeEvent(originalSource, this, newThreshold);
    }
    return true;
  }

  public int getThreshold() {
    return threshold;
  }

  public static int getDefaultThreshold() {
    return defaultThreshold;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.CALL_TREE;
  }

  @Override
  Object getValue() {
    return threshold;
  }

  @Override
  void setValue(Object newValue) {
    int newThreshold = ((Integer) newValue).intValue();
    threshold = newThreshold;
  }
}

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


package org.gprofng.mpmt.util.gui;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import javax.swing.JScrollBar;

/**
 * A scrollbar class that takes long value instead of ints. This class uses the swing JScrollBar but
 * handles the conversion of long values to pixels.
 */
public class AnLongScrollBar extends JScrollBar {
  private long longMin;
  private long longMax;
  private long longExtent;
  private long longMaxWithExtent;
  private long longValue;

  private int intMin;
  private int intMax;
  private int intExtent;
  private int intMaxWithExtent;
  private int oldIntVal;

  private long longUnitInc;
  private long longBlockInc;
  private int shift;

  private boolean setting_values = false;
  private ArrayList<LongAdjustmentListener> longListeners;

  private final int BLOCK_STEP_MAGIC = 2;
  private final int UNIT_STEP_MAGIC = 1;

  /** Creates a new instance of JLongScroll */
  public AnLongScrollBar(int orientation) {
    super(orientation);
    initialize();
  }

  /** initialize listeners */
  private void initialize() {
    addAdjustmentListener(
        new AdjustmentListener() {
          /** When an adjustment is made to the integer value, compute a new long value */
          public void adjustmentValueChanged(AdjustmentEvent e) {
            /* If adjustment initiated by this class ignore the event */
            if (setting_values) {
              return;
            }
            int val = e.getValue();
            boolean isAdjusting = e.getValueIsAdjusting();

            long value;
            LongAdjustmentEvent le;
            if (shift == 0) {
              value = val;
              le = new LongAdjustmentEvent(value, isAdjusting);
            } else {
              int delta = val - oldIntVal;
              int sign;
              if (delta > 0) {
                sign = 1;
              } else {
                delta = -delta;
                sign = -1;
              }
              if (delta == UNIT_STEP_MAGIC) { // magic for unit increment
                value = longValue + longUnitInc * sign;
              } else if (delta == BLOCK_STEP_MAGIC) { // magic for block increment
                value = longValue + longBlockInc * sign;
              } else { // interpret as a drag of the slider
                value = (long) val * (1L << shift);
              }
              setValueInternal(value);
              le = new LongAdjustmentEvent(longValue, isAdjusting);
            }
            fireAdjustementEvent(le);
          }
        });

    longListeners = new ArrayList();

    super.setValues(0, 0, 0, 0);
  }

  // override parent version
  public void setUnitIncrement(int inc) {
    setUnitIncrement((long) inc);
  }

  public void setUnitIncrement(long inc) {
    longUnitInc = inc;
    int val;
    if (shift == 0) {
      val = (int) inc;
    } else {
      val = UNIT_STEP_MAGIC; // supply magic value 1
    }
    super.setUnitIncrement(val);
  }

  // override parent version
  public void setBlockIncrement(int inc) {
    setBlockIncrement((long) inc);
  }

  public void setBlockIncrement(long inc) {
    longBlockInc = inc;
    int val;
    if (shift == 0) {
      val = (int) inc;
    } else {
      val = BLOCK_STEP_MAGIC; // supply magic longValue 2
    }
    super.setBlockIncrement(val);
  }

  public long getLongValue() {
    return longValue;
  }

  private long setValueInternal(long value) {
    // correct for overshoot
    if (value < longMin) {
      value = longMin;
    } else if (value > longMaxWithExtent) {
      value = longMaxWithExtent;
    }
    longValue = value;

    long tmpValue = value >> shift;
    int intValue = (int) tmpValue;
    if (intValue < intMin) {
      intValue = intMin;
    } else if (intValue > intMaxWithExtent) {
      intValue = intMaxWithExtent;
    }
    oldIntVal = intValue;

    setting_values = true;
    {
      setValue(intValue);
    }
    setting_values = false;
    return longValue;
  }

  /** Set the values for the scollbar. Also sets block and unit increment */
  private void setValuesInternal(long tmpValue, long tmpExtent, long tmpMin, long tmpMax) {
    setting_values = true;
    {
      longValue = tmpValue;
      longExtent = tmpExtent;
      longMin = tmpMin;
      longMax = tmpMax;
      longMaxWithExtent = longMax - longExtent;

      /* scale the long values to fit in int */
      int shiftcnt = 0;
      while (tmpMax > Integer.MAX_VALUE
          || tmpMin < Integer.MIN_VALUE
          || tmpExtent > Integer.MAX_VALUE) {
        tmpMin /= 2;
        tmpMax /= 2;
        tmpValue /= 2;
        tmpExtent /= 2;
        shiftcnt++;
      }
      this.shift = shiftcnt;

      oldIntVal = (int) tmpValue;
      intExtent = (int) tmpExtent;
      intMin = (int) tmpMin;
      intMax = (int) tmpMax;
      intMaxWithExtent = intMax - intExtent;
      super.setValues(oldIntVal, intExtent, intMin, intMax);
    }
    setting_values = false;
  }

  private void setDefaultIncrementsInternal() {
    long uinc = longExtent / 50;
    if (uinc < 1) {
      uinc = 1;
    }
    setUnitIncrement(uinc);
    long binc = longExtent * 3 / 4;
    if (binc < 1) {
      binc = 1;
    }
    setBlockIncrement(binc);
  }

  public void setValues( // override parent version
      int tmpValue, int tmpExtent, int tmpMin, int tmpMax) {
    setValues((long) tmpValue, (long) tmpExtent, (long) tmpMin, (long) tmpMax);
  }

  public void setValues(long tmpValue, long tmpExtent, long tmpMin, long tmpMax) {
    setValuesInternal(tmpValue, tmpExtent, tmpMin, tmpMax);
    setDefaultIncrementsInternal();
  }

  /** Notify listeners of adjustment */
  private void fireAdjustementEvent(LongAdjustmentEvent event) {
    for (LongAdjustmentListener listener : longListeners) {
      listener.adjustmentValueChanged(event);
    }
  }

  /** Add LongAdjustmentListener */
  public void addLongAdjustmentListener(LongAdjustmentListener listener) {
    longListeners.add(listener);
  }

  /** A listener for adjustements to the AnLongScrollBar */
  public interface LongAdjustmentListener {
    public void adjustmentValueChanged(LongAdjustmentEvent adjustmentEvent);
  }

  /** An event for adjestment to JLongScrollBars */
  public class LongAdjustmentEvent {
    private final long value;
    private final boolean isAdjusting;

    public LongAdjustmentEvent(long value, boolean isAdjusting) {
      this.value = value;
      this.isAdjusting = isAdjusting;
    }

    /** Get the new specified value */
    public long getValue() {
      return value;
    }

    public boolean getValueIsAdjusting() {
      return isAdjusting;
    }
  }
}

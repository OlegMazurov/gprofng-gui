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

package org.gprofng.mpmt.util.ruler.valuetypes;

/**
 * The ValuesGeneric interface allows for specification of how to format a value to display it and
 * what unit string to use.
 */
public interface ValuesGeneric {

  /** Get the units (e.g. "usec") */
  public String getUnitString();

  /** get <value> as a string (does not include units) */
  public String getFormattedValue(long value);

  /** get <value> as a string, with enough precision to show <range> */
  public String getFormattedValue(long value, long range);

  /** Get the low value for the range */
  public long getLowValue();

  /** Get the high value for the range */
  public long getHighValue();

  /** Set the data range */
  public void setRange(long v1, long v2);
}

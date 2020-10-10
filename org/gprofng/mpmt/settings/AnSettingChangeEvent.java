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

import java.util.EventObject;

public class AnSettingChangeEvent extends EventObject {
  public enum Type {
    VIEW_MODE, // fired when there is a change to "View Mode" setting
    VIEW_MODE_ENABLED, // fired when there is a change to "View Mode" visibility
    FORMAT, // fired when there are a changes to "Function Name Style" or "Append SO name to
            // Function name" settings
    COMPARE_MODE, // fired when there is a change to "Compare Mode" setting
    METRICS, // fired when there is a change to "Metrics" (visibility) settings
    TIMELINE, // fired when there is a change to "Timeline" settings
    SRC_DIS, // fired when there is a change to "Source/Disassembly" settings
    SEARCH_PATH, // fired when there is a change to "Search Path" settings
    PATH_MAP, // fired when there is a change to "Path Map" settings
    CALL_TREE, // fired when there is a change to "Call Tree" settings
    VIEWS, // fired when there is a change to "Views" settings
    LIBRARY_VISIBILITY, // fired when there is a change to "Library Visibility" settings
    TABLE_FORMATS, // fired when there is a change to Table Formats
  }

  private final Object originalSource;
  private final Type type;
  private final Object oldValue;
  private final Object newValue;

  public AnSettingChangeEvent(
      Object originalSource, Object source, Type type, Object oldValue, Object newValue) {
    super(source);
    this.type = type;
    this.originalSource = originalSource;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * @return the originalSource
   */
  public Object getOriginalSource() {
    return originalSource;
  }

  /**
   * @return the type
   */
  public Type getType() {
    return type;
  }

  /**
   * @return the oldValue
   */
  public Object getOldValue() {
    return oldValue;
  }

  /**
   * @return the newValue
   */
  public Object getNewValue() {
    return newValue;
  }
}

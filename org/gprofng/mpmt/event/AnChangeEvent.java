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


package org.gprofng.mpmt.event;

import java.util.EventObject;

/**
 * @author tpreisle
 */
public class AnChangeEvent extends EventObject {
  public enum Type {
    MOST_RECENT_EXPERIMENT_LIST_CHANGED, // fired when there is a change to the most recent
                                         // experiments list

    REMOTE_CONNECTION_CHANGING, // fired when remote connection is about to change
    REMOTE_CONNECTION_CANCELLED_OR_FAILED, // fired if remove connection cancelled or failed
    REMOTE_CONNECTION_CHANGED, // fired when remote connection successfully connected

    FILTER_CHANGING, // fired when filters are about to change
    FILTER_CHANGED, // fired when filters have changed

    SOURCE_FINDING_CHANGING, // fired when find source are changing (archive, er_print pathmap, ...)
    SOURCE_FINDING_CHANGED, // fired when find source are changing (archive, er_print pathmap, ...)

    EXPERIMENTS_LOADING_NEW, // fired when new experiment(s) are about to be loaded from scratch
    EXPERIMENTS_LOADING_ADDED_OR_REMOVED, // fired when new experiment(s) are about to be added or
                                          // removed (compare, aggregate, ...)
    EXPERIMENTS_LOADED_FAILED, // fired when new experiment(s) failed to load
    EXPERIMENTS_LOADED, // fired when new experiment(s) loaded successfully

    SETTING_CHANGING, // fired when a setting is abot to change.
    SETTING_CHANGED, // fired when a setting is changed.

    SELECTED_OBJECT_CHANGED, // fired when selected object has changed from outside a main view
                             // (CalledByCalls)

    SELECTION_UPDATE, // fired when selection should be changed. Handler of this event will fire a
                      // SELECTION_CHANGING/SELECTION_CHANGED event
    SELECTION_CHANGING, // fired when selection is about to be changed.
    SELECTION_CHANGED, // fired when selection has changed. One handler of this event (Summary view)
                       // will update Selection Details panel

    DEBUG, // for debugging
    TEST1, // for testing
    TEST2, // for testing
  }

  private final Type type;

  public AnChangeEvent(Object source, Type type) {
    super(source);
    this.type = type;
  }

  /**
   * @return the event type
   */
  public Type getType() {
    return type;
  }
}

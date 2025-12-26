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


package org.gprofng.mpmt.picklist;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.EventListenerList;

public class PickList {
  /** Default max size of Picklist */
  private int maxSize = 8;
  /** Increase size of Picklist, if variable sized */
  private static int increaseSize = 8;
  /** The list itself and it's current size */
  private PickListElement[] picklist = null;

  private int picklistSize = 0;
  /** Holds list of event listeners */
  protected EventListenerList listenerList = null;

  /** Creates a new variable sized Picklist */
  public PickList() {
    initialize(0);
  }

  /** Creates a new Picklist with maximux 'max' elements. 'max' <= 0 means variable sized list. */
  public PickList(int max) {
    initialize(max);
  }

  private EventListenerList getListenerList() {
    if (listenerList == null) {
      listenerList = new EventListenerList();
    }
    return listenerList;
  }

  /** Initializing data */
  private void initialize(int max) {
    if (max <= 0) {
      maxSize = 0; // Open-ended ...
      picklist = new PickListElement[increaseSize];
    } else {
      maxSize = max; // Fixed sized
      picklist = new PickListElement[increaseSize];
    }
  }

  /**
   * Adds the specified component to the beginning of this list, increasing its size by one.
   *
   * @param elem the component to be added.
   */
  public void addElement(PickListElement elem) {
    addElement(elem, true);
    // notify listener of a change
    fireContentsChanged(this);
  }

  public void addElements(List<PickListElement> list) {
    for (PickListElement elem : list) {
      addElement(elem, true);
    }
    // notify listener of a change
    fireContentsChanged(this);
  }

  /**
   * Adds the specified component to the beginning of this list, increasing its size by one.
   *
   * @param elem the component to be added.
   */
  private synchronized void addElement(PickListElement elem, boolean check) {
    // Is this the only method that needs to synchronized??? FIXUP
    // first check if element already in the list
    int foundAt = contains(elem);
    int shiftDownFrom = 0;

    if (check && foundAt >= 0) {
      shiftDownFrom = foundAt;
      picklistSize--;
    } else {
      // Open-ended list
      if (picklistSize == picklist.length) {
        // Need to increate the size of picklist
        PickListElement[] picklist2 = new PickListElement[picklist.length + increaseSize];
        for (int i = 0; i < picklist.length; i++) {
          picklist2[i] = picklist[i];
        }
        picklist = picklist2;
      }
      shiftDownFrom = picklistSize;
    }

    // Shift down elements
    for (int i = shiftDownFrom; i > 0; i--) {
      picklist[i] = picklist[i - 1];
    }

    // insert at top and increase size
    picklist[0] = elem;
    picklistSize++;

    // if fixed-sizes list, check size and possile delete last element
    if (maxSize > 0 && picklistSize >= maxSize) {
      picklistSize--;
    }
  }

  /**
   * Tests if the specified element is a component in this list.
   *
   * @param elem an object.
   * @return index of the specified element in the list if it exists as determined by the
   *     <tt>equals</tt> method; <code>-1</code> otherwise.
   */
  public int contains(PickListElement elem) {
    int foundAt = -1;

    if (elem != null) {
      for (int i = 0; i < picklistSize; i++) {
        if (picklist[i].equals(elem)) {
          foundAt = i;
          break;
        }
      }
    }
    return foundAt;
  }

  /**
   * Returns the value at the specified index.
   *
   * @param index the requested index
   * @return the value at <code>index</code>
   */
  public PickListElement getElementAt(int index) {
    if (index >= 0 && index < picklistSize) {
      return picklist[index];
    } else {
      return null;
    }
  }

  public List<PickListElement> getElements() {
    List<PickListElement> ret = new ArrayList<>();
    for (PickListElement picklistElement : picklist) {
      if (picklistElement != null) {
        ret.add(picklistElement);
      }
    }
    return ret;
  }

  /**
   * Returns the max length of the list, 0 being variable sized.
   *
   * @return the max length of the list, 0 being variable sized
   */
  public int getMaxSize() {
    return maxSize;
  }

  /**
   * Returns the top-most element in the list if not empty, otherwise null.
   *
   * @return the top-most element in the list
   */
  public PickListElement getMostRecentUsedElement() {
    if (picklistSize > 0) {
      return picklist[0];
    } else {
      return null;
    }
  }

  /**
   * Returns the length of the list.
   *
   * @return the length of the list
   */
  public int getSize() {
    return picklistSize;
  }

  /** Removes all elements from this list and sets its size to zero. */
  public void removeAllElements() {
    for (int i = 0; i < picklist.length; i++) {
      picklist[i] = null;
    }
    picklistSize = 0;

    // notify listener of a change
    fireContentsChanged(this);
  }

  /**
   * Removes the first (lowest-indexed) occurrence of the argument from this list.
   *
   * @param elem the element to be removed.
   * @return the element removed if found, null otherwise.
   */
  public PickListElement removeElement(PickListElement elem) {
    return removeElementAt(contains(elem));
  }

  /**
   * Removes the element at the specified index from the list. Each element in this list with an
   * index greater or equal to the specified <code>index</code> is shifted downward to have an index
   * one smaller than the value it had previously. The size of this list is decreased by <tt>1</tt>.
   *
   * <p>The index must be a value greater than or equal to <code>0</code> and less than the current
   * size of the list.
   *
   * <p>
   *
   * @param index the index of the object to remove.
   * @return the element removed if index valid, null otherwise.
   */
  public PickListElement removeElementAt(int index) {
    PickListElement toBeRemoved = null;
    if (index < 0 || index >= picklistSize) {
      return toBeRemoved;
    }
    toBeRemoved = picklist[index];
    for (int i = index; i < picklistSize - 1; i++) {
      picklist[i] = picklist[i + 1];
    }
    picklistSize--;

    // notify listener of a change
    fireContentsChanged(this);

    return toBeRemoved;
  }

  /**
   * Replaces the element at the specified index with the one specified.
   *
   * @param index the index of the object to remove.
   * @return the element replaced if index valid, null otherwise.
   */
  public PickListElement replaceElementAt(PickListElement elem, int index) {
    PickListElement toBeReplaced = null;
    if (index < 0 || index >= picklistSize) {
      return toBeReplaced;
    }
    toBeReplaced = picklist[index];
    picklist[index] = elem;

    // notify listener of a change
    fireContentsChanged(this);

    return toBeReplaced;
  }

  /**
   * Adds a listener to the list that's notified each time a change to the data model occurs.
   *
   * @param l the <code>PicklistDataListener</code> to be added
   */
  public void addPicklistListener(PickListListener l) {
    getListenerList().add(PickListListener.class, l);
  }

  /**
   * Removes a listener from the list that's notified each time a change to the data model occurs.
   *
   * @param l the <code>PicklistDataListener</code> to be removed
   */
  public void removePicklistListener(PickListListener l) {
    getListenerList().remove(PickListListener.class, l);
  }

  private void fireContentsChanged(Object source) {
    Object[] listeners = getListenerList().getListenerList();
    PickListEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == PickListListener.class) {
        if (e == null) {
          e = new PickListEvent(source, PickListEvent.CONTENTS_CHANGED);
        }
        ((PickListListener) listeners[i + 1]).contentsChanged(e);
      }
    }
  }
}

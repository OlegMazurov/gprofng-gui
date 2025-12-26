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
package org.gprofng.mpmt;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.util.gui.AnJPanel;
import java.awt.*;
import javax.swing.*;

public final class AnList extends AnJPanel {

  private final boolean resizable;
  private final JPanel label;
  private final JPanel value;

  // Constructor
  public AnList(final boolean resizable) {
    this.resizable = resizable;

    label = new AnJPanel();
    label.setLayout(new BoxLayout(label, BoxLayout.Y_AXIS));
    value = new AnJPanel();
    value.setLayout(new BoxLayout(value, BoxLayout.Y_AXIS));

    if (resizable) {
      BorderLayout borderLayout = new BorderLayout();
      if (AnEnvironment.isLFNimbus()) {
        borderLayout.setHgap(4);
      }
      setLayout(borderLayout);

      add(label, BorderLayout.WEST);
      add(value, BorderLayout.CENTER);
    } else {
      add(label);
      add(value);
    }
  }

  // Add label
  public JComponent addLabel(final JComponent item) {
    return (JComponent) label.add((item != null) ? item : new AnJPanel());
  }

  // Add value
  private JComponent addValue(final JComponent item) {
    return (JComponent) value.add((item != null) ? item : new AnJPanel());
  }

  // Add label and value
  public void add(final JComponent litem, final JComponent vitem) {
    setAlignmentY(addLabel(litem), addValue(vitem));
  }

  // Align labels and values vertically
  private void setAlignmentY(final JComponent litem, final JComponent vitem) {
    final Dimension lsize;
    final Dimension vsize;
    Dimension msize;

    lsize = new Dimension(litem.getPreferredSize());
    vsize = new Dimension(vitem.getPreferredSize());

    if (lsize.height < vsize.height) {
      lsize.height = vsize.height;
    } else {
      vsize.height = lsize.height;
    }

    // Set labels size
    if (resizable) {
      msize = litem.getMaximumSize();
      msize.height = lsize.height;
      litem.setMaximumSize(msize);
    } else {
      litem.setMaximumSize(new Dimension(lsize));
    }

    litem.setMinimumSize(new Dimension(lsize));
    litem.setPreferredSize(new Dimension(lsize));

    // Set values size
    if (resizable) {
      msize = vitem.getMaximumSize();
      msize.height = vsize.height;
      vitem.setMaximumSize(msize);
    } else {
      vitem.setMaximumSize(new Dimension(vsize));
    }

    vitem.setMinimumSize(new Dimension(vsize));
    vitem.setPreferredSize(new Dimension(vsize));
  }

  // Align Labels and Values vertically
  public void setAlignmentY() {
    final Component[] labels = label.getComponents();
    final Component[] values = value.getComponents();
    final int size = Math.min(labels.length, values.length);

    for (int i = 0; i < size; i++) {
      setAlignmentY((JComponent) labels[i], (JComponent) values[i]);
    }
  }

  // Align items horizontally
  private void setAlignmentX(final Component[] items) {
    final int size;
    int width;
    int max_width;
    Dimension psize;

    size = items.length;
    max_width = 0;
    for (int i = 0; i < size; i++) {
      width = items[i].getPreferredSize().width;
      if (max_width < width) {
        max_width = width;
      }
    }

    for (int i = 0; i < size; i++) {
      psize = items[i].getPreferredSize();
      psize.width = max_width;
      ((JComponent) items[i]).setPreferredSize(psize);
    }
  }

  // Align the Values only horizontally, usually the Labels does not need
  // further alignment
  public void setAlignmentX() {
    setAlignmentX(value.getComponents());
  }

  // Clean up
  public void removeAll() {
    label.removeAll();
    value.removeAll();
  }
}

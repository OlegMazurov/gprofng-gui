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

package org.gprofng.mpmt.table;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class AnJTable extends JTable {

  public AnJTable(TableModel dm, TableColumnModel cm) {
    super(dm, cm);
    init();
  }

  private void init() {
    setupSelectionDragHack();
    AnUtility.setAccessibleContext(getAccessibleContext(), AnLocale.getString("Table"));
  }

  private final class AnJTableMouseAdapter extends MouseAdapter {

    private final MouseListener[] list;
    private final AnJTable table;

    public AnJTableMouseAdapter(final MouseListener[] ls, AnJTable t) {
      list = ls;
      table = t;
    }

    private void editCell(final MouseEvent e) {
      if (!JTextPane.class.isAssignableFrom(e.getSource().getClass())) {
        return;
      }
      table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());
    }

    private void stopCellEditing(final MouseEvent e) {
      if (!JTextPane.class.isAssignableFrom(e.getSource().getClass())) {
        return;
      }
      if (table.getCellEditor() != null) {
        table.getCellEditor().stopCellEditing();
      }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
      for (final MouseListener l : list) {
        l.mousePressed(e);
      }
      if (table.getCellEditor() != null) {
        table.getCellEditor().stopCellEditing();
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      for (final MouseListener l : list) {
        l.mouseClicked(e);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      for (final MouseListener l : list) {
        l.mouseReleased(e);
      }
      // xxxx may be enabled if we want some interaction after the cell is selected.
      // table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());

      table.requestFocusInWindow();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      stopCellEditing(e);
      for (final MouseListener l : list) {
        l.mouseEntered(e);
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      stopCellEditing(e);
      for (final MouseListener l : list) {
        l.mouseEntered(e);
      }
    }
  }

  private void setupSelectionDragHack() {
    // Bracket the other mouse listeners so we may inject our lie
    final MouseListener[] ls = getMouseListeners();
    for (final MouseListener l : ls) {
      removeMouseListener(l);
    }

    this.addMouseListener(new AnJTableMouseAdapter(ls, this));
    // this.addMouseMotionListener(new AnJTableMouseMotionAdapter(mls, this));
  }
}

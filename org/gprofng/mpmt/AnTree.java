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


package org.gprofng.mpmt;

import org.gprofng.mpmt.util.gui.AnJTree;
import org.gprofng.mpmt.util.gui.AnTextIcon;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public final class AnTree extends AnJTree {

  private final DefaultMutableTreeNode root;
  private final DefaultTreeModel model;
  private final int expand_row;
  private final AnTreeRenderer renderer;

  // Constructor
  public AnTree(final String name, final int row) {
    super();

    root = new DefaultMutableTreeNode(name, true);
    model = new DefaultTreeModel(root);
    expand_row = row;

    model.setAsksAllowsChildren(true);

    setModel(model);
    putClientProperty("JTree.lineStyle", "Angled");
    setShowsRootHandles(true);
    setEditable(true);

    setCellRenderer(renderer = new AnTreeRenderer());
    setCellEditor(new AnTreeEditor(this, renderer));
  }

  // Add one node into the root
  private void addNode(final AnTextIcon item) {
    final DefaultMutableTreeNode node;

    node = new DefaultMutableTreeNode(item, true);
    node.add(new DefaultMutableTreeNode(null, false));
    root.add(node);
  }

  // Add the extra node; the first child node
  public int addExtra(final String extra) {
    int last;

    // Remove all nodes when restart
    getCellEditor().cancelCellEditing();
    root.removeAllChildren();
    last = 0;

    if (last == 0) {
      addNode(new AnTextIcon(extra, null));
      model.nodeStructureChanged(root);
    } else {
      last--;
    }
    return last;
  }

  // Add nodes
  public void addNodes(final AnTextIcon[] list, final int last) {
    final int size;
    int iv;
    final int[] indices;

    size = list.length;
    indices = new int[size - last];
    iv = 0;
    for (int i = last; i < size; i++) {
      indices[iv++] = i + 1;
      addNode(list[i]);
    }

    model.nodesWereInserted(root, indices);

    // Initially expand the first child node
    if (last == 0) {
      expandRow(expand_row);
    }
  }

  // Remove nodes
  public void removeNodes(final int[] remove_index) {
    final int nd = remove_index.length;
    final DefaultMutableTreeNode node;
    final int[] indices;
    final Object[] rm_node;

    getCellEditor().cancelCellEditing();

    // If remove all, clean up but keep the extra node
    if (nd >= root.getChildCount() - 1) {
      node = (DefaultMutableTreeNode) root.getChildAt(0);
      addExtra(((AnTextIcon) node.getUserObject()).getText());
      return;
    }

    indices = new int[nd];
    rm_node = new Object[nd];
    for (int i = nd - 1; i >= 0; i--) {
      indices[i] = remove_index[i] + 1;
      rm_node[i] = root.getChildAt(indices[i]);
      root.remove(indices[i]);
    }

    model.nodesWereRemoved(root, indices, rm_node);
  }

  // Remove all nodes
  public void removeAllNodes() {
    if (root.getChildCount() != 0) {
      getCellEditor().cancelCellEditing();
      root.removeAllChildren();
      model.nodeStructureChanged(root);
    }
  }

  // Set object of the content node
  public void setContent(final int index, final Object object) {
    final DefaultMutableTreeNode node, content;

    node = (DefaultMutableTreeNode) root.getChildAt(index);
    content = (DefaultMutableTreeNode) node.getChildAt(0);
    content.setUserObject(object);
    model.nodeChanged(content);
  }

  // Tree cell renderer
  private static final class AnTreeRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(
        final JTree tree,
        Object value,
        final boolean sel,
        final boolean expanded,
        final boolean leaf,
        final int row,
        final boolean hasFocus) {
      final Object object;
      final AnTextIcon item;
      Icon icon = null;

      setLeafIcon(null);
      setClosedIcon(null);
      setOpenIcon(null);

      if (value instanceof DefaultMutableTreeNode) {
        object = ((DefaultMutableTreeNode) value).getUserObject();

        if (object instanceof AnTextIcon) {
          item = (AnTextIcon) object;
          value = item.getText();
          icon = item.getIcon();
        } else if (object instanceof JComponent) {
          ((JComponent) object)
              .setBorder(hasFocus ? AnVariable.treeFocusBorder : AnVariable.noFocusBorder);
          return (Component) object;
        }
      }

      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

      if (icon != null) {
        setIcon(icon);
      }
      return this;
    }
  }

  // Tree cell editor
  private final class AnTreeEditor extends DefaultTreeCellEditor {

    public AnTreeEditor(final JTree tree, final DefaultTreeCellRenderer renderer) {
      super(tree, renderer);
    }

    public Component getTreeCellEditorComponent(
        final JTree tree,
        final Object value,
        final boolean isSelected,
        final boolean expanded,
        final boolean leaf,
        final int row) {

      editingComponent =
          this.renderer.getTreeCellRendererComponent(
              tree, value, isSelected, expanded, leaf, row, true);
      return editingComponent;
    }

    public boolean isCellEditable(final EventObject evt) {
      if (evt instanceof MouseEvent) {
        final MouseEvent mEvt = (MouseEvent) evt;
        if (mEvt.getClickCount() == 1) {
          final TreePath selPath = tree.getPathForLocation(mEvt.getX(), mEvt.getY());
          if (selPath == null) {
            return false;
          }
          final DefaultMutableTreeNode node =
              (DefaultMutableTreeNode) selPath.getLastPathComponent();
          if (node.getUserObject() instanceof ExperimentsView.AnTreePanel) {
            return true;
          }
        }
      }
      return super.isCellEditable(evt);
    }
  }
}

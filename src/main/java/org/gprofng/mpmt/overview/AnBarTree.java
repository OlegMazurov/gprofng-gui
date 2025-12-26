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


package org.gprofng.mpmt.overview;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.metrics.MetricNode;
import org.gprofng.mpmt.metrics.MetricsGUI;
import org.gprofng.mpmt.metrics.SelectableMetricNode;
import org.gprofng.mpmt.overview.Bar.ValueColor;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractCellEditor;
import javax.swing.JTree;
import javax.swing.JTree.DynamicUtilTreeNode;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;

public class AnBarTree extends JTree {
  private static int CHECKBOX_WIDTH = 20; // FIXUP: can this be calculated?
  private CheckBoxBarPanel[] checkBoxBarPanels = new CheckBoxBarPanel[20];

  public AnBarTree(Vector<Object> rootVector) {
    super(rootVector);
    AnUtility.setAccessibleContext(getAccessibleContext(), AnLocale.getString("Metrics Tree"));
    TreeCellRenderer anCellRenderer = new CellRenderer();
    setCellRenderer(anCellRenderer);
    CellEditor anCellEditor = new CellEditor(this);
    setCellEditor(anCellEditor);
    setEditable(true);
    setOpaque(false);
    //        setRootVisible(true);
    ToolTipManager.sharedInstance().registerComponent(this);

    addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
              for (CheckBoxBarPanel cbp : checkBoxBarPanels) {
                if (cbp != null) {
                  if (cbp.isHasFocus()) {
                    cbp.getCheckBox().doClick();
                    break;
                  }
                }
              }
            }
          }
        });
  }

  public void setTreeNodeStateSelected(
      SelectableMetricNode selectableMetricNode, boolean selected) {
    CellEditor cellEditor = (CellEditor) getCellEditor();
    cellEditor.setTreeNodeStateSelected(selectableMetricNode, selected);
  }

  public static class BarNode<E> extends Vector<E> {
    public MetricNode metricNode;
    private boolean selected;
    public String toolTipText;
    public Bar bar;
    public int maxLength;
    public int level;
    private CheckBoxBarPanel checkBoxBarPanel = null;

    public BarNode(
        MetricNode metricNode,
        boolean isSelected,
        String toolTipText,
        Bar bar,
        List<E> elements,
        int maxLength,
        int level) {
      this.metricNode = metricNode;
      this.selected = isSelected;
      this.toolTipText = toolTipText;
      this.bar = bar;
      if (elements != null) {
        for (int i = 0, n = elements.size(); i < n; i++) {
          add(elements.get(i));
        }
      }
      this.maxLength = maxLength;
      this.level = level;

      if (metricNode instanceof SelectableMetricNode) {
        ((SelectableMetricNode) metricNode)
            .addChangeListener(
                new ChangeListener() {
                  //                    @Override
                  @Override
                  public void stateChanged(ChangeEvent e) {
                    Object o = e.getSource();
                    if (o instanceof SelectableMetricNode) {
                      selected = ((SelectableMetricNode) o).hasSelections();
                      if (checkBoxBarPanel
                          != null /*&& checkBoxBarPanel.isSelected() != selected*/) {
                        checkBoxBarPanel.setSelected(selected);
                        MetricsGUI.getInstance().refreshMetricsPanel();
                      }
                    }
                  }
                });
      }
    }

    public BarNode(
        MetricNode metricNode,
        boolean isSelected,
        String toolTipText,
        Bar bar,
        int maxLength,
        int level) {
      this(metricNode, isSelected, toolTipText, bar, null, maxLength, level);
    }

    /**
     * @return the bar
     */
    public Bar getBar() {
      return bar;
    }

    public String getName() {
      return metricNode.getMetricBasic().getName();
    }

    public String getDisplayName() {
      return metricNode.getMetricBasic().getDisplayName();
    }

    /**
     * @return the selected
     */
    public boolean isSelected() {
      return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
      this.selected = selected;
    }

    /**
     * @return the barCheckBox
     */
    public CheckBoxBarPanel getCheckBoxBarPanel() {
      return checkBoxBarPanel;
    }

    /**
     * @param barCheckBox the barCheckBox to set
     */
    public void setCheckBoxBarPanel(CheckBoxBarPanel checkBoxBarPanel) {
      this.checkBoxBarPanel = checkBoxBarPanel;
    }
  }

  class CellRenderer implements TreeCellRenderer {
    DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    Color backgroundSelectionColor = defaultRenderer.getBackgroundSelectionColor();
    Color backgroundNonSelectionColor = defaultRenderer.getBackgroundNonSelectionColor();

    public CellRenderer() {}

    @Override
    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus) {
      CheckBoxBarPanel checkBoxBarPanel = null;
      boolean isSelected = false;
      String text = null;
      String toolTipText = null;
      Bar bar = null;
      int maxLength = 100;
      boolean hot = false;

      if ((value != null) && (value instanceof DynamicUtilTreeNode)) {
        Object userObject = ((DynamicUtilTreeNode) value).getUserObject();
        if (userObject instanceof BarNode) {
          BarNode node = (BarNode) userObject;
          isSelected = node.isSelected();
          text = node.getDisplayName();
          toolTipText = node.toolTipText;
          bar = node.bar;
          maxLength = node.maxLength;
          int level = node.level;
          maxLength = maxLength - (level - 1) * CHECKBOX_WIDTH;
          hot = node.metricNode.isHot();
          checkBoxBarPanel = node.getCheckBoxBarPanel();
          if (checkBoxBarPanel == null) {
            List<Color> keyColorList = null;
            if (bar != null && bar.getValueColor() != null && bar.getValueColor().size() <= 3) {
              keyColorList = new ArrayList<>();
              for (ValueColor valueColor : bar.getValueColor()) {
                keyColorList.add(valueColor.getColor());
              }
            }
            checkBoxBarPanel =
                new CheckBoxBarPanel(
                    node.metricNode,
                    isSelected,
                    hot,
                    text,
                    toolTipText,
                    bar,
                    maxLength,
                    keyColorList,
                    level,
                    -1); // <===
            checkBoxBarPanel.setInsets(new Insets(2, 0, 2, 0));
            node.setCheckBoxBarPanel(checkBoxBarPanel);
          } else {
            checkBoxBarPanel.setSelected(isSelected);
          }
        }
      }
      if (checkBoxBarPanel == null) {
        checkBoxBarPanel =
            new CheckBoxBarPanel(
                null, isSelected, hot, text, toolTipText, bar, maxLength, null, 0, -1);
      }

      for (CheckBoxBarPanel cbp : checkBoxBarPanels) {
        if (cbp != null) {
          cbp.setHasFocus(false);
        }
      }
      if (hasFocus) {
        checkBoxBarPanel.setHasFocus(true);
      }
      if (row >= 0 && row < checkBoxBarPanels.length) {
        checkBoxBarPanels[row] = checkBoxBarPanel;
      }

      return checkBoxBarPanel;
    }
  }

  class CellEditor extends AbstractCellEditor implements TreeCellEditor {
    ChangeEvent changeEvent = null;
    JTree tree;
    ActionListener actionListener = null;

    public CellEditor(JTree tree) {
      this.tree = tree;
    }

    @Override
    public Object getCellEditorValue() {
      return null; // FIXUP: ??????
    }

    @Override
    public boolean isCellEditable(EventObject event) {
      return true;
    }

    @Override
    public Component getTreeCellEditorComponent(
        final JTree tree,
        final Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row) {
      Component editor =
          tree.getCellRenderer()
              .getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
      final CheckBoxBarPanel checkBoxBarPanel = (CheckBoxBarPanel) editor;
      ActionListener[] al = checkBoxBarPanel.getCheckBox().getActionListeners();
      if (al.length
          == 1) { // FIXUP: Hack: we only want to add this listener ONCE and we get here many
                  // times!!!!!!!!!
        checkBoxBarPanel
            .getCheckBox()
            .addActionListener(
                new ActionListener() {
                  // @Override
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    checkBoxClicked(tree, checkBoxBarPanel, value);
                  }
                });
      }

      return editor;
    }

    private List<TreeNode> findNodesInTreeByName(String name) {
      List<TreeNode> list = new ArrayList<>();
      Object root = getModel().getRoot();
      TreeNode rootNode = (TreeNode) root;
      findNodesInTreeByName(rootNode, list, name);
      return list;
    }

    private void findNodesInTreeByName(TreeNode treeNode, List<TreeNode> list, String name) {
      if (treeNode instanceof DynamicUtilTreeNode) {
        DynamicUtilTreeNode dynamicTreeNode = (DynamicUtilTreeNode) treeNode;
        Object uo = dynamicTreeNode.getUserObject();
        if (uo instanceof BarNode) {
          BarNode bn = (BarNode) uo;
          String bnName = bn.metricNode.getMetricBasic().getName();
          if (bnName.equals(name)) {
            list.add(treeNode);
          }
        }
      }
      for (int i = 0; i < treeNode.getChildCount(); i++) {
        findNodesInTreeByName(treeNode.getChildAt(i), list, name);
      }
    }

    /** Checkbox clicked.... */
    private void checkBoxClicked(JTree tree, CheckBoxBarPanel barPanel, final Object value) {
      if ((value != null) && (value instanceof DynamicUtilTreeNode)) {
        DynamicUtilTreeNode dynamicUtilTreeNode = (DynamicUtilTreeNode) value;
        Object userObject = dynamicUtilTreeNode.getUserObject();
        if (userObject instanceof BarNode) {
          BarNode barNode = (BarNode) userObject;
          MetricNode metricNode = barNode.metricNode;
          if (metricNode instanceof SelectableMetricNode) {
            SelectableMetricNode selectableMetricNode = (SelectableMetricNode) metricNode;
            //                        selectableMetricNode.setSelected(barPanel.isSelected(),
            // false);
            barNode.setSelected(barPanel.isSelected()); // ???

            // synchronize selection with outher node representing same metric (same metric name)
            List<TreeNode> nodeList =
                findNodesInTreeByName(selectableMetricNode.getMetricBasic().getName());
            for (int i = 0; i < nodeList.size(); i++) {
              TreeNode treeNode = nodeList.get(i);
              if (treeNode instanceof DynamicUtilTreeNode) {
                DynamicUtilTreeNode child = (DynamicUtilTreeNode) treeNode;
                Object uo = child.getUserObject();
                if (uo instanceof BarNode) {
                  BarNode bn = (BarNode) uo;
                  if (bn != barNode) {
                    bn.setSelected(barPanel.isSelected());
                  }
                }
              }
            }
          }
        }
      }
    }

    /** Set internal state in barnode to selected */
    protected void setTreeNodeStateSelected(
        SelectableMetricNode selectableMetricNode, boolean selected) {
      List<TreeNode> nodeList =
          findNodesInTreeByName(selectableMetricNode.getMetricBasic().getName());
      for (int i = 0; i < nodeList.size(); i++) {
        TreeNode treeNode = nodeList.get(i);
        if (treeNode instanceof DynamicUtilTreeNode) {
          DynamicUtilTreeNode child = (DynamicUtilTreeNode) treeNode;
          Object uo = child.getUserObject();
          if (uo instanceof BarNode) {
            BarNode bn = (BarNode) uo;
            bn.setSelected(selected);
          }
        }
      }
    }
  }
}

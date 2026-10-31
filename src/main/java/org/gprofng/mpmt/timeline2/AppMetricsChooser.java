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


package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.experiment_props.ExperimentProperties;
import org.gprofng.mpmt.util.gui.AnDialog2;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * Chooser of the application metrics to show in the Timeline: a tree of the metric names (split at
 * ':') with check boxes and a filter on names and descriptions. The order in which metrics are
 * checked is the order of the rows.
 */
final class AppMetricsChooser extends JPanel {

  /** Leaf of the tree: one metric. */
  private static final class Metric {
    final String name;
    final String label;
    final String description;

    Metric(String name, String label, String description) {
      this.name = name;
      this.label = label;
      this.description = description;
    }
  }

  private final AppMetricsModel model;
  private final List<Integer> expIds;
  private final Map<Integer, Set<String>> working = new HashMap<>(); // edited selections
  private final Map<Integer, AppMetricsModel.Layout> workingLayouts = new HashMap<>();
  // checked: all metrics share one row, else each has its own
  private final JCheckBox sameRow = new JCheckBox("All metrics in one row");
  // checked: stacked, else drawn over each other; only available with "one row"
  private final JCheckBox stacked = new JCheckBox("Stacked");
  private final JComboBox<AppMetricsModel.ScaleMode> scaleCombo =
      new JComboBox<>(AppMetricsModel.ScaleMode.values());
  private final JComboBox<String> experimentCombo = new JComboBox<>();
  private final JTextField filterField = new JTextField();
  private final JLabel countLabel = new JLabel();
  private final JTree tree =
      new JTree(new DefaultTreeModel(new DefaultMutableTreeNode())) {
        @Override
        public String getToolTipText(MouseEvent e) { // full name and description of a metric
          final TreePath path = getPathForLocation(e.getX(), e.getY());
          if (path != null) {
            final Object o = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (o instanceof Metric) {
              final Metric m = (Metric) o;
              return "<html>" + escape(m.name) + "<br>" + escape(m.description) + "</html>";
            }
          }
          return null;
        }
      };
  private int current; // index into expIds

  private AppMetricsChooser(
      final AppMetricsModel model, final List<Integer> expIds, final List<String> expNames) {
    super(new BorderLayout(0, 6));
    this.model = model;
    this.expIds = expIds;
    for (int id : expIds) {
      working.put(id, new LinkedHashSet<>(model.selection(id)));
      workingLayouts.put(id, model.layout(id));
    }

    final JPanel top = new JPanel(new BorderLayout(0, 4));
    if (expIds.size() > 1) {
      for (String n : expNames) {
        experimentCombo.addItem(n);
      }
      experimentCombo.addActionListener(
          e -> {
            current = experimentCombo.getSelectedIndex();
            showLayout();
            rebuildTree();
          });
      top.add(experimentCombo, BorderLayout.NORTH);
    }
    final JPanel filterRow = new JPanel(new BorderLayout(6, 0));
    filterRow.add(new JLabel("Filter:"), BorderLayout.WEST);
    filterRow.add(filterField, BorderLayout.CENTER);
    top.add(filterRow, BorderLayout.SOUTH);
    add(top, BorderLayout.NORTH);

    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    tree.setCellRenderer(new Renderer());
    tree.setToggleClickCount(0);
    ToolTipManager.sharedInstance().registerComponent(tree);
    tree.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path != null) {
              toggle(path);
            }
          }
        });
    tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleMetric");
    tree.getActionMap()
        .put(
            "toggleMetric",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                final TreePath path = tree.getSelectionPath();
                if (path != null) {
                  toggle(path);
                }
              }
            });
    final JScrollPane scroll = new JScrollPane(tree);
    scroll.setPreferredSize(new Dimension(560, 420));
    add(scroll, BorderLayout.CENTER);

    final JPanel options = new JPanel(new java.awt.GridLayout(0, 1, 0, 2));
    final JPanel rowLine = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
    rowLine.add(sameRow);
    rowLine.add(stacked);
    options.add(rowLine);
    final JPanel scaleLine = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
    scaleLine.add(new JLabel("Scale:"));
    scaleLine.add(scaleCombo);
    options.add(scaleLine);
    scaleCombo.setRenderer(
        new javax.swing.DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              javax.swing.JList<?> list, Object value, int index, boolean sel, boolean focus) {
            return super.getListCellRendererComponent(
                list, ((AppMetricsModel.ScaleMode) value).label, index, sel, focus);
          }
        });
    showLayout();
    sameRow.addActionListener(e -> storeLayout());
    stacked.addActionListener(e -> storeLayout());
    scaleCombo.addActionListener(e -> storeLayout());

    final JPanel bottom = new JPanel(new BorderLayout());
    bottom.add(options, BorderLayout.NORTH);
    bottom.add(countLabel, BorderLayout.WEST);
    final JButton clear = new JButton("Clear");
    clear.addActionListener(
        e -> {
          selection().clear();
          tree.repaint();
          updateCount();
        });
    bottom.add(clear, BorderLayout.EAST);
    add(bottom, BorderLayout.SOUTH);

    filterField
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                rebuildTree();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                rebuildTree();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {}
            });
    rebuildTree();
  }

  /**
   * Lets the user choose the metrics; on OK stores the choice in the model and runs onChanged.
   *
   * <p>Must be called on the EDT, after the rows have been built (metrics are known then).
   */
  static void show(final AnWindow window, final AppMetricsModel model, final Runnable onChanged) {
    final List<Integer> expIds = model.expIdsWithMetrics();
    if (expIds.isEmpty()) {
      return;
    }
    final List<String> names = new ArrayList<>();
    for (int id : expIds) {
      String name = "Experiment " + id;
      for (ExperimentProperties props : window.getExperimentProperties().getAllExperimentProperties()) {
        if (props.getID() == id) {
          name = props.getName();
        }
      }
      names.add(name);
    }
    final AppMetricsChooser chooser = new AppMetricsChooser(model, expIds, names);
    final AnDialog2 dialog =
        new AnDialog2(window.getFrame(), window.getFrame(), "Application Metrics");
    dialog.setCustomPanel(chooser);
    dialog.setVisible(true);
    if (dialog.getStatus() == AnDialog2.Status.OK) {
      for (int id : expIds) {
        model.setSelection(id, new ArrayList<>(chooser.working.get(id)));
        model.setLayout(id, chooser.workingLayouts.get(id));
      }
      onChanged.run();
    }
  }

  private static String escape(final String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private void showLayout() { // controls <- working layout of the current experiment
    final AppMetricsModel.Layout layout = workingLayouts.get(expIds.get(current));
    sameRow.setSelected(layout.sameRow);
    stacked.setSelected(layout.stacked);
    scaleCombo.setSelectedItem(layout.scale);
    updateStackingEnabled();
  }

  private void storeLayout() { // working layout of the current experiment <- controls
    workingLayouts.put(
        expIds.get(current),
        new AppMetricsModel.Layout(
            sameRow.isSelected(),
            stacked.isSelected(),
            (AppMetricsModel.ScaleMode) scaleCombo.getSelectedItem()));
    updateStackingEnabled();
  }

  // stacking only means something when the metrics share a row
  private void updateStackingEnabled() {
    stacked.setEnabled(sameRow.isSelected());
  }

  private Set<String> selection() {
    return working.get(expIds.get(current));
  }

  private void toggle(final TreePath path) {
    final Object o = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
    if (o instanceof Metric) {
      final Set<String> sel = selection();
      final String name = ((Metric) o).name;
      if (!sel.remove(name)) {
        sel.add(name);
      }
      ((DefaultTreeModel) tree.getModel()).nodeChanged((DefaultMutableTreeNode) path.getLastPathComponent());
      updateCount();
    }
  }

  private void updateCount() {
    countLabel.setText(selection().size() + " selected");
  }

  private void rebuildTree() {
    final AppMetricTree metrics = model.cachedTree(expIds.get(current));
    final String filter = filterField.getText().trim().toLowerCase();
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    final Map<String, DefaultMutableTreeNode> groups = new HashMap<>();
    if (metrics != null) {
      for (int i = 0; i < metrics.size(); i++) {
        final String name = metrics.names[i];
        final String description = metrics.descriptions[i];
        if (!filter.isEmpty()
            && !name.toLowerCase().contains(filter)
            && !description.toLowerCase().contains(filter)) {
          continue;
        }
        final List<String> parts = new ArrayList<>();
        for (String part : name.split(":")) {
          if (!part.isEmpty()) {
            parts.add(part);
          }
        }
        if (parts.isEmpty()) {
          parts.add(name);
        }
        DefaultMutableTreeNode parent = root;
        String key = "";
        for (int k = 0; k < parts.size() - 1; k++) {
          key += parts.get(k) + ":";
          DefaultMutableTreeNode group = groups.get(key);
          if (group == null) {
            group = new DefaultMutableTreeNode(parts.get(k));
            groups.put(key, group);
            parent.add(group);
          }
          parent = group;
        }
        parent.add(
            new DefaultMutableTreeNode(new Metric(name, parts.get(parts.size() - 1), description)));
      }
    }
    tree.setModel(new DefaultTreeModel(root));
    if (!filter.isEmpty()) {
      for (int row = 0; row < tree.getRowCount(); row++) {
        tree.expandRow(row); // rowCount grows as groups expand
      }
    }
    updateCount();
  }

  private final class Renderer implements TreeCellRenderer {
    private final JCheckBox check = new JCheckBox();
    private final JLabel group = new JLabel();

    @Override
    public Component getTreeCellRendererComponent(
        JTree t,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus) {
      final Object o = ((DefaultMutableTreeNode) value).getUserObject();
      if (o instanceof Metric) {
        final Metric m = (Metric) o;
        check.setText(m.label);
        check.setSelected(selection().contains(m.name));
        check.setOpaque(selected);
        check.setBackground(selected ? t.getBackground().darker() : t.getBackground());
        return check;
      }
      group.setText(String.valueOf(o));
      return group;
    }
  }
}

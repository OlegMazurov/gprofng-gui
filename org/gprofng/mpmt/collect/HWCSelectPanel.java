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

package org.gprofng.mpmt.collect;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.picklist.StringPickList;
import org.gprofng.mpmt.picklist.StringPickListElement;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class HWCSelectPanel extends JPanel {

  private HWCSelectDialog dialog;

  private List<HWCEntry> hwcFlatList; // All counters
  private List<HWCEntry> hwcFilteredList; // Filtered counters only
  private final Map<String, HWCEntry> hwcNameMap;
  private TableModel tableModel;
  private JTable table;

  private static StringPickList includePicklist = new StringPickList();
  private static StringPickList excludePicklist = new StringPickList();
  private ItemListener includeItemListener = null;
  private ItemListener excludeItemListener = null;

  private JMenuItem copySelectedItems;
  private JMenuItem copyAllItems;

  public HWCSelectPanel(
      HWCSelectDialog dialog, List<HWCEntry> hwcFlatList, Map<String, HWCEntry> hwcNameMap) {
    AnUtility.checkIfOnAWTThread(true);
    this.dialog = dialog;
    this.hwcFlatList = hwcFlatList;
    this.hwcNameMap = hwcNameMap;
    initComponents();

    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    AnUtility.setTextAndAccessibleContext(
        infoLabel,
        AnLocale.getString(
            "Select one or more HW counters from the list of available counters below."));

    // Filter panel
    filterPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    AnUtility.setTextAndAccessibleContext(filterLabel, " " + AnLocale.getString("Filters"));
    filterLabel.setLabelFor(includeComboBox);
    filterLabel.setOpaque(true);
    checkBoxPanel.setOpaque(false);

    recommendedIconLabel.setIcon(AnUtility.circleGreenIcon);
    recommendedCheckBox.setOpaque(false);
    recommendedCheckBox.setSelected(false);
    AnUtility.setTextAndAccessibleContext(
        recommendedCheckBox, AnLocale.getString("Recommended (R)"));
    recommendedCheckBox.setToolTipText(AnLocale.getString("Recommended hardware counters"));
    recommendedCheckBox.setMnemonic(AnLocale.getString('R', "HWCSelectPanel.recommendedCheckBox"));

    timeIconLabel.setIcon(AnUtility.circleRedIcon);
    timeCheckBox.setOpaque(false);
    timeCheckBox.setSelected(false);
    AnUtility.setTextAndAccessibleContext(timeCheckBox, AnLocale.getString("Time (T)"));
    timeCheckBox.setToolTipText(AnLocale.getString("Hardware counters supporting time profiling"));
    timeCheckBox.setMnemonic(AnLocale.getString('T', "HWCSelectPanel.timeCheckBox"));

    memoryIconLabel.setIcon(AnUtility.circleBlueIcon);
    memoryCheckBox.setOpaque(false);
    memoryCheckBox.setSelected(false);
    AnUtility.setTextAndAccessibleContext(memoryCheckBox, AnLocale.getString("Memoryspace (M)"));
    memoryCheckBox.setToolTipText(
        AnLocale.getString("Hardware counters supporting memory profiling"));
    memoryCheckBox.setMnemonic(AnLocale.getString('M', "HWCSelectPanel.memoryCheckBox"));

    String includeTT = AnLocale.getString("Include entries that contain any of these text strings");
    includeLabel.setText(AnLocale.getString("Include:"));
    includeLabel.setToolTipText(includeTT);
    includeLabel.setDisplayedMnemonic(
        AnLocale.getString('I', "HWCSelectPanel.IncludeLabelMnemonic"));
    includeLabel.setLabelFor(includeComboBox);
    includeComboBox.setToolTipText(includeTT);
    String excludeTT = AnLocale.getString("Exclude entries that contain any of these text strings");
    excludeLabel.setToolTipText(excludeTT);
    excludeLabel.setText(AnLocale.getString("Exclude:"));
    excludeLabel.setDisplayedMnemonic(
        AnLocale.getString('X', "HWCSelectPanel.ExcludeLabelMnemonic"));
    excludeLabel.setLabelFor(excludeComboBox);
    excludeComboBox.setToolTipText(excludeTT);
    refreshButton.setText(AnLocale.getString("Refresh"));
    refreshButton.setMnemonic(AnLocale.getString('f', "HWCSelectPanel.RefreshButton"));
    refreshButton.setToolTipText(AnLocale.getString("Apply filters"));
    includeComboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    excludeComboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

    // Label panel
    listLabelPanel.setOpaque(false);
    AnUtility.setTextAndAccessibleContext(listLabel, AnLocale.getString("Available Counters"));
    listLabel.setDisplayedMnemonic(AnLocale.getString('V', "HWCSelectPanel.listLabel"));

    // Table
    tablePanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    tablePanel.setLayout(new BorderLayout());

    table = new JTable(tableModel = new TableModel());
    table.setDefaultRenderer(String.class, new TableCellRenderer());
    table.setBorder(null);
    table.setBackground(Color.WHITE);
    table.getTableHeader().setReorderingAllowed(false);
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    AnUtility.setAccessibleContext(
        table.getTableHeader().getAccessibleContext(), AnLocale.getString("Table Header"));
    // Handle Enter key
    table.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
              if (HWCSelectPanel.this.dialog.getOKButton().isEnabled()) {
                HWCSelectPanel.this.dialog.getOKButton().doClick();
                e.consume();
              }
            }
          }
        });
    // Handle double-click
    table.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              if (HWCSelectPanel.this.dialog.getOKButton().isEnabled()) {
                HWCSelectPanel.this.dialog.getOKButton().doClick();
                e.consume();
              }
            }
          }
        });

    JPopupMenu popupMenu = new JPopupMenu();
    copySelectedItems = new JMenuItem(AnLocale.getString("Copy Selected"));
    copySelectedItems.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            int selectedRows[] = table.getSelectedRows();
            copyToClipboard(selectedRows);
          }
        });
    copyAllItems = new JMenuItem(AnLocale.getString("Copy All"));
    copyAllItems.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            copyToClipboard(0, table.getRowCount());
          }
        });
    popupMenu.add(copySelectedItems);
    popupMenu.add(copyAllItems);
    table.setComponentPopupMenu(popupMenu);

    listLabel.setLabelFor(table);
    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setBorder(null);
    scrollPane.setBackground(Color.WHITE);
    tablePanel.add(scrollPane, BorderLayout.CENTER);

    includeComboBox.addItemListener(
        includeItemListener =
            new ItemListener() {
              @Override
              public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                  applyFiltersAndUpdateGUI();
                }
              }
            });
    excludeComboBox.addItemListener(
        excludeItemListener =
            new ItemListener() {
              @Override
              public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                  applyFiltersAndUpdateGUI();
                }
              }
            });

    dialog.getOKButton().setEnabled(false);

    table
        .getSelectionModel()
        .addListSelectionListener(
            new ListSelectionListener() {
              @Override
              public void valueChanged(ListSelectionEvent e) {
                HWCSelectPanel.this
                    .dialog
                    .getOKButton()
                    .setEnabled(table.getSelectedRowCount() > 0);
              }
            });

    // Set coulumn widths (available space goes to last column)
    table.getColumnModel().getColumn(0).setPreferredWidth(35);
    table.getColumnModel().getColumn(0).setMaxWidth(35);
    table.getColumnModel().getColumn(1).setPreferredWidth(20);
    table.getColumnModel().getColumn(1).setMaxWidth(20);
    table.getColumnModel().getColumn(2).setPreferredWidth(300);
    table.getColumnModel().getColumn(2).setMaxWidth(800);
    table.getColumnModel().getColumn(3).setPreferredWidth(100);
    table.getColumnModel().getColumn(3).setMaxWidth(800);
    for (int i = 4; i < 6; i++) {
      table.getColumnModel().getColumn(i).setPreferredWidth(20);
      table.getColumnModel().getColumn(i).setMaxWidth(20);
    }
    setPreferredSize(new Dimension(1250, 800));

    applyFiltersAndUpdateGUI();
  }

  private void copyToClipboard(int first, int last) {
    int selectedRows[] = new int[last - first];
    for (int i = 0; i < selectedRows.length; i++) {
      selectedRows[i] = i + first;
    }
    copyToClipboard(selectedRows);
  }

  private void copyToClipboard(int selectedRows[]) {
    int[] columnWidths = getColumnWidths(selectedRows);
    StringBuilder buf = new StringBuilder();
    // Header
    for (int column = 0; column < table.getColumnCount(); column++) {
      String headerText =
          (String) table.getTableHeader().getColumnModel().getColumn(column).getHeaderValue();
      String format = '%' + String.format("-%ds", columnWidths[column]);
      buf.append(String.format(format, headerText));
    }
    buf.append("\n");
    buf.append("\n");
    // Table rows
    for (int i : selectedRows) {
      buf.append(formatRowAsText(columnWidths, i));
      buf.append("\n");
    }

    copyToClipboard(buf.toString());
    System.out.println(buf.toString());
  }

  private String formatRowAsText(int[] columnWidths, int row) {
    StringBuilder buf = new StringBuilder();

    for (int column = 0; column < table.getColumnCount(); column++) {
      Object object = table.getModel().getValueAt(row, column);
      if (object instanceof String) {
        String format = '%' + String.format("-%ds", columnWidths[column]);
        buf.append(String.format(format, object));
      }
    }

    return buf.toString();
  }

  private void copyToClipboard(String text) {
    StringSelection data = new StringSelection(text);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(data, data);
  }

  private int[] getColumnWidths(int selectedRows[]) {
    int[] columnWidths = getFixedColumnWidths();
    for (int row : selectedRows) {
      String text;
      int len;
      text = (String) table.getModel().getValueAt(row, 2);
      len = text.length() + 3;
      if (len > columnWidths[2]) {
        columnWidths[2] = len;
      }
      text = (String) table.getModel().getValueAt(row, 3);
      len = text.length() + 3;
      if (len > columnWidths[3]) {
        columnWidths[3] = len;
      }
      text = (String) table.getModel().getValueAt(row, 6);
      len = text.length() + 3;
      if (len > columnWidths[6]) {
        columnWidths[6] = len;
      }
    }
    return columnWidths;
  }

  private int[] getFixedColumnWidths() {
    int[] columnWidths = new int[table.getColumnCount()];
    columnWidths[0] = 4;
    columnWidths[1] = 2;
    columnWidths[2] = 0; // variable width
    columnWidths[3] = 0; // variable width
    columnWidths[4] = 2;
    columnWidths[5] = 2;
    columnWidths[6] = 0; // variable width
    return columnWidths;
  }

  protected void resetGUI() {
    //        includeComboBox.setSelectedItem("");
    //        excludeComboBox.setSelectedItem("");
  }

  private void updateFilterComboBoxes(String includeFilterString, String excludeFilterString) {
    includePicklist.addElement(includeFilterString);
    excludePicklist.addElement(excludeFilterString);
    updateFilterComboBoxes(
        includePicklist, includeFilterString, excludePicklist, excludeFilterString);
  }

  private boolean containsIgnoreCase(String string, String clause) {
    return string != null && string.toLowerCase().contains(clause.toLowerCase());
  }

  private List<HWCEntry> filterEntries(
      List<HWCEntry> origList, String rawIncludeFilterString, boolean exclude) {
    String[] substrings =
        rawIncludeFilterString.split(" "); // multiword match  //FIXUP bypass escaped spaces?
    //        String[] substrings = {raw_includeFilterString}; // exact text match
    List<String> clauses = new ArrayList();
    for (int ii = 0; ii < substrings.length; ii++) {
      if (substrings[ii].isEmpty()) {
        continue; // strip out empty clauses caused by consecutive spaces
      }
      clauses.add(substrings[ii]);
    }
    final List<HWCEntry> newList;
    if (clauses.isEmpty()) {
      newList = new ArrayList(origList);
    } else {
      newList = new ArrayList();
      for (HWCEntry entry : origList) {
        boolean pass = false;
        for (String clause : clauses) {
          if (containsIgnoreCase(entry.getMetricText(), clause)
              || containsIgnoreCase(entry.getCounterText(), clause)
              || containsIgnoreCase(entry.getDescriptionText(hwcNameMap), clause)) {
            pass = true;
            break;
          }
        }
        if (exclude) {
          if (!pass) {
            newList.add(entry);
          }
        } else if (pass) {
          newList.add(entry);
        }
      }
    }
    return newList;
  }

  private void updateFilterComboBoxes(
      StringPickList includePicklist,
      String currentIncludeFilter,
      StringPickList excludePicklist,
      String currentExcludeFilter) {
    AnUtility.checkIfOnAWTThread(true);
    includeComboBox.removeAllItems();
    includeComboBox.addItem("");
    for (StringPickListElement elem : includePicklist.getStringElements()) {
      if (elem.getString().length() > 0) {
        includeComboBox.addItem(elem.getString());
      }
    }
    if (currentIncludeFilter != null) {
      includeComboBox.setSelectedItem(currentIncludeFilter);
    }

    excludeComboBox.removeAllItems();
    excludeComboBox.addItem("");
    for (StringPickListElement elem : excludePicklist.getStringElements()) {
      if (elem.getString().length() > 0) {
        excludeComboBox.addItem(elem.getString());
      }
    }
    if (currentExcludeFilter != null) {
      excludeComboBox.setSelectedItem(currentExcludeFilter);
    }
  }

  private void applyFiltersAndUpdateGUI() {
    AnUtility.checkIfOnAWTThread(true);
    //        System.out.println("applyFiltersAndUpdateGUI");
    includeComboBox.removeItemListener(includeItemListener);
    excludeComboBox.removeItemListener(excludeItemListener);

    hwcFilteredList = new ArrayList<HWCEntry>();

    // Check boxes
    for (HWCEntry hwcEntry : hwcFlatList) {
      if (recommendedCheckBox.isSelected() && !hwcEntry.isRecommended()) {
        continue;
      }
      if (timeCheckBox.isSelected() && !hwcEntry.supportsTime(hwcNameMap)) {
        continue;
      }
      if (memoryCheckBox.isSelected() && !hwcEntry.supportsMemspace(hwcNameMap)) {
        continue;
      }

      hwcFilteredList.add(hwcEntry);
    }

    // Text filters
    String includeFilterString = (String) includeComboBox.getSelectedItem();
    String excludeFilterString = (String) excludeComboBox.getSelectedItem();

    if (includeFilterString == null) {
      includeFilterString = "";
    }
    if (excludeFilterString == null) {
      excludeFilterString = "";
    }
    hwcFilteredList = filterEntries(hwcFilteredList, includeFilterString, false);
    hwcFilteredList = filterEntries(hwcFilteredList, excludeFilterString, true);
    includeComboBox.addItem(includeFilterString);
    excludeComboBox.addItem(excludeFilterString);

    // Update table
    tableModel.fireTableDataChanged();
    // Update labels
    String format0 = AnLocale.getString("  (%d/%d shown)");
    maxListLabel.setText(String.format(format0, hwcFilteredList.size(), hwcFlatList.size()));
    String format1 =
        AnLocale.getString("Available HW counters, showing %s out of %s available counters");
    String tooltip = String.format(format1, hwcFilteredList.size(), hwcFlatList.size());
    maxListLabel.setToolTipText(tooltip);
    listLabel.setToolTipText(tooltip);

    // Update comboboxes
    updateFilterComboBoxes(includeFilterString, excludeFilterString);

    includeComboBox.addItemListener(includeItemListener);
    excludeComboBox.addItemListener(excludeItemListener);

    copySelectedItems.setEnabled(table.getRowCount() > 0);
    copyAllItems.setEnabled(table.getRowCount() > 0);

    requestTableFocus();
  }

  private void requestTableFocus() {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            if (table.getModel().getRowCount() > 0) {
              table.getSelectionModel().setSelectionInterval(0, 0);
              table.requestFocus();
            }
          }
        });
  }

  class TableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JLabel label =
          (JLabel)
              super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      HWCEntry hwcEntry = hwcFilteredList.get(table.convertRowIndexToModel(row));
      if (column == 0 || column == 1 || column == 4 || column == 5) {
        label.setHorizontalAlignment(JLabel.CENTER);
      } else {
        label.setHorizontalAlignment(JLabel.LEFT);
      }
      if (column == 1 && hwcEntry.isRecommended()) {
        label.setIcon(AnUtility.circleGreenIcon);
        label.setText("");
      } else if (column == 4 && hwcEntry.supportsTime(hwcNameMap)) {
        label.setIcon(AnUtility.circleRedIcon);
        label.setText("");
      } else if (column == 5 && hwcEntry.supportsMemspace(hwcNameMap)) {
        label.setIcon(AnUtility.circleBlueIcon);
        label.setText("");
      } else {
        label.setIcon(null);
      }
      String ttText = hwcEntry.getDescriptionText(hwcNameMap);
      if (ttText == null || ttText.length() == 0) {
        ttText = hwcEntry.getMetricText();
      }
      label.setToolTipText(ttText);
      return label;
    }
  }

  class TableModel extends AbstractTableModel {

    private String[] columnNames = {
      "#",
      AnLocale.getString("R"),
      AnLocale.getString("Metric"),
      AnLocale.getString("Counter"),
      AnLocale.getString("T"),
      AnLocale.getString("M"),
      AnLocale.getString("Description")
    };

    @Override
    public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public int getRowCount() {
      int rowCount;
      if (hwcFilteredList == null) {
        rowCount = 0;
      } else {
        rowCount = hwcFilteredList.size();
      }
      return rowCount;
    }

    @Override
    public String getColumnName(int col) {
      return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
      if (row >= hwcFilteredList.size()) {
        return null; // FIXUP: why is this necessary?
      }
      HWCEntry hwcEntry = hwcFilteredList.get(row);
      switch (col) {
        case 0:
          {
            return String.format("%03d", hwcEntry.getNo());
          }
        case 1:
          {
            return hwcEntry.isRecommended() ? "R" : "";
          }
        case 2:
          { // Metric
            return hwcEntry.getMetricText();
          }
        case 3:
          { // Counter
            return hwcEntry.getCounterText();
          }
        case 4:
          {
            return hwcEntry.supportsTime(hwcNameMap) ? "T" : "";
          }
        case 5:
          {
            return hwcEntry.supportsMemspace(hwcNameMap) ? "M" : "";
          }
        case 6:
          { // Description
            return hwcEntry.getDescriptionText(hwcNameMap);
          }
        default:
          {
            return "???";
          }
      }
    }

    @Override
    public Class getColumnClass(int c) {
      Object o = getValueAt(0, c);
      if (o != null) {
        return getValueAt(0, c).getClass();
      } else {
        return super.getColumnClass(c);
      }
    }
  }

  protected List<HWCEntry> getSelectedEntries() {
    List<HWCEntry> list = new ArrayList<HWCEntry>();

    int selected[] = table.getSelectedRows();
    for (int i : selected) {
      list.add(hwcFilteredList.get(table.convertRowIndexToModel(i)).copy());
    }

    return list;
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    infoLabel = new javax.swing.JLabel();
    filterPanel = new javax.swing.JPanel();
    filterLabel = new javax.swing.JLabel();
    checkBoxPanel = new javax.swing.JPanel();
    recommendedIconLabel = new javax.swing.JLabel();
    recommendedCheckBox = new javax.swing.JCheckBox();
    timeIconLabel = new javax.swing.JLabel();
    timeCheckBox = new javax.swing.JCheckBox();
    memoryIconLabel = new javax.swing.JLabel();
    memoryCheckBox = new javax.swing.JCheckBox();
    includeLabel = new javax.swing.JLabel();
    includeComboBox = new javax.swing.JComboBox();
    excludeLabel = new javax.swing.JLabel();
    excludeComboBox = new javax.swing.JComboBox();
    refreshButton = new javax.swing.JButton();
    listLabelPanel = new javax.swing.JPanel();
    listLabel = new javax.swing.JLabel();
    maxListLabel = new javax.swing.JLabel();
    tablePanel = new javax.swing.JPanel();

    setLayout(new java.awt.GridBagLayout());

    infoLabel.setText("info...");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    add(infoLabel, gridBagConstraints);

    filterPanel.setLayout(new java.awt.GridBagLayout());

    filterLabel.setText("Filter");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.ipady = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    filterPanel.add(filterLabel, gridBagConstraints);

    checkBoxPanel.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    checkBoxPanel.add(recommendedIconLabel, gridBagConstraints);

    recommendedCheckBox.setText("Recommended");
    recommendedCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            recommendedCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    checkBoxPanel.add(recommendedCheckBox, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    checkBoxPanel.add(timeIconLabel, gridBagConstraints);

    timeCheckBox.setText("Time");
    timeCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            timeCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    checkBoxPanel.add(timeCheckBox, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    checkBoxPanel.add(memoryIconLabel, gridBagConstraints);

    memoryCheckBox.setText("Memory");
    memoryCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            memoryCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    checkBoxPanel.add(memoryCheckBox, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    filterPanel.add(checkBoxPanel, gridBagConstraints);

    includeLabel.setText("Include");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
    filterPanel.add(includeLabel, gridBagConstraints);

    includeComboBox.setEditable(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
    filterPanel.add(includeComboBox, gridBagConstraints);

    excludeLabel.setText("and exclude");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 0);
    filterPanel.add(excludeLabel, gridBagConstraints);

    excludeComboBox.setEditable(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
    filterPanel.add(excludeComboBox, gridBagConstraints);

    refreshButton.setText("Refresh");
    refreshButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            refreshButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 0);
    filterPanel.add(refreshButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(filterPanel, gridBagConstraints);

    listLabelPanel.setLayout(new java.awt.GridBagLayout());

    listLabel.setText("Available Counters:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    listLabelPanel.add(listLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    listLabelPanel.add(maxListLabel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    add(listLabelPanel, gridBagConstraints);

    tablePanel.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(tablePanel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void refreshButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_refreshButtonActionPerformed
    applyFiltersAndUpdateGUI();
  } // GEN-LAST:event_refreshButtonActionPerformed

  private void recommendedCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_recommendedCheckBoxActionPerformed
    applyFiltersAndUpdateGUI();
  } // GEN-LAST:event_recommendedCheckBoxActionPerformed

  private void timeCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_timeCheckBoxActionPerformed
    applyFiltersAndUpdateGUI();
  } // GEN-LAST:event_timeCheckBoxActionPerformed

  private void memoryCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_memoryCheckBoxActionPerformed
    applyFiltersAndUpdateGUI();
  } // GEN-LAST:event_memoryCheckBoxActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel checkBoxPanel;
  private javax.swing.JComboBox excludeComboBox;
  private javax.swing.JLabel excludeLabel;
  private javax.swing.JLabel filterLabel;
  private javax.swing.JPanel filterPanel;
  private javax.swing.JComboBox includeComboBox;
  private javax.swing.JLabel includeLabel;
  private javax.swing.JLabel infoLabel;
  private javax.swing.JLabel listLabel;
  private javax.swing.JPanel listLabelPanel;
  private javax.swing.JLabel maxListLabel;
  private javax.swing.JCheckBox memoryCheckBox;
  private javax.swing.JLabel memoryIconLabel;
  private javax.swing.JCheckBox recommendedCheckBox;
  private javax.swing.JLabel recommendedIconLabel;
  private javax.swing.JButton refreshButton;
  private javax.swing.JPanel tablePanel;
  private javax.swing.JCheckBox timeCheckBox;
  private javax.swing.JLabel timeIconLabel;
  // End of variables declaration//GEN-END:variables
}

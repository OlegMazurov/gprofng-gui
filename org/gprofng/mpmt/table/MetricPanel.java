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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnTable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

public class MetricPanel extends JPanel implements Transferable {

  static {
    DragSource.getDefaultDragSource().addDragSourceListener(new MetricPanelDragSourceListener());
  }

  private AnTableHeaderPanel anTableHeaderPanel;
  private AnMetric anMetric;
  private AnTable anTable;
  private MetricLabel[] metricLabels;

  private int maxMetricNameLabels = 16;
  private ExclusiveAndInclusivePanel exclusiveAndInclusivePanel;
  private JLabel removeIconLabel;
  private JLabel configureIconLabel;
  private int column;
  private int count;
  private JPanel iconNamePanel;
  private JLabel[] metricNameLabels = new JLabel[maxMetricNameLabels];

  public MetricPanel(
      AnTableHeaderPanel anTableHeaderPanel,
      AnMetric anMetric,
      AnTable anTable,
      final MetricLabel[] metricLabels,
      boolean wrapMetricName,
      final int column,
      int count,
      Color backgroundcolor) {
    this.anTableHeaderPanel = anTableHeaderPanel;
    this.anMetric = anMetric;
    this.anTable = anTable;
    this.metricLabels = metricLabels;
    this.column = column;
    this.count = count;
    String metricName = anMetric.getUserName();
    setOpaque(false);
    setOpaque(true);
    setBackground(backgroundcolor);
    setLayout(new GridBagLayout());
    setBorder(
        BorderFactory.createMatteBorder(
            0, 0, 0, 1, AnEnvironment.TABLE_VERTICAL_GRID_METRIC_COLOR));
    GridBagConstraints gridBagConstraints = new GridBagConstraints();

    // Mouse events
    HeaderMouseHandler headerMouseHandler = new HeaderMouseHandler(this);
    addMouseMotionListener(headerMouseHandler);
    addMouseListener(headerMouseHandler);

    //            // Sort metric or not?
    //            boolean sortedByThisMetric = false;
    //            if (anTable.canSort()) {
    //                for (int i = 0; i < count; i++) {
    //                    if (metricLabels[index + i].getAnMetric().isSorted()) {
    //                        sortedByThisMetric = true;
    //                        break;
    //                    }
    //                }
    //            }
    iconNamePanel = new JPanel(new GridBagLayout());
    iconNamePanel.setOpaque(false);
    //            if (sortedByThisMetric) {
    //                Color moreBluish = new Color(backgroundcolor.getRed() - 6,
    // backgroundcolor.getGreen() - 6, backgroundcolor.getBlue());
    //                iconNamePanel.setOpaque(true);
    //                iconNamePanel.setBackground(moreBluish);
    //            }
    // Configure icon
    if (!metricLabels[column].getAnMetric().isNameMetric()) {
      configureIconLabel = new JLabel(AnUtility.hamburgerBlankIcon);
      configureIconLabel.setToolTipText(AnLocale.getString("Configure this metric"));
      AnUtility.setAccessibleContext(
          configureIconLabel.getAccessibleContext(), configureIconLabel.getToolTipText());
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.insets = new Insets(1, 1, 0, 2);
      gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
      iconNamePanel.add(configureIconLabel, gridBagConstraints);
      configureIconLabel.addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              showPopup(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
              configureIconLabel.setIcon(AnUtility.hamburgerFocusedIcon);
              removeIconLabel.setIcon(AnUtility.removeIcon);
            }

            @Override
            public void mouseExited(MouseEvent e) {
              configureIconLabel.setIcon(AnUtility.hamburgerIcon);
            }
          });
    }
    // Metric name
    // Use metricNameLabels[0] as default
    // metricNameLabels[1-9] are for wrapped metric names
    if (!metricLabels[column].getAnMetric().isNameMetric()) {
      metricNameLabels[0] = new JLabel(metricName);
      AnUtility.setAccessibleContext(
          metricNameLabels[0].getAccessibleContext(), metricNameLabels[0].getText());
      metricNameLabels[0].setHorizontalAlignment(JLabel.CENTER);
      String ttText = metricName;
      if (anMetric.getShortDesc() != null) {
        ttText += " - " + anMetric.getShortDesc();
      }
      setToolTipText(AnLocale.getString("Metric") + ": " + ttText);
    } else {
      if (anTable.getType() == AnDisplay.DSP_Source
          || anTable.getType() == AnDisplay.DSP_SourceV2
          || anTable.getType() == AnDisplay.DSP_Disassembly
          || anTable.getType() == AnDisplay.DSP_DisassemblyV2
          || anTable.getType() == AnDisplay.DSP_SourceDisassembly) {
        String labelText = anTable.getNames()[0];
        labelText = labelText.replaceFirst(".*: ", "");
        labelText = labelText.replaceFirst(" \\(.*", "");
        metricNameLabels[0] = new JLabel(labelText);
        AnUtility.setAccessibleContext(
            metricNameLabels[0].getAccessibleContext(), metricNameLabels[0].getText());
      } else {
        metricNameLabels[0] = new JLabel(anTable.getNames()[0]);
        AnUtility.setAccessibleContext(
            metricNameLabels[0].getAccessibleContext(), metricNameLabels[0].getText());
      }
      setToolTipText(anTable.getNames()[0]);
    }
    metricNameLabels[0].setFont(AnTableHeaderPanel.smallBoldFont);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    if (metricLabels[column].getAnMetric().isNameMetric()) {
      gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    }
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    iconNamePanel.add(metricNameLabels[0], gridBagConstraints);

    for (int i = 1; i < maxMetricNameLabels; i++) {
      metricNameLabels[i] = new JLabel(wrapMetricName ? "" : "");
      AnUtility.setAccessibleContext(
          metricNameLabels[i].getAccessibleContext(), metricNameLabels[i].getText());
      metricNameLabels[i].setFont(AnTableHeaderPanel.smallBoldFont);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = i;
      iconNamePanel.add(metricNameLabels[i], gridBagConstraints);
    }

    // Remove icon
    if (!metricLabels[column].getAnMetric().isNameMetric()) {
      removeIconLabel = new JLabel(AnUtility.removeIconBlank);
      removeIconLabel.setToolTipText(AnLocale.getString("Remove this metric"));
      AnUtility.setAccessibleContext(
          removeIconLabel.getAccessibleContext(), removeIconLabel.getToolTipText());
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.insets = new Insets(1, 2, 0, 1);
      gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
      iconNamePanel.add(removeIconLabel, gridBagConstraints);
      removeIconLabel.addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              AnWindow.getInstance()
                  .getSettings()
                  .getMetricsSetting()
                  .set(this, metricLabels[column].getAnMetric().getComd(), false);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
              removeIconLabel.setIcon(AnUtility.removeFocusedIcon);
              configureIconLabel.setIcon(AnUtility.hamburgerIcon);
            }

            @Override
            public void mouseExited(MouseEvent e) {
              removeIconLabel.setIcon(AnUtility.removeIcon);
            }
          });
    }
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    add(iconNamePanel, gridBagConstraints);

    // Add Exclusive/Inclusive panel
    exclusiveAndInclusivePanel =
        new ExclusiveAndInclusivePanel(
            this, anTable, metricLabels, wrapMetricName, column, count, headerMouseHandler);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(2, 0, 0, 0);
    add(exclusiveAndInclusivePanel, gridBagConstraints);

    // DnD
    addMouseMotionListener(new MouseDraggedListener());
    setTransferHandler(new DADTransferHandler());
    setDropTarget(new DropTarget(this, new MetricPanelDropTargetListener()));
  }

  /**
   * @return the anTableHeaderPanel
   */
  public AnTableHeaderPanel getAnTableHeaderPanel() {
    return anTableHeaderPanel;
  }

  /**
   * @return the anMetric
   */
  public AnMetric getAnMetric() {
    return anMetric;
  }

  class MouseDraggedListener extends MouseMotionAdapter {

    // @Override
    @Override
    public void mouseDragged(MouseEvent e) {
      JComponent c = (JComponent) e.getSource();
      TransferHandler handler = c.getTransferHandler();
      handler.exportAsDrag(c, e, TransferHandler.COPY);
    }
  }

  private static DataFlavor dadDataFlavor = null;

  public static DataFlavor getDADDataFlavor() throws Exception {
    if (dadDataFlavor == null) {
      dadDataFlavor =
          new DataFlavor(
              DataFlavor.javaJVMLocalObjectMimeType
                  + ";class=org.gprofng.mpmt.table.MetricPanel");
    }
    return dadDataFlavor;
  }

  // @Override
  @Override
  public Object getTransferData(DataFlavor flavor) {
    DataFlavor thisFlavor;

    try {
      thisFlavor = getDADDataFlavor();
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      return null;
    }

    if (thisFlavor != null && flavor.equals(thisFlavor)) {
      return MetricPanel.this;
    }

    return null;
  }

  // @Override
  @Override
  public DataFlavor[] getTransferDataFlavors() {
    DataFlavor[] flavors = {null};

    try {
      flavors[0] = getDADDataFlavor();
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      return null;
    }

    return flavors;
  }

  // @Override
  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    DataFlavor[] flavors = {null};
    try {
      flavors[0] = getDADDataFlavor();
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      return false;
    }

    for (DataFlavor f : flavors) {
      if (f.equals(flavor)) {
        return true;
      }
    }

    return false;
  }

  class DADTransferHandler extends TransferHandler {

    public DADTransferHandler() {
      super();
    }

    // @Override()
    @Override
    public Transferable createTransferable(JComponent c) {
      if (c instanceof MetricPanel) {
        Transferable tip = (MetricPanel) c;
        return tip;
      }
      return null;
    }

    // @Override()
    @Override
    public int getSourceActions(JComponent c) {
      if (c instanceof MetricPanel) {
        return TransferHandler.COPY;
      }

      return TransferHandler.NONE;
    }
  }

  protected void mouseEntered(MouseEvent e) {
    if (removeIconLabel != null) {
      removeIconLabel.setIcon(AnUtility.removeIcon);
    }
    if (configureIconLabel != null) {
      configureIconLabel.setIcon(AnUtility.hamburgerIcon);
    }
  }

  public void mouseExited(MouseEvent e) {
    if (removeIconLabel != null) {
      removeIconLabel.setIcon(AnUtility.removeIconBlank);
    }
    if (configureIconLabel != null) {
      configureIconLabel.setIcon(AnUtility.hamburgerBlankIcon);
    }
  }

  protected void showPopup(MouseEvent event) {
    MetricLabel metricLabel = metricLabels[column];
    JPopupMenu popup = new JPopupMenu();
    AnWindow.getInstance()
        .getSettings()
        .getMetricsSetting()
        .addMetricsPopupMenus(popup, metricLabel.getAnMetric(), anTable, getSortByMenu());
    popup.show(event.getComponent(), event.getX() + 5, event.getY() + 5);
  }

  private JMenu getSortByMenu() {
    JMenu sortByMenuItem = new JMenu(AnLocale.getString("Sort This Metric By"));
    if (anTable.canSort()) {
      if (metricLabels[column].getAnMetric().isNameMetric()) {
        JMenuItem menuItem = new JMenuItem(metricLabels[column].getAnMetric().getUserName());
        menuItem.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                AnWindow.getInstance()
                    .getSettings()
                    .getMetricsSetting()
                    .setSortMetricByDType(this, column, anTable.getType());
              }
            });
        sortByMenuItem.add(menuItem);
      } else {
        List<JMenuItem> list = exclusiveAndInclusivePanel.getSortByMenuItems();
        for (JMenuItem menuItem : list) {
          sortByMenuItem.add(menuItem);
        }
      }
    } else {
      sortByMenuItem.setEnabled(false);
    }
    return sortByMenuItem;
  }

  private int labelWidth(String text) {
    AnTableHeaderPanel.testLabel.setText(text);
    return AnTableHeaderPanel.testLabel.getPreferredSize().width;
  }

  protected int splitMetricNameIntoMultipleRows(int maxPanelWidth) {
    //            System.out.println("splitMetricNameIntoMultipleRows: " + maxPanelWidth);
    int maxLabelWidth = maxPanelWidth - 25; // subtract icon widths and spacing
    String metricName = metricNameLabels[0].getText();
    //            metricName =
    // "cpu_clk_thread_unhalted.ref_xclk~msr_offcore=0~edge=0~umask=0~anythr=0/0";
    //            metricName =
    // "cpu_clk_thread_unhalted.ref_xclk~msr_offcore=0~edge=0~umask=0~anythr=0/0
    // xxx\\xxxx\\xxxx\\xxxx\\xxxxdsa 1111 222 33333 44444 55555";
    //            metricName =
    // "br_inst_exec.all_indirect_jump_non_call_ret~edge=0~inv=0~in_tx=0~system=0~msr_offcore=0/1,on";
    String[] split = metricName.split(" |_|\\.|~|-|\\\\");
    if (split.length == 1) {
      // do nothing
      return 1;
    }
    int masterIndex = -1;
    Character splitCharNextLine = null;
    for (int i = 0; i < split.length - 1; i++) {
      masterIndex += split[i].length() + 1;
      char splitChar = metricName.charAt(masterIndex);
      //                System.out.println(splitChar);
      if (splitCharNextLine != null) {
        split[i] = splitCharNextLine + split[i];
        splitCharNextLine = null;
      }
      if (splitChar != '~') {
        split[i] = split[i] + splitChar;
      } else {
        splitCharNextLine = splitChar;
      }
    }

    int row = 0;
    int start = 0;
    while (start < split.length) {
      int count = 0;
      StringBuilder sb = new StringBuilder();
      while (true) {
        if ((start + count) >= (split.length)) {
          break;
        }
        if (labelWidth(sb.toString() + split[start + count]) >= maxLabelWidth && count != 0) {
          break;
        }
        if (count > 0 && split[start + count].toString().charAt(0) == '~') {
          break;
        }
        sb.append(split[start + count]);
        count++;
      }
      start = start + count;

      metricNameLabels[row].setText(sb.toString());
      if ((row + 1) >= maxMetricNameLabels) {
        metricNameLabels[row].setText(metricNameLabels[row].getText() + "...");
        row++;
        break;
      }
      row++;
    }
    return row;
  }

  protected void adjustMetricNameRows(int rows) {
    for (int i = 1; i <= rows - 1; i++) {
      if (metricNameLabels[i].getText().isEmpty()) {
        metricNameLabels[i].setText(" ");
      }
    }
  }

  /**
   * @return the iconNamePanel
   */
  public JPanel getIconNamePanel() {
    return iconNamePanel;
  }

  /**
   * @return the exclusiveAndInclusivePanel
   */
  public ExclusiveAndInclusivePanel getExclusiveAndInclusivePanel() {
    return exclusiveAndInclusivePanel;
  }

  public String dump() {
    return metricNameLabels[0].getText()
        + metricNameLabels[1].getText()
        + metricNameLabels[2].getText();
  }

  static class MetricPanelDragSourceListener implements DragSourceListener {
    // @Override
    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
      //                System.out.println("DragSource:dragDropEnd: " +
      // dsde.getDragSourceContext().getComponent());
      if (dsde.getDragSourceContext().getComponent() instanceof MetricPanel) {
        MetricPanel metricPanel = (MetricPanel) dsde.getDragSourceContext().getComponent();
        metricPanel.getAnTableHeaderPanel().setDropPosition(-1);
      }
    }

    // @Override
    @Override
    public void dragExit(DragSourceEvent dse) {
      //                System.out.println("DragSource:dragExit: " + dse);
      if (dse.getDragSourceContext().getComponent() instanceof MetricPanel) {
        MetricPanel metricPanel = (MetricPanel) dse.getDragSourceContext().getComponent();
        metricPanel.getAnTableHeaderPanel().setDropPosition(-1);
      }
    }

    // @Override
    @Override
    public void dragEnter(DragSourceDragEvent dsde) {
      //                System.out.println("DragSource:dragEnter: " + dsde.getLocation());
    }

    @Override
    public void dragOver(DragSourceDragEvent dsde) {
      //                System.out.println("DragSource:dragOver: " + dsde.getLocation());
      if (dsde.getDragSourceContext().getComponent() instanceof MetricPanel) {
        MetricPanel metricPanel = (MetricPanel) dsde.getDragSourceContext().getComponent();
        metricPanel.getAnTableHeaderPanel().setDropLocation(dsde.getLocation().x);
      }
    }

    @Override
    public void dropActionChanged(DragSourceDragEvent dsde) {}
  }
}

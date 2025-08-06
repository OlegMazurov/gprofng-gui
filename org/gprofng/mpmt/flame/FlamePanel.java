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

package org.gprofng.mpmt.flame;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.filter.Filters;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.util.gui.AnGradientPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

public class FlamePanel extends JPanel implements MouseListener, MouseMotionListener {

  private enum State {
    CLEAR,
    RESET,
    ROWS
  };

  private final FlameView flameView;
  private State state;
  private final Object stateLock = new Object();
  private int lastWidth = 0;
  private final DataPanel dataPanel;

  private int rowHeight;
  private Font font;
  private final int defaultFontSize;

  private static final int outerGap = 2;
  private static final int innerGap = 2;

  protected FlamePanel(FlameView flameView) {
    this.flameView = flameView;
    setOpaque(true);
    setBackground(AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR);

    font = getFont().deriveFont((float) (getFont().getSize() - 0));
    defaultFontSize = font.getSize();
    rowHeight = font.getSize() + 4;
    state = State.CLEAR;

    setLayout(new GridBagLayout());
    dataPanel = new DataPanel();
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(outerGap, outerGap, outerGap, outerGap);
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;

    add(dataPanel, gridBagConstraints);

    dataPanel.addMouseListener(this);
    dataPanel.addMouseMotionListener(this);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (flameView.getFlameData() != null) {
      FlameBlock mouseOverFlameBlock = flameView.getFlameData().getMouseOverFlameBlock();
      if (mouseOverFlameBlock != null) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          setSelectedFlameBlock(mouseOverFlameBlock);
          if (e.getClickCount() >= 2) {
            //                    System.out.println("left mouse clicked " + e.getClickCount() + " "
            // + mouseOverFlameBlock.getFunctionName());
            setBaseFlameBlock(mouseOverFlameBlock, true);
          }
        }
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {}

  @Override
  public void mousePressed(MouseEvent e) {
    if (flameView.getFlameData() == null) {
      return;
    }
    final FlameBlock mouseOverFlameBlock = flameView.getFlameData().getMouseOverFlameBlock();
    if (SwingUtilities.isRightMouseButton(e)) {
      if (mouseOverFlameBlock != null) {
        setSelectedFlameBlock(mouseOverFlameBlock);
      }
      if (e.isPopupTrigger()) {
        JPopupMenu popup = new JPopupMenu("Popup");
        FlameBlock selectedFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
        for (JComponent jmi : getMenuList(selectedFlameBlock)) {
          popup.add(jmi);
        }
        popup.show(dataPanel, e.getX(), e.getY());
      }
    }
  }

  protected void showPopup(FlameBlock selectedFlameBlock) {
    //        int row = flameBlock.getFlameRow().getRow();
    //        int x = flameBlock.getXPos();
    //        int y = dataPanel.getHeight() - (rowHeight * row);

    Point mousePoint = MouseInfo.getPointerInfo().getLocation(); // screen position
    Point panelPoint = dataPanel.getLocationOnScreen();
    int x = mousePoint.x - panelPoint.x;
    int y = mousePoint.y - panelPoint.y;

    JPopupMenu popup = new JPopupMenu("Popup");
    for (JComponent jmi : getMenuList(selectedFlameBlock)) {
      popup.add(jmi);
    }
    popup.show(dataPanel, x, y);
  }

  private List<JComponent> getMenuList(final FlameBlock selectedFlameBlock) {
    List<JComponent> list = new ArrayList<>();

    JMenuItem showSource = new JMenuItem(AnLocale.getString("Show Source"));
    showSource.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance().setSelectedView(AnDisplay.DSP_Source);
          }
        });
    list.add(showSource);

    JMenuItem showDis = new JMenuItem(AnLocale.getString("Show Disassembly"));
    showDis.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance().setSelectedView(AnDisplay.DSP_Disassembly);
          }
        });
    list.add(showDis);

    list.add(new JSeparator());

    for (JComponent jmi : flameView.getFilterMenuList(selectedFlameBlock)) {
      list.add(jmi);
    }
    list.add(new JSeparator());

    JMenu setMetricMenu = new JMenu(AnLocale.getString("Set Metric"));
    AnMetric[] availableMetrics =
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .getMetricListByMType(MetricsSetting.MET_CALL_AGR);
    if (availableMetrics != null) {
      for (AnMetric am : availableMetrics) {
        if (am.isNameMetric() || am.isStatic()) {
          continue;
        }
        final AnMetric am2 = am;
        JMenuItem metricMenuItem = new JMenuItem(am2.getUserName());
        metricMenuItem.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                flameView.setMetric(am2);
              }
            });
        setMetricMenu.add(metricMenuItem);
      }
      // More metrics
      JMenu moreMetricMenu = new JMenu(AnLocale.getString("More Metrics"));
      AnWindow.getInstance()
          .getSettings()
          .getMetricsSetting()
          .addMetricMenuSelector(moreMetricMenu, AnDisplay.DSP_CallFlame, false);
      // Customize: diasble already enabled metrics (?)
      for (Component component : moreMetricMenu.getMenuComponents()) {
        if (component instanceof MetricsSetting.MetricCheckBox) {
          if (((MetricsSetting.MetricCheckBox) component).isSelected()) {
            component.setEnabled(false);
          }
        }
      }
      setMetricMenu.add(moreMetricMenu);
    }
    list.add(setMetricMenu);
    list.add(new JSeparator());

    list.add(flameView.getUndoSetBaseAction().getMenuItem(false));
    list.add(flameView.getRedoSetBaseAction().getMenuItem(false));
    list.add(flameView.getSetBaseAction().getMenuItem(false));
    list.add(flameView.getResetBaseAction().getMenuItem(false));
    list.add(new JSeparator());
    list.add(flameView.getUpOneAction().getMenuItem(false));
    list.add(flameView.getDownOneAction().getMenuItem(false));
    list.add(flameView.getLeftOneAction().getMenuItem(false));
    list.add(flameView.getRightOneAction().getMenuItem(false));

    if (selectedFlameBlock == null) {
      showSource.setEnabled(false);
      showDis.setEnabled(false);
    }

    return list;
  }

  protected void setAsBase() {
    FlameBlock newBaseFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
    if (newBaseFlameBlock != null) {
      setBaseFlameBlock(newBaseFlameBlock, true);
      flameView.scrollToRow(newBaseFlameBlock.getFlameRow().getRow());
    }
    flameView.updateActionsEnableStatus();
  }

  protected boolean canSetBase() {
    return flameView != null
        && flameView.getFlameData() != null
        && flameView.getFlameData().getSelectedFlameBlock()
            != flameView.getFlameData().getBaseFlameBlock();
  }

  protected void resetBase() {
    FlameBlock selectedFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
    FlameBlock newBaseFlameBlock = flameView.getFlameData().getTotalFlameBlock();
    if (newBaseFlameBlock != null) {
      setBaseFlameBlock(newBaseFlameBlock, true);
      flameView.scrollToRow(newBaseFlameBlock.getFlameRow().getRow());
    }
    flameView.getFlameData().setSelectedFlameBlock(selectedFlameBlock);
    flameView.updateActionsEnableStatus();
  }

  protected boolean canResetBase() {
    return flameView != null
        && flameView.getFlameData() != null
        && flameView.getFlameData().getBaseFlameBlock()
            != flameView.getFlameData().getTotalFlameBlock();
  }

  protected void upOne() {
    FlameBlock selectedFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
    FlameBlock newSelectedFlameBlock = selectedFlameBlock.getNextFlameBlock(FlameBlock.LAST_UP);
    if (newSelectedFlameBlock == null) {
      newSelectedFlameBlock = selectedFlameBlock.getNextFlameBlock(FlameBlock.UP);
    }
    if (newSelectedFlameBlock != null) {
      setSelectedFlameBlock(newSelectedFlameBlock);
      flameView.scrollToRow(newSelectedFlameBlock.getFlameRow().getRow());
    }
    flameView.updateActionsEnableStatus();
  }

  protected boolean canUpOne() {
    return flameView != null
        && flameView.getFlameData() != null
        && flameView.getFlameData().getSelectedFlameBlock() != null
        && flameView.getFlameData().getSelectedFlameBlock().getNextFlameBlock(FlameBlock.UP)
            != null;
  }

  protected void downOne() {
    FlameBlock selectedFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
    FlameBlock newSelectedFlameBlock = selectedFlameBlock.getNextFlameBlock(FlameBlock.DOWN);
    if (newSelectedFlameBlock != null) {
      newSelectedFlameBlock.getNext()[FlameBlock.LAST_UP] = selectedFlameBlock;
      setSelectedFlameBlock(newSelectedFlameBlock);
      flameView.scrollToRow(newSelectedFlameBlock.getFlameRow().getRow());
    }
    flameView.updateActionsEnableStatus();
  }

  protected boolean canDownOne() {
    return flameView != null
        && flameView.getFlameData() != null
        && flameView.getFlameData().getSelectedFlameBlock() != null
        && flameView.getFlameData().getSelectedFlameBlock().getNextFlameBlock(FlameBlock.DOWN)
            != null;
  }

  protected void leftOne() {
    FlameBlock selectedFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
    FlameBlock newSelectedFlameBlock = selectedFlameBlock.getNextFlameBlock(FlameBlock.LEFT);
    if (newSelectedFlameBlock != null) {
      setSelectedFlameBlock(newSelectedFlameBlock);
    }
    flameView.updateActionsEnableStatus();
  }

  protected boolean canLeftOne() {
    return flameView != null
        && flameView.getFlameData() != null
        && flameView.getFlameData().getSelectedFlameBlock() != null
        && flameView.getFlameData().getSelectedFlameBlock().getNextFlameBlock(FlameBlock.LEFT)
            != null;
  }

  protected void rightOne() {
    FlameBlock selectedFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
    FlameBlock newSelectedFlameBlock = selectedFlameBlock.getNextFlameBlock(FlameBlock.RIGHT);
    if (newSelectedFlameBlock != null) {
      setSelectedFlameBlock(newSelectedFlameBlock);
    }
    flameView.updateActionsEnableStatus();
  }

  protected boolean canRightOne() {
    return flameView != null
        && flameView.getFlameData() != null
        && flameView.getFlameData().getSelectedFlameBlock() != null
        && flameView.getFlameData().getSelectedFlameBlock().getNextFlameBlock(FlameBlock.RIGHT)
            != null;
  }

  protected void setBaseFlameBlock(FlameBlock baseFlameBlock, boolean pushToSetBaseStack) {
    //        System.out.println("FlamePanel setBaseFlameBlock " + pushToSetBaseStack);
    if (pushToSetBaseStack) {
      flameView.getFlameToolBar().pushToSetBaseStack(baseFlameBlock);
    }
    flameView.getFlameData().setBaseFlameBlock(baseFlameBlock);
    flameView.getFlameData().recalculateFlameBlockWidths(dataPanel.getWidth() - 2 * innerGap);
    dataPanel.repaint(500);
    flameView.updateActionsEnableStatus();
    flameView.requestFocus(); // so key events keep working! (???).
  }

  protected void setSelectedFlameBlock(FlameBlock selectedFlameBlock) {
    if (flameView.getFlameData().setSelectedFlameBlock(selectedFlameBlock)) {
      dataPanel.repaint(500);
    }
    flameView.updateActionsEnableStatus();
    flameView.requestFocus(); // so key events keep working! (???).
  }

  @Override
  public void mouseExited(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseDragged(MouseEvent e) {}

  @Override
  public void mouseMoved(MouseEvent e) {
    if (flameView.getFlameData() == null) {
      return;
    }
    int panelHeight = dataPanel.getHeight();
    int row = (panelHeight - innerGap - e.getY()) / rowHeight;
    FlameBlock flameBlock = flameView.getFlameData().findFlameBlock(row, e.getX() - innerGap);
    //        System.out.println(e.getX() + " " + e.getY() + " " + row);
    if (flameView.getFlameData().setMouseOverFlameBlock(flameBlock)) {
      String toolTipText = null;
      if (flameBlock != null) {
        toolTipText = flameBlock.getToolTipTextHTML();
      }
      dataPanel.setToolTipText(toolTipText);
      dataPanel.repaint(500);
    }
  }

  /** Called when new experiment loaded */
  protected void initView() {
    synchronized (stateLock) {
      state = State.CLEAR;
      dataPanel.repaint(500);
    }
  }

  /** Called just before everything is recomputed (doCompute) */
  protected void resetView() {
    synchronized (stateLock) {
      state = State.RESET;
      dataPanel.repaint(500);
    }
  }

  /** Called when function colors have changed */
  protected void functionColorsChanged() {
    synchronized (stateLock) {
      if (flameView.getFlameData() != null) {
        flameView.getFlameData().resetBlockColors();
        if (flameView.isSelected()) {
          dataPanel.repaint(500);
        }
      }
    }
  }

  /** Called when a row is complete */
  protected void rowIsComplete(int newRow) {
    //        System.out.println("rowIsComplete " + newRow);
    if (flameView.getFlameData().allRowsDone()) {
      flameView.getFlameData().calculateFlameBlockWidths(dataPanel.getSize().width);
      synchronized (stateLock) {
        state = State.ROWS;
        dataPanel.repaint(500);
      }
    }
  }

  protected int getRowHeight() {
    return rowHeight;
  }

  protected void setZoomLevel(int zoomLevel) {
    int newRowHeight = defaultFontSize + zoomLevel + 4;
    if (newRowHeight != this.rowHeight) {
      this.rowHeight = newRowHeight;
      font = getFont().deriveFont((float) (defaultFontSize + zoomLevel));
      setPanelSize(flameView.getFlameData().getFlameRows().length);
      flameView.scrollToRow(0);
      repaint();
    }
  }

  protected void setPanelSize(int rowCount) {
    int newHeight = rowCount * rowHeight + 2 * outerGap;
    //        System.out.println("setPanelSize " + newHeight);
    setMinimumSize(new Dimension(10, newHeight));
    setPreferredSize(new Dimension(10, newHeight));
    revalidate();
  }

  class DataPanel extends AnGradientPanel {

    private DataPanel() {
      super(AnEnvironment.FLAME_TOP_COLOR, AnEnvironment.FLAME_BOTTOM_COLOR);
    }

    @Override
    public void paint(Graphics g) {
      //            System.out.println("paint() " + getSize());

      super.paint(g); // Paints background

      synchronized (stateLock) {
        switch (state) {
          case CLEAR:
            paintClear(g);
            break;
          case RESET:
            paintClear(g);
            paintReset(g);
            break;
          case ROWS:
            paintClear(g);
            int width = getSize().width;
            FlameData flameData = flameView.getFlameData();
            if (width != lastWidth) {
              flameData.recalculateFlameBlockWidths(width - 2 * innerGap);
            }
            lastWidth = width;
            //                        Date start = new Date();
            paintRows(g, flameData);
            //                        System.out.println("paint() " + (new Date().getTime() -
            // start.getTime()));
            break;
        }
      }
    }

    private void paintClear(Graphics g) {
      // Nothing
    }

    private void paintReset(Graphics g) {
      int width = getSize().width;
      Rectangle ret = getVisibleRect();
      g.setColor(AnEnvironment.FLAME_INFO_TEXT_COLOR);
      Font font = getFont().deriveFont((float) (getFont().getSize() + 3));
      g.setFont(font);
      String msg = AnLocale.getString("Fetching data...");
      int msgWidth = g.getFontMetrics().stringWidth(msg);
      g.drawString(msg, width / 2 - msgWidth / 2, ret.y + ret.height / 2 + 10);
      flameView.updateSelection();
    }

    private void paintNoData(Graphics g) {
      setPanelSize(1);
      int width = getSize().width;
      Rectangle ret = getVisibleRect();
      g.setColor(AnEnvironment.FLAME_INFO_TEXT_COLOR);
      Font font = getFont().deriveFont((float) (getFont().getSize() + 3));
      g.setFont(font);
      String msg =
          String.format(
              AnLocale.getString("Data not available for this metric (%s)"),
              flameView.getFlameData().getAnMetric());
      if (Filters.getInstance().anyFilters()) {
        msg += AnLocale.getString(" and filter selection");
      }
      int msgWidth = g.getFontMetrics().stringWidth(msg);
      g.drawString(msg, width / 2 - msgWidth / 2, ret.y + ret.height / 2 + 10);
      flameView.updateSelection();
    }

    private void paintRows(Graphics g, FlameData flameData) {
      if (flameData == null) {
        paintNoData(g);
        return;
      }
      if (flameData.getFlameRows().length == 0
          || flameData.getFlameRows()[0] == null
          || flameData.getFlameRows()[0].getFlameBlocks().isEmpty()) {
        paintNoData(g);
        return;
      }
      FlameRow[] flameRows = flameData.getFlameRows();
      if (flameRows != null) {
        int width = getSize().width;
        int height = getSize().height;
        Rectangle visibleRectangle = getVisibleRect();
        g.setFont(font);
        for (int row = 0; row < flameRows.length; row++) {
          FlameRow flameRow = flameRows[row];
          g.setClip(visibleRectangle);
          int blockTextYPos = height - (rowHeight * row) - 4 - innerGap;
          if (flameRow != null && flameRow.isReadyToPaint()) {
            int blockYPos = height - (rowHeight * row) - rowHeight - innerGap;
            FlameBlock flameBlock = flameRow.getFirstPaintedFlameBlock();
            while (flameBlock != null) {
              if (flameBlock.paintBlock()) {
                int blockWidth = flameBlock.getXWidth();
                int blockXPos = flameBlock.getXPos() + innerGap;

                if (blockWidth >= 2) {
                  g.setColor(flameBlock.getColor());
                  g.fillRect(blockXPos, blockYPos, blockWidth, rowHeight);
                }
                g.setColor(AnEnvironment.FLAME_BLOCK_BORDER_COLOR);
                g.drawRect(blockXPos, blockYPos, blockWidth, rowHeight);
                if (blockWidth > 6) {
                  Font font = g.getFont();
                  Font boldFont = font.deriveFont(Font.BOLD);
                  g.setColor(Color.BLACK);
                  if (flameBlock == flameData.getBaseFlameBlock()) {
                    g.setFont(boldFont);
                  }
                  g.setClip(blockXPos, visibleRectangle.y, blockWidth, visibleRectangle.height);
                  g.drawString(flameBlock.getFunctionDisplayName(), blockXPos + 2, blockTextYPos);
                  if (flameBlock == flameData.getBaseFlameBlock()) {
                    String valueText = flameBlock.getValueText();
                    g.setFont(boldFont);
                    int valueWidth = g.getFontMetrics().stringWidth(valueText);
                    g.setColor(flameBlock.getColor());
                    g.fillRect(
                        blockXPos + blockWidth - valueWidth - 20, blockYPos, blockWidth, rowHeight);
                    g.setColor(Color.BLACK);
                    g.drawString(valueText, blockWidth - valueWidth, blockTextYPos + 0);
                    g.setFont(font);
                  }
                  g.setFont(font);
                  g.setClip(visibleRectangle);
                }
              }

              flameBlock = flameBlock.getNextFlameBlock(FlameBlock.RIGHT);
            }
          } else if (flameRow != null) {
            g.setColor(Color.GREEN.darker().darker());
            g.drawString(
                String.format("%2s", row) + ": " + AnLocale.getString("Ready..."),
                width / 2 - 12,
                blockTextYPos);
          } else {
            g.setColor(AnEnvironment.FLAME_INFO_TEXT_COLOR);
            g.drawString(
                String.format("%2s", row) + ": " + AnLocale.getString("Fetching data..."),
                width / 2 - 12,
                blockTextYPos);
          }
        }

        FlameBlock selectedFlameBlock = flameData.getSelectedFlameBlock();
        FlameBlock mouseOverFlameBlock = flameData.getMouseOverFlameBlock();
        if (selectedFlameBlock != null) {
          drawXFlameBlock(
              g,
              selectedFlameBlock,
              AnEnvironment.FLAME_BLOCK_SELECTED_BORDER1,
              AnEnvironment.FLAME_BLOCK_SELECTED_BORDER2);
        }
        if (mouseOverFlameBlock != null) {
          drawXFlameBlock(
              g, mouseOverFlameBlock, AnEnvironment.FLAME_BLOCK_MOUSEOVER_BORDER1, null);
        }
      }
    }

    private void drawXFlameBlock(Graphics g, FlameBlock flameBlock, Color color1, Color color2) {
      if (flameBlock.paintBlock()) {
        int height = getSize().height;
        int blockWidth = flameBlock.getXWidth();
        int blockXPos = flameBlock.getXPos() + innerGap;
        int blockYPos =
            height - (rowHeight * flameBlock.getFlameRow().getRow()) - rowHeight - innerGap;
        if (color1 != null) {
          g.setColor(color1);
          g.drawRect(blockXPos, blockYPos, blockWidth, rowHeight);
          g.drawRect(blockXPos - 1, blockYPos - 1, blockWidth + 2, rowHeight + 2);
        }
        if (color2 != null) {
          g.setColor(color2);
          g.drawRect(blockXPos + 1, blockYPos + 1, blockWidth - 2, rowHeight - 2);
        }
      }
    }
  }

  protected static int getOuterGap() {
    return outerGap;
  }

  protected static int getInnerGap() {
    return innerGap;
  }
}

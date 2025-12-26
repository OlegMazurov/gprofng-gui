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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.Vector;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.event.PopupMenuEvent;

/**
 * A component that lets the user navigate between hot lines clicking on an appropriate colored
 * rect. due to rfe #4487681 ()
 */
public final class AnHotGap extends JPanel implements MouseListener, MouseMotionListener {
  // Hot Rect constants

  private AnMenuListener menuListener;
  private AnTable table;
  private final double box_height = 2.; // height of rect
  public static final int HOTGAP_WIDTH = 18;
  private final double box_width = HOTGAP_WIDTH - 6; // width of rect
  private final double height_adjustment = 16; // height of the scrollbare arrow
  private final double handle_adjustment = 8; // half height of the handle
  private final double x_left = (HOTGAP_WIDTH - box_width) / 2 - 1; // left rect indentation
  private Vector<RectData> vect;      // list of hot rects
  private Vector<Integer> hots;       // list of hot lines
  private Vector<Integer> warm_lines; // list of warm lines (a metric value > 0)
  private int warm_lines_count = 0; // count of warm lines
  private boolean show_warm_lines = true;
  private FuncListDisp disp = null; // SRC or DIS display
  private int hots_count = 0; // Count of hot lines
  private int row_count; // count of the table lines
  private int curr_line = 0; // Current line
  private boolean isHotRect = false; // indicates hot rect
  private String text = null; // Tooltip text for hot rect
  private Rectangle2D.Double rect, rect_shadow;
  private Rectangle r;
  private ToolTipManager tm; // Tooltip manager
  private AnTable.FListTableModel model; // table model
  private final BasicStroke stroke = new BasicStroke(BasicStroke.JOIN_ROUND);
  private static final String prefix_msg = AnLocale.getString("Non-Zero-Metric item at: ");
  private final String updateView = AnLocale.getString("Updating the view");

  // Menu actions
  public static final String STR_ACTION_NEXT_HOT_LINE = AnLocale.getString("Next Hot Line");
  public static final String STR_ACTION_NEXT_NZ_LINE =
      AnLocale.getString("Next Non-zero Metric Line");
  public static final String STR_ACTION_PREV_HOT_LINE = AnLocale.getString("Previous Hot Line");
  public static final String STR_ACTION_PREV_NZ_LINE =
      AnLocale.getString("Previous Non-zero Metric Line");
  public static final String STR_ACTION_SHOW_NZ_LINE =
      AnLocale.getString("Show Non-zero Metric Line");
  public static final String STR_ACTION_HIDE_NZ_LINE =
      AnLocale.getString("Hide Non-zero Metric Line");

  /** Creates hot rects panel */
  public AnHotGap(FuncListDisp disp, int headerHeight) {
    init(disp, disp.table, headerHeight);
  }

  public AnHotGap(final FuncListDisp disp, final AnTable table, int headerHeight) {
    init(disp, table, headerHeight);
  }

  private void init(final FuncListDisp disp, AnTable table, int headerHeight) {
    this.disp = disp;
    this.table = table;
    table.gap = this;
    model = table.getTableModel();
    setLayout(new java.awt.GridLayout());
    java.awt.Dimension dim =
        new java.awt.Dimension(HOTGAP_WIDTH, table.getPreferredSize().height - headerHeight);
    setPreferredSize(dim);
    dim = new java.awt.Dimension(HOTGAP_WIDTH, table.getMaximumSize().height - headerHeight);
    setMaximumSize(dim);
    dim = new java.awt.Dimension(HOTGAP_WIDTH, table.getMinimumSize().height - headerHeight);
    setMinimumSize(dim);
    addMouseMotionListener(this);
    addMouseListener(this);
    tm = ToolTipManager.sharedInstance();
    tm.registerComponent(this);
    // Popup menu
    menuListener = new AnMenuListener();
    this.addMouseListener(menuListener);
  }

  /** Updates hot rects panel and sets required flags and variables */
  public void update() {
    synchronized (updateView) {
      row_count = model.getRowCount();
      if (row_count == 0) {
        hots_count = 0;
        repaint();
        return;
      }
      if (show_warm_lines) {
        warm_lines = new Vector<Integer>();

        for (int i = 0; i < row_count; i++) {
          int mcc = model.getColumnCount();
          int j, k = model.getNameCol();
          String s;
          for (j = 0; j < mcc; j++) {
            if (j == k) {
              continue;
            }
            s = model.getValueAt(i, j).toString();
            if (s.matches(".*[1-9].*")) {
              warm_lines.add(i);
              break;
            }
          }
        }
        warm_lines_count = warm_lines.size();
      } else {
        warm_lines_count = 0;
      }

      hots = new Vector<Integer>();

      for (int i = 0; i < row_count; i++) {
        int j;
        if ((j = model.getSrcType(i)) < 0) {
          hots.add(i);
        }
      }

      hots_count = hots.size();

      repaint();
    }
  }

  /** Paints this component. */
  @Override
  protected void paintComponent(final Graphics g) {
    update(g);
  }

  /**
   * Updates this component.
   *
   * @param g the specified context to use for updating
   * @see #paint
   * @see #repaint()
   */
  @Override
  public void update(final Graphics g) {
    synchronized (updateView) {
      double y;
      int line;
      int line_height = 0;
      final Graphics2D g2 = (Graphics2D) g;

      // Clean old markers
      java.awt.Dimension dim = getSize();
      r = new Rectangle(0, 0, dim.height, dim.height);
      g2.setPaint(java.awt.Color.WHITE /* AnVariable.BG_COLOR */);
      g2.fill(r);

      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

      if ((hots_count == 0) && (warm_lines_count == 0)) {
        return;
      }

      double gap_height =
          table.getVerticalScrollBar().getSize().height - 2 * height_adjustment - handle_adjustment;
      double inc = (gap_height / row_count);
      double rowHeight = table.getRowHeight();
      if (inc > rowHeight) {
        gap_height = gap_height * rowHeight / inc;
        inc = rowHeight;
      }

      // Calculate minimum line height
      if (row_count > 0) {
        line_height = (dim.height / row_count);
      }

      vect = new Vector<RectData>(hots_count + warm_lines_count);

      int addRow = 0;

      Rectangle2D.Double cur_rect_warm = null;
      int current_line = table.getSelectedRow();
      for (int i = 0; i < warm_lines_count; i++) {
        line = warm_lines.get(i);
        y = addRow + height_adjustment + inc * line;

        rect = new java.awt.geom.Rectangle2D.Double(x_left, y, box_width, box_height);
        g2.setPaint(AnVariable.WARMLINE_COLOR);
        g2.setStroke(stroke);
        g2.draw(rect);
        g2.fill(rect);

        r = rect.getBounds();
        r = new Rectangle(r.x, r.y, r.width + 2, r.height + 1);

        vect.add(new RectData(r, line));

        // Highlight current line
        if (line == current_line) {
          // rect = new java.awt.geom.Rectangle2D.Double(x_left-4, y, box_width/2+4, box_height+2);
          rect_shadow =
              new java.awt.geom.Rectangle2D.Double(
                  x_left - 1, y - 1, box_width + 2, box_height + 2);
          cur_rect_warm = rect;
        }
      }

      Rectangle2D.Double cur_rect_hot = null;
      for (int i = 0; i < hots_count; i++) {
        line = hots.get(i);
        y = addRow + height_adjustment + inc * line;

        rect = new java.awt.geom.Rectangle2D.Double(x_left, y, box_width, box_height);
        g2.setPaint(AnVariable.HOTLINE_COLOR);
        g2.setStroke(stroke);
        g2.draw(rect);
        g2.fill(rect);

        r = rect.getBounds();
        r = new Rectangle(r.x, r.y, r.width + 2, r.height + 1);

        vect.add(new RectData(r, line)); // , model.getSrcLine(line)));
        // Highlight current line
        if (line == current_line) {
          // rect = new java.awt.geom.Rectangle2D.Double(x_left-4, y, box_width/2+4, box_height+2);
          rect_shadow =
              new java.awt.geom.Rectangle2D.Double(
                  x_left - 1, y - 1, box_width + 2, box_height + 2);
          cur_rect_hot = rect;
        }
      }
      if (cur_rect_warm != null) {
        g2.setPaint(Color.BLACK);
        g2.draw(rect_shadow);
        g2.fill(rect_shadow);
        g2.setPaint(AnVariable.WARMLINE_COLOR);
        g2.setStroke(stroke);
        g2.draw(cur_rect_warm);
        g2.fill(cur_rect_warm);
      }
      if (cur_rect_hot != null) {
        g2.setPaint(Color.BLACK);
        g2.draw(rect_shadow);
        g2.fill(rect_shadow);
        g2.setPaint(AnVariable.HOTLINE_COLOR);
        g2.setStroke(stroke);
        g2.draw(cur_rect_hot);
        g2.fill(cur_rect_hot);
      }
    }
  }

  private void goToLine(int line) {
    disp.goToLine(table, line);
    update();
  }

  /**
   * Returns hot line for the current hot rect
   *
   * @return current hot line
   */
  private int getLine() {
    return curr_line;
  }

  /**
   * Checks selected point if that is from hot rect and sets approproiate flags and vars
   *
   * @param p point being checked
   * @return <code>true</code> if point belongs hot rect
   */
  private boolean checkRect(final Point p) {
    isHotRect = false;
    curr_line = 0;
    final int size = vect.size();
    if (size == 0) {
      return false;
    }

    RectData data;
    Vector<Integer> candidates = new Vector<Integer>();
    for (int i = 0; i < size; i++) {
      data = vect.get(i);
      if (data.getRect().contains(p)) {
        candidates.add(i);
      }
    }
    for (int i = 0; i < candidates.size(); i++) {
      data = vect.get(candidates.get(i));
      for (int j = 0; j < hots_count; j++) {
        int line = hots.get(j);
        if (line == data.getLine()) {
          setText(data.getHint());
          isHotRect = true;
          curr_line = data.getLine();
          return true;
        }
      }
    }
    for (int i = 0; i < candidates.size(); i++) {
      data = vect.get(candidates.get(i));
      setText(data.getHint());
      isHotRect = true;
      curr_line = data.getLine();
      return true;
    }
    return false;
  }

  /**
   * Sets tooltip text for hot rects
   *
   * @param str tooltip text
   */
  private void setText(final String str) {
    text = str;
  }

  /**
   * Returns tooltip text for hot rect (overrides method in <code>javax.swing.JComponent</code>)
   *
   * @param event Mouse event
   */
  @Override
  public String getToolTipText(final MouseEvent event) {
    return text;
  }

  /**
   * Returns tooltip location for hot rect (overrides method in <code>javax.swing.JComponent</code>)
   *
   * @param event Mouse event
   */
  @Override
  public Point getToolTipLocation(final MouseEvent event) {
    final Point p = event.getPoint();
    if (getSize().getHeight() - p.getY() <= 25) {
      p.setLocation(0, p.y - (20 + box_height));
    } else {
      p.setLocation(0, p.y + 15);
    }
    return p;
  }

  // This methods are required by MouseMotionListener.
  public void mouseMoved(final MouseEvent e) {
    synchronized (updateView) {
      final Point mp = e.getPoint();
      if (vect == null) {
        return;
      }
      if (checkRect(mp)) {
        tm.setEnabled(true);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      } else {
        tm.setEnabled(false);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }

  public void mouseDragged(final MouseEvent e) {}

  // This methods are required by MouseListener.
  public void mouseClicked(final MouseEvent e) {
    if (!isHotRect) {
      return;
    }
    goToLine(getLine());
  }

  public void mouseExited(final MouseEvent e) {
    tm.setEnabled(true);
  }

  public void mouseEntered(final MouseEvent e) {}

  public void mousePressed(final MouseEvent e) {}

  public void mouseReleased(final MouseEvent e) {}

  /** Inner class to represent hot rect data */
  private final class RectData {

    private final Rectangle r;
    private final int line;

    /**
     * Constructor of hot rect data
     *
     * @param r hot rectangle
     * @param line hot line
     */
    public RectData(final Rectangle r, final int line /*, final String hint*/) {
      this.r = r;
      this.line = line;
    }

    public Rectangle getRect() {
      return r;
    }

    public String getHint() {
      return prefix_msg + model.getSrcLine(line);
    }

    public int getLine() {
      return line;
    }
  }

  /**
   * Navigate in Hot Gap Possible actions: Next Hot Line - go to next Hot Line Next Non-zero Metric
   * Line - go to next Non-zero Metric Line Previous Hot Line - go to Previous Hot Line Previous
   * Non-zero Metric Line - got to Previous Non-zero Metric Line
   */
  private void HG_Action(String actionName, ActionEvent ev) {
    if (actionName.equals(STR_ACTION_NEXT_HOT_LINE)) {
      HG_Next_Hot_Line();
    }
    if (actionName.equals(STR_ACTION_NEXT_NZ_LINE)) {
      HG_Next_NZ_Line();
    }
    if (actionName.equals(STR_ACTION_PREV_HOT_LINE)) {
      HG_Prev_Hot_Line();
    }
    if (actionName.equals(STR_ACTION_PREV_NZ_LINE)) {
      HG_Prev_NZ_Line();
    }
    if (actionName.equals(STR_ACTION_SHOW_NZ_LINE)) {
      show_warm_lines = true;
      update();
    }
    if (actionName.equals(STR_ACTION_HIDE_NZ_LINE)) {
      show_warm_lines = false;
      update();
    }
  }

  /** Navigate to Next Hot Line */
  public void HG_Next_Hot_Line() {
    int current_line = table.getSelectedRow();
    for (int i = 0; i < hots_count; i++) {
      int line = hots.get(i);
      if (current_line >= line) {
        continue;
      }
      goToLine(line);
      repaint();
      break;
    }
  }

  /** Navigate to Previous Hot Line */
  public void HG_Prev_Hot_Line() {
    int current_line = table.getSelectedRow();
    for (int i = hots_count - 1; i >= 0; i--) {
      int line = hots.get(i);
      if (current_line <= line) {
        continue;
      }
      goToLine(line);
      repaint();
      break;
    }
  }

  /** Navigate to Next Non-Zero Metric Line */
  public void HG_Next_NZ_Line() {
    int next_hot_line = -1;
    int next_nz_line = -1;
    int current_line = table.getSelectedRow();
    for (int i = 0; i < hots_count; i++) {
      int line = hots.get(i);
      if (current_line >= line) {
        continue;
      }
      next_hot_line = line;
      break;
    }
    for (int i = 0; i < warm_lines_count; i++) {
      int line = warm_lines.get(i);
      if (current_line >= line) {
        continue;
      }
      next_nz_line = line;
      break;
    }
    if ((next_hot_line < 0) && (next_nz_line < 0)) {
      return;
    }
    if (next_hot_line < 0) {
      next_hot_line = next_nz_line;
    }
    if (next_nz_line < 0) {
      next_nz_line = next_hot_line;
    }
    if (next_hot_line < next_nz_line) {
      next_nz_line = next_hot_line;
    }
    goToLine(next_nz_line);
    repaint();
  }

  /** Navigate to Previous Non-Zero Metric Line */
  public void HG_Prev_NZ_Line() {
    int next_hot_line = -1;
    int next_nz_line = -1;
    int current_line = table.getSelectedRow();
    for (int i = hots_count - 1; i >= 0; i--) {
      int line = hots.get(i);
      if (current_line <= line) {
        continue;
      }
      next_hot_line = line;
      break;
    }
    for (int i = warm_lines_count - 1; i >= 0; i--) {
      int line = warm_lines.get(i);
      if (current_line <= line) {
        continue;
      }
      next_nz_line = line;
      break;
    }
    if ((next_hot_line < 0) && (next_nz_line < 0)) {
      return;
    }
    if (next_hot_line < 0) {
      next_hot_line = next_nz_line;
    }
    if (next_nz_line < 0) {
      next_nz_line = next_hot_line;
    }
    if (next_hot_line > next_nz_line) {
      next_nz_line = next_hot_line;
    }
    goToLine(next_nz_line);
    repaint();
  }

  /** Is navigation to Next Hot Metric Line available? */
  public boolean HG_Is_Next_Hot_Line() {
    int current_line = table.getSelectedRow();
    for (int i = 0; i < hots_count; i++) {
      int line = hots.get(i);
      if (current_line >= line) {
        continue;
      }
      return true;
    }
    return false;
  }

  /** Is navigation to Previous Hot Metric Line available? */
  public boolean HG_Is_Prev_Hot_Line() {
    int current_line = table.getSelectedRow();
    for (int i = hots_count - 1; i >= 0; i--) {
      int line = hots.get(i);
      if (current_line <= line) {
        continue;
      }
      return true;
    }
    return false;
  }

  /** Is navigation to Next Non-Zero Metric Line available? */
  public boolean HG_Is_Next_NZ_Line() {
    int next_hot_line = -1;
    int next_nz_line = -1;
    if (!show_warm_lines) {
      return false;
    }
    int current_line = table.getSelectedRow();
    for (int i = 0; i < hots_count; i++) {
      int line = hots.get(i);
      if (current_line >= line) {
        continue;
      }
      next_hot_line = line;
      break;
    }
    for (int i = 0; i < warm_lines_count; i++) {
      int line = warm_lines.get(i);
      if (current_line >= line) {
        continue;
      }
      next_nz_line = line;
      break;
    }
    if ((next_hot_line < 0) && (next_nz_line < 0)) {
      return false;
    }
    return true;
  }

  /** Is navigation to Previous Non-Zero Metric Line available? */
  public boolean HG_Is_Prev_NZ_Line() {
    int next_hot_line = -1;
    int next_nz_line = -1;
    if (!show_warm_lines) {
      return false;
    }
    int current_line = table.getSelectedRow();
    for (int i = hots_count - 1; i >= 0; i--) {
      int line = hots.get(i);
      if (current_line <= line) {
        continue;
      }
      next_hot_line = line;
      break;
    }
    for (int i = warm_lines_count - 1; i >= 0; i--) {
      int line = warm_lines.get(i);
      if (current_line <= line) {
        continue;
      }
      next_nz_line = line;
      break;
    }
    if ((next_hot_line < 0) && (next_nz_line < 0)) {
      return false;
    }
    return true;
  }

  // ------- Private classes to implement popup menu items ------- //
  private class AnMenuListener extends MouseAdapter {

    private boolean debug;

    AnMenuListener() {
      debug = false;
    }

    public JPopupMenu initPopup(MouseEvent event) {
      return init_popup();
    }

    public JPopupMenu init_popup() {
      AccessibleContext ac;
      JMenuItem mi;
      JPopupMenu popup = new JPopupMenu();
      String txt;

      // Add "Previous Hot Line" action
      txt = STR_ACTION_PREV_HOT_LINE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(HG_Is_Prev_Hot_Line());
      popup.add(mi);

      // Add "Next Hot Line" action
      txt = STR_ACTION_NEXT_HOT_LINE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(HG_Is_Next_Hot_Line());
      popup.add(mi);

      // Add separator
      popup.addSeparator();

      // Add "Previous Non-zero Metric Line" action
      txt = STR_ACTION_PREV_NZ_LINE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(HG_Is_Prev_NZ_Line());
      popup.add(mi);

      // Add "Next Non-zero Metric Line" action
      txt = STR_ACTION_NEXT_NZ_LINE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(HG_Is_Next_NZ_Line());
      popup.add(mi);

      // Add separator
      popup.addSeparator();

      // Add "Show Non-zero Metric Line" action
      txt = STR_ACTION_SHOW_NZ_LINE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(!show_warm_lines);
      popup.add(mi);

      // Add "Hide Non-zero Metric Line" action
      txt = STR_ACTION_HIDE_NZ_LINE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(show_warm_lines);
      popup.add(mi);
      return popup;
    }

    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        JPopupMenu popup = initPopup(e);
        if (popup != null) {
          popup.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuWillBecomeInvisible(" + e + ")");
      }
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuWillBecomeVisible(" + e + ")");
      }
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuCanceled(" + e + ")");
      }
    }
  }

  /** Generic action for context menu items. Action name is passed as String. */
  class UpdateAction extends AbstractAction {

    String actionName = null;

    public UpdateAction(String txt) {
      super(txt);
      actionName = txt;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      HG_Action(actionName, ev);
    }
  }
}

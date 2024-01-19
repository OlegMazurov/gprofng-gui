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

package org.gprofng.mpmt.filter;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.ipc.IPCCancelledException;
import org.gprofng.mpmt.ipc.IPCContext;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

public final class Filters {

  private static Filters instance = null;
  private Frame frame;
  private AnWindow window;
  private CustomFilterDialog customFilterDialog;
  // States
  private Stack<FilterClause> clausesStack = new Stack<FilterClause>();
  private Stack<FilterClause> clausesUndoStack = new Stack<FilterClause>();
  private Stack<FilterClause> clausesRedoStack = new Stack<FilterClause>();
  // Listeners
  private List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

  // Constructor
  public Filters(final AnWindow window, final Frame frame) {
    this.window = window;
    this.frame = frame;
    instance = this;
  }

  public static Filters getInstance() {
    return instance;
  }

  public CustomFilterDialog getCustomFilterDialog() {
    if (customFilterDialog == null) {
      customFilterDialog = new CustomFilterDialog(frame, window);
    }
    return customFilterDialog;
  }

  public void showCustomFilterDialog() {
    getCustomFilterDialog().setFilter(getStandardFilters(true));
    getCustomFilterDialog().setVisible(true);
  }

  private void invalidFilterDialog(String filter) {
    String filterTxt = filter;
    if (filter.length() > 200) {
      filterTxt = filter.subSequence(0, 199) + "...";
    }
    String errmsg = AnLocale.getString("Invalid filter:") + "\n" + filterTxt;
    AnUtility.showMessage(frame, errmsg, JOptionPane.ERROR_MESSAGE);
  }

  protected String trim(String filter) {
    filter = filter.replaceAll("//.*\n", " ");
    filter = filter.replaceAll("\n", " ");
    filter = filter.replaceAll(" +", " ");
    filter = filter.trim();
    return filter;
  }

  /** Reset All filters from context menus */
  public void resetAllFiltersAction() {
    //        System.out.println("Filters:resetAllFiltersAction");
    if (!isFactorySetting()) {
      clausesStack.removeAllElements();
      clausesUndoStack.removeAllElements();
      clausesRedoStack.removeAllElements();
      apply(false);
    }
  }

  /**
   * Returns whether the current state of filters are the default (factory setting)
   *
   * @return true if filter state is default (factory setting)
   */
  private boolean isFactorySetting() {
    boolean val;
    val = clausesStack.isEmpty() && clausesUndoStack.isEmpty() && clausesRedoStack.isEmpty();
    return val;
  }

  public List<FilterClause> getFilters() {
    List<FilterClause> list1 = new ArrayList<FilterClause>();
    Iterator<FilterClause> iterator = clausesStack.iterator();
    while (iterator.hasNext()) {
      FilterClause clause = iterator.next();
      if (clause.isEnabled()) {
        if (clause.isStandardFilter()) {
          list1.add(clause);
        } else if (clause.isCustomFilter()) {
          list1 = new ArrayList<FilterClause>(); // reset list
          list1.add(clause);
        } else if (clause.isNoFiltersFilter()) {
          list1 = new ArrayList<FilterClause>(); // reset list
        } else {
          assert true;
        }
      }
    }
    List<FilterClause> list2 = new ArrayList<FilterClause>();
    for (int n = list1.size() - 1; n >= 0; n--) {
      list2.add(list1.get(n));
    }
    return list2;
  }

  public boolean anyFilters() {
    return !getFilters().isEmpty();
  }

  public void removeClause(FilterClause clause) {
    //        int i = clausesUndoStack.search(clause);
    //        if (i > 0) {
    //            clausesUndoStack.remove(clausesUndoStack.size() - i);
    //        }
    clause.setEnabled(false);
    clausesUndoStack.push(clause);
    apply(false);
  }

  /** Undoes the last filter operation back to last applied */
  public void undoLastFilteraction() {
    if (clausesUndoStack.size() > 0) {
      FilterClause clause = clausesUndoStack.pop();
      clause.setEnabled(!clause.isEnabled());
      clausesRedoStack.push(clause);
    }
    apply(false);
  }

  /**
   * Returns whether there are any filters to undo
   *
   * @return true if there are more filters to undo
   */
  public boolean canUndoLastFilter() {
    return clausesUndoStack.size() > 0;
  }

  /** Undoes the last filter operation back to last applied */
  public void redoLastFilteraction() {
    if (clausesRedoStack.size() > 0) {
      FilterClause clause = clausesRedoStack.pop();
      clause.setEnabled(!clause.isEnabled());
      clausesUndoStack.push(clause);
    }
    apply(false);
  }

  /**
   * Returns whether there are any filters to redo
   *
   * @return true if there are more filters to redo
   */
  public boolean canRedoLastFilter() {
    //        return redoFilters.size() > 0;
    return clausesRedoStack.size() > 0;
  }

  public boolean canRemoveAllFilters() {
    return !getFilters().isEmpty();
  }

  private String getStandardFilters(boolean insertNL) {
    StringBuilder sb = new StringBuilder();

    List<FilterClause> enabledFilters = new ArrayList<FilterClause>();
    for (FilterClause clause : clausesStack) {
      if (clause.isEnabled()) {
        if (clause.isStandardFilter()) {
          enabledFilters.add(clause);
        } else if (clause.isCustomFilter()) {
          enabledFilters = new ArrayList<FilterClause>(); // reset list
          enabledFilters.add(clause);
        } else if (clause.isNoFiltersFilter()) {
          enabledFilters = new ArrayList<FilterClause>(); // reset list
        } else {
          assert true;
        }
      }
    }

    int n = 0;
    for (FilterClause fcl : enabledFilters) {
      if (fcl.isEnabled()) {
        sb.append(fcl.getClause());
        if (!fcl.isCustomFilter() && insertNL) {
          sb.append("        // ");
          sb.append(fcl.getShortNameWithSerialNumber());
          sb.append("\n");
        }
        if (n < (enabledFilters.size() - 1)) {
          if (!fcl.isCustomFilter() && insertNL) {
            sb.append("&&\n");
          } else {
            sb.append(" && ");
          }
        }
      }
      n++;
    }
    return sb.toString();
  }

  protected void apply(final boolean showTooltip) {
    if (SwingUtilities.isEventDispatchThread()) {
      AnUtility.dispatchOnAWorkerThread(
          new Runnable() {
            @Override
            public void run() {
              setFilter(showTooltip);
            }
          },
          "Filter Worker Thread");
    } else {
      setFilter(showTooltip);
    }
  }

  /** Construct a complete filter based on settings in all three panels and apply */
  private void setFilter(boolean showTooltip) {
    AnUtility.checkIfOnAWTThread(false);
    String filterString = trim(getStandardFilters(false));
    if (filterString.length() == 0) {
      filterString = "1";
    }

    boolean valid = validateFilterExpression(filterString); // IPC
    if (!valid) {
      invalidFilterDialog(filterString);
      return;
    }
    // Apply the  filter
    setFilter(filterString.toString().replace("\n", " "), showTooltip);

    return;
  }

  /**
   * Set filter
   *
   * @param filtertext
   * @param showTooltip
   */
  private void setFilter(final String filtertext, final boolean showTooltip) {
    AnUtility.checkIfOnAWTThread(false);
    if (System.getenv("SP_ANALYZER_DUMP_FILTER") != null) {
      System.err.println("setFilterStr:   w_id=" + 0 + "   filtertext='" + filtertext + "'");
    }
    synchronized (AnVariable.mainFlowLock) {
      IPCContext.newCurrentContext(
          AnLocale.getString("Setting filter..."),
          IPCContext.Scope.SESSION,
          false,
          AnWindow.getInstance());
      Object progressBarHandle =
          AnWindow.getInstance()
              .getSystemProgressPanel()
              .progressBarStart(AnLocale.getString("Setting Filter"));
      String msg = null;
      try {
        msg = setFilterStr(0, filtertext); // IPC
      } catch (IPCCancelledException ex) {
        System.out.println("\nfilter cancelled...");
      }
      final String message = msg;
      AnUtility.dispatchOnSwingThread(
          new Runnable() {
            @Override
            public void run() {
              if (message != null) { // Filter setting failed
                String errmsg = AnLocale.getString("Error: ") + message;
                AnUtility.showMessage(frame, errmsg, JOptionPane.ERROR_MESSAGE);
              } else {
                AnEventManager.getInstance()
                    .fireAnChangeEvent(
                        new AnChangeEvent(showTooltip, AnChangeEvent.Type.FILTER_CHANGING));
                AnEventManager.getInstance()
                    .fireAnChangeEvent(
                        new AnChangeEvent(showTooltip, AnChangeEvent.Type.FILTER_CHANGED));
              }
            }
          });
      AnWindow.getInstance().getSystemProgressPanel().progressBarStop(progressBarHandle);
      IPCContext.newCurrentContext(null, IPCContext.Scope.SESSION, false, AnWindow.getInstance());
    }
  }

  /**
   * Setting clause
   *
   * @param clause the filter clause
   */
  public void addClause(
      String shortName, String longName, String clauseText, FilterClause.Kind kind) {
    if (clauseText == null) {
      return;
    }
    //        System.out.println("FilterSelector:setClause: " + clause);
    FilterClause cl = new FilterClause(shortName, longName, clauseText, kind);
    addClause(cl, true);
  }

  public void addClause(FilterClause cl, boolean showTooltip) {
    clausesStack.push(cl);
    clausesUndoStack.push(cl);
    clausesRedoStack.removeAllElements();
    apply(showTooltip);
  }

  public JMenu removeFilterMenuItem() {
    JMenu removeMenu = new JMenu(AnLocale.getString("Remove Filter"));
    List<FilterClause> filters = getFilters();
    for (FilterClause filterClause : filters) {
      JMenuItem mi = new JMenuItem();
      mi.setText(filterClause.getShortNameWithSerialNumber());
      removeMenu.add(mi);
      final FilterClause fc = filterClause;
      mi.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              removeClause(fc);
            }
          });
    }
    return removeMenu;
  }

  private String setFilterStr(final int win_id, final String filter) {
    synchronized (IPC.lock) {
      window.IPC().send("setFilterStr");
      window.IPC().send(win_id);
      window.IPC().send(filter);
      String s = window.IPC().recvString();
      lastFilterStr = null;
      lastFilterStr = getFilterStr(win_id); // Optimization
      return s;
    }
  }

  private static String lastFilterStr = null; // Optimization. Important for context menu.

  public String getFilterStr(final int win_id) {
    if (null != lastFilterStr) {
      return lastFilterStr; // Optimization
    }
    synchronized (IPC.lock) {
      window.IPC().send("getFilterStr");
      window.IPC().send(win_id);
      lastFilterStr = window.IPC().recvString(); // Optimization
      return lastFilterStr;
    }
  }

  public boolean validateFilterExpression(final String str_expr) {
    synchronized (IPC.lock) {
      window.IPC().send("validateFilterExpression");
      window.IPC().send(str_expr);
      int val = window.IPC().recvInt();
      return val != 0;
    }
  }

  public Object[] getFilterKeywords() {
    synchronized (IPC.lock) {
      window.IPC().send("getFilterKeywords");
      window.IPC().send(0);
      Object[] obj = (Object[]) window.IPC().recvObject();
      return obj;
    }
  }
}

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


package org.gprofng.mpmt.statecolors;

import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.filter.FilterClause;
import org.gprofng.mpmt.settings.ViewModeSetting;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

// Stack displayed as Colored boxes labeled with PC names
public class StackView extends JList {
  protected final StateColorMap color_map;
  private String addFilterString = AnLocale.getString("Add Filter: ");
  private String viewName = AnLocale.getString("Call Stack");
  protected long stackId = -1;

  private final String exclude_PC_stack =
      AnLocale.getString("Include only events not containing the current PC stack");
  private final String include_PC_stack =
      AnLocale.getString("Include only events containing the current PC stack");
  private final String filter_exclude_PC_stack = addFilterString + exclude_PC_stack;
  private final String filter_include_PC_stack = addFilterString + include_PC_stack;
  private final String exclude_function_stack =
      AnLocale.getString("Include only events not containing the current function stack");
  private final String include_function_stack =
      AnLocale.getString("Include only events containing the current function stack");
  private final String filter_exclude_function_stack = addFilterString + exclude_function_stack;
  private final String filter_include_function_stack = addFilterString + include_function_stack;

  // Constructor
  public StackView(final StateColorMap color_map) {
    this.color_map = color_map;
    AnUtility.setAccessibleContext(getAccessibleContext(), AnLocale.getString("Stack View"));

    ListRenderer lrend;
    lrend = new ListRenderer();
    setCellRenderer(lrend);
    //	setBackground(lrend.getBackground());

    // XXXmpview can this be done later, like in setStates?
    Font font = new Font("Monospaced", Font.PLAIN, getFont().getSize());
    setFont(font);
    setFixedCellHeight(font.getSize() < 17 ? 17 : font.getSize());

    KeyStroke keyStroke = KeyboardShortcuts.contextMenuActionShortcut;
    getInputMap().put(keyStroke, keyStroke);
    getActionMap()
        .put(
            keyStroke,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                StackViewPopupListener popupListener =
                    new StackViewPopupListener(AnWindow.getInstance());
                popupListener.showPopup(StackView.this, 0, 0);
              }
            });

    addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
              AnWindow.getInstance().setSelectedView(AnDisplay.DSP_Source);
            }
          }
        });

    setSelectionMode(
        ListSelectionModel.SINGLE_SELECTION); // Allow only single line selection for now.
  }

  public void setStackId(final long id) {
    this.stackId = id;
  }

  // Set components
  public void setStates(Vector<StackState> sv_state, int index) {
    int size = sv_state.size();

    setListData(sv_state);

    if (size == 0) {
      return;
    }
    // Set selected function
    if (index == -1) {
      clearSelection();
      ensureIndexIsVisible(0);
    } else {
      index = Math.max(index, 0);
      index = Math.min(index, size - 1);
      setSelectedIndex(index);
      ensureIndexIsVisible(index);
    }
  }

  // Clear list
  public void reset() {
    setListData(new Vector<StackState>());
  }

  // Set selected function
  public StackState getSelectedState() {
    return (StackState) getSelectedValue();
  }

  // Set selected function
  public void setSelectedFunction(long func) {
    StackState state;
    ListModel data = getModel();

    if (data == null) return;

    state = (StackState) getSelectedValue();
    if ((state != null) && (state.getNumber() == func)) return;

    clearSelection();

    int ii;

    for (ii = 0; ii < data.getSize(); ii++) {
      state = (StackState) data.getElementAt(ii);
      if (state.getNumber() == func) {
        setSelectedValue(state, true); // could do a hashmap
        break;
      }
    }
    // repaint();
  }

  // List renderer
  protected class ListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
        JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      setText(value.toString());
      StackState svstate = (StackState) value;
      Color color = color_map.getFuncColor(svstate.getNumber());

      setIcon(new ImageIcon(StackState.createContigIcon(color)));
      setBorder(null);

      return this;
    }
  }

  public class StackViewPopupListener extends MouseAdapter implements PopupMenuListener {

    private AnWindow window;

    public StackViewPopupListener(AnWindow w) {
      window = w;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    // public void mouseReleased(MouseEvent e) {
    //    maybeShowPopup(e);
    // }
    public void maybeShowPopup(MouseEvent e) {
      showPopup(e.getComponent(), e.getX(), e.getY());
    }

    public void showPopup(Component component, int x, int y) {
      JPopupMenu popup = createStackViewPopupMenu();
      popup.show(component, x, y);
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {}

    class ShowSource extends AbstractAction {

      public ShowSource() {
        super(AnLocale.getString("Show Source"));
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        window.setSelectedView(AnDisplay.DSP_Source);
      }
    }

    class ShowDisasm extends AbstractAction {

      public ShowDisasm() {
        super(AnLocale.getString("Show Disassembly"));
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        window.setSelectedView(AnDisplay.DSP_Disassembly);
      }
    }

    class FilterByStackId extends AbstractAction {

      private boolean isExclude;

      public FilterByStackId(boolean isE) {
        super(isE ? filter_exclude_PC_stack : filter_include_PC_stack);
        isExclude = isE;
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        String clause = "(";
        clause += stackId;
        clause += ")";
        String stack = "STACKID";
        clause = "(" + clause + "==" + stack + ")";
        if (isExclude) {
          clause = "(!" + clause + ")";
        }
        String shortName = viewName + ": " + AnLocale.getString("Current PC Stack");
        String longName;
        if (isExclude) {
          longName = filter_exclude_PC_stack;
        } else {
          longName = filter_include_PC_stack;
        }
        window
            .getFilters()
            .addClause(
                shortName,
                longName,
                clause,
                FilterClause.Kind.STANDARD); // Put the string in text field
      }
    }

    class FilterByStack extends AbstractAction {

      private boolean isExclude;

      public FilterByStack(boolean isE) {
        super(isE ? filter_exclude_function_stack : filter_include_function_stack);
        isExclude = isE;
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        ListModel model = getModel();

        long[] funcs = new long[model.getSize()];
        for (int i = 0; i < model.getSize(); i++) {
          StackState state = (StackState) model.getElementAt(i);
          funcs[model.getSize() - i - 1] = state.getNumber();
        }
        long[] funcIds = window.getFuncIds(funcs);
        String clause = "(";
        for (int i = 0; i < funcIds.length; i++) {
          clause += funcIds[i];
          if (i != funcIds.length - 1) {
            clause += ",";
          }
        }
        clause += ")";
        String stack = "USTACK";
        if (window.getSettings().getViewModeSetting().get() == ViewModeSetting.ViewMode.MACHINE) {
          stack = "MSTACK";
        }
        if (window.getSettings().getViewModeSetting().get() == ViewModeSetting.ViewMode.EXPERT) {
          stack = "XSTACK";
        }
        clause = "(" + clause + " ORDERED IN " + stack + ")";
        if (isExclude) {
          clause = "(!" + clause + ")";
        }
        String shortName = viewName + ": " + AnLocale.getString("Current Function Stack");
        String longName;
        if (isExclude) {
          longName = filter_exclude_function_stack;
        } else {
          longName = filter_include_function_stack;
        }
        window
            .getFilters()
            .addClause(
                shortName,
                longName,
                clause,
                FilterClause.Kind.STANDARD); // Put the string in text field
      }
    }

    class FilterByFunction extends AbstractAction {

      public FilterByFunction() {
        super(
            AnLocale.getString(
                addFilterString + "Include only stacks containing the selected function"));
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        long ID = window.getSelectedObject().getSelObjV2("FUNCTION");
        String clause = "(" + ID + ")";
        String stack = "USTACK";
        if (window.getSettings().getViewModeSetting().get() == ViewModeSetting.ViewMode.MACHINE) {
          stack = "MSTACK";
        }
        if (window.getSettings().getViewModeSetting().get() == ViewModeSetting.ViewMode.EXPERT) {
          stack = "XSTACK";
        }
        clause = "(" + clause + " IN " + stack + ")";
        String shortName = viewName + ": " + AnLocale.getString("Selected Function");
        String longName =
            AnLocale.getString("Include only stacks containing the selected function");
        window
            .getFilters()
            .addClause(
                shortName,
                longName,
                clause,
                FilterClause.Kind.STANDARD); // Put the string in text field
      }
    }

    class FilterByPC extends AbstractAction {

      public FilterByPC() {
        super(
            AnLocale.getString(addFilterString + "Include only stacks containing the selected PC"));
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        long LID = window.getSelectedObject().getSelObjV2("INSTRUCTION");
        String clause = "(" + LID + ")";
        String stack = "USTACK";
        if (window.getSettings().getViewModeSetting().get() == ViewModeSetting.ViewMode.MACHINE) {
          stack = "MSTACK";
        }
        if (window.getSettings().getViewModeSetting().get() == ViewModeSetting.ViewMode.EXPERT) {
          stack = "XSTACK";
        }
        clause = "(" + clause + " IN " + stack + "I)";
        String shortName = viewName + ": " + AnLocale.getString("Selected PC");
        String longName = AnLocale.getString("Include only stacks containing the selected PC");
        window
            .getFilters()
            .addClause(
                shortName,
                longName,
                clause,
                FilterClause.Kind.STANDARD); // Put the string in text field
      }
    }

    class RemoveLastFilter extends AbstractAction {

      public RemoveLastFilter() {
        super(AnLocale.getString("Undo Last Filter"));
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        window.getFilters().undoLastFilteraction();
      }
    }

    class RedoLastFilter extends AbstractAction {

      public RedoLastFilter() {
        super(AnLocale.getString("Redo Filter"));
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        window.getFilters().redoLastFilteraction();
      }
    }

    class RemoveAllFilter extends AbstractAction {

      public RemoveAllFilter() {
        super(AnLocale.getString("Remove All Filters"));
      }

      @Override
      public void actionPerformed(ActionEvent ev) {
        window.getFilters().addClause(FilterClause.getNoFiltersClause(), false);
      }
    }

    private JPopupMenu createStackViewPopupMenu() {
      JMenuItem menuItem;
      JPopupMenu popup = new JPopupMenu();

      menuItem = new JMenuItem(new ShowSource());
      popup.add(menuItem);

      menuItem = new JMenuItem(new ShowDisasm());
      popup.add(menuItem);
      popup.addSeparator();

      menuItem = new JMenuItem(new FilterByStack(false));
      popup.add(menuItem);

      menuItem = new JMenuItem(new FilterByStack(true));
      popup.add(menuItem);

      if (stackId != -1) {
        menuItem = new JMenuItem(new FilterByStackId(false));
        popup.add(menuItem);

        menuItem = new JMenuItem(new FilterByStackId(true));
        popup.add(menuItem);
      }

      menuItem = new JMenuItem(new FilterByFunction());
      popup.add(menuItem);

      menuItem = new JMenuItem(new FilterByPC());
      popup.add(menuItem);

      menuItem = new JMenuItem(new RemoveLastFilter());
      menuItem.setEnabled(window.getFilters().canUndoLastFilter());
      popup.add(menuItem);

      menuItem = new JMenuItem(new RedoLastFilter());
      menuItem.setEnabled(window.getFilters().canRedoLastFilter());
      popup.add(menuItem);

      menuItem = new JMenuItem(new RemoveAllFilter());
      menuItem.setEnabled(window.getFilters().canRemoveAllFilters());
      popup.add(menuItem);

      return popup;
    }
  }
}

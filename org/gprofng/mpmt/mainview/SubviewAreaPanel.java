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

package org.gprofng.mpmt.mainview;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.CalledByCallsDisp;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.mainview.MainViewPanel.SubviewArea;
import org.gprofng.mpmt.util.gui.AnTabbedPane;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class SubviewAreaPanel extends BorderPanel {

  private final SubviewArea subviewArea;
  private final AnTabbedPane tabbedPane;

  protected SubviewAreaPanel(SubviewArea subviewArea) {
    this.subviewArea = subviewArea;
    tabbedPane = new AnTabbedPane();
    tabbedPane.setBorder(null);
    add(tabbedPane);
    setBackground(AnEnvironment.SUBVIEW_PANEL_BACKGROUND);

    // Focus keyboard shortcuts
    if (subviewArea == SubviewArea.SUBVIEW_AREA_1) {
      KeyStroke keyStroke = KeyboardShortcuts.subviewArea1FocusShortcut;
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, keyStroke);
      getActionMap().put(keyStroke, new FocusDefaultAction());
    } else if (subviewArea == SubviewArea.SUBVIEW_AREA_2) {
      // Special case caller/callee tables
      KeyStroke keyStroke = KeyboardShortcuts.subviewArea2CallerFocusShortcut;
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, keyStroke);
      getActionMap().put(keyStroke, new FocusCallerAction());
      keyStroke = KeyboardShortcuts.subviewArea2CalleeFocusShortcut;
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, keyStroke);
      getActionMap().put(keyStroke, new FocusCalleeAction());
    } else if (subviewArea == SubviewArea.SUBVIEW_AREA_3) {
      KeyStroke keyStroke = KeyboardShortcuts.subviewArea3FocusShortcut;
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, keyStroke);
      getActionMap().put(keyStroke, new FocusDefaultAction());
    }
  }

  static class FocusDefaultAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      SubviewAreaPanel subviewAreaPanel = (SubviewAreaPanel) e.getSource();
      AnTabbedPane tabbedPane = subviewAreaPanel.getTabbedPane();
      if (tabbedPane != null) {
        tabbedPane.requestFocus();
      }
    }
  }

  static class FocusCallerAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      SubviewAreaPanel subviewAreaPanel = (SubviewAreaPanel) e.getSource();
      AnTabbedPane tabbedPane = subviewAreaPanel.getTabbedPane();
      if (tabbedPane != null && tabbedPane.getTabCount() > 0) {
        Subview subview = (Subview) tabbedPane.getComponentAt(0);
        Component component = subview.getComponent();
        if (component instanceof CalledByCallsDisp) {
          CalledByCallsDisp calledByCallsDisp = (CalledByCallsDisp) component;
          if (calledByCallsDisp.getCallerTable().getSelectedRow() < 0) {
            calledByCallsDisp.getCallerTable().setSelectedRow(0);
          }
          calledByCallsDisp.getCallerTable().requestFocus();
        }
      }
    }
  }

  static class FocusCalleeAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      SubviewAreaPanel subviewAreaPanel = (SubviewAreaPanel) e.getSource();
      AnTabbedPane tabbedPane = subviewAreaPanel.getTabbedPane();
      if (tabbedPane != null && tabbedPane.getTabCount() > 0) {
        Subview subview = (Subview) tabbedPane.getComponentAt(0);
        Component component = subview.getComponent();
        if (component instanceof CalledByCallsDisp) {
          CalledByCallsDisp calledByCallsDisp = (CalledByCallsDisp) component;
          if (calledByCallsDisp.getCalleeTable().getSelectedRow() < 0) {
            calledByCallsDisp.getCalleeTable().setSelectedRow(0);
          }
          calledByCallsDisp.getCalleeTable().requestFocus();
        }
      }
    }
  }

  protected AnTabbedPane getTabbedPane() {
    return tabbedPane;
  }

  protected void addSubview(Subview subview) {
    getTabbedPane().addTab(subview.getDisplayName(), subview);
  }

  protected void setHidden(Subview subview, boolean hidden) {
    int index = getTabbedPane().indexOfComponent(subview);
    if (hidden && index >= 0) {
      getTabbedPane().remove(subview);
    }
    if (!hidden && index < 0) {
      addSubview(subview);
    }
  }

  protected void remove(Subview subview) {
    getTabbedPane().remove(subview);
  }

  protected void setSelected(Subview subview) {
    int index = getTabbedPane().indexOfComponent(subview);
    if (index >= 0) {
      getTabbedPane().setSelectedComponent(subview);
    }
  }

  protected boolean isSelected(Subview subview) {
    Subview selectedSubview = (Subview) getTabbedPane().getSelectedComponent();
    return selectedSubview == subview;
  }

  protected int getTabCount() {
    return getTabbedPane().getTabCount();
  }

  /** Dump contents of subviews. Used in unit tests. */
  public String dumpSubviews() {
    StringBuilder buf = new StringBuilder();

    buf.append("\n");
    buf.append(subviewArea);
    buf.append("\n");
    buf.append("\n");
    Component selectedComponent = tabbedPane.getSelectedComponent();
    for (Component outerComponent : tabbedPane.getComponents()) {
      if (outerComponent instanceof Subview) {
        buf.append(((Subview) outerComponent).getDisplayName());
        buf.append("\n");
        buf.append("\n");
        if (outerComponent == selectedComponent) {
          Component innerComponent = ((Subview) outerComponent).getComponent();
          if (innerComponent instanceof ExportSupport
              && ((ExportSupport) innerComponent)
                  .getSupportedExportFormats()
                  .contains(ExportSupport.ExportFormat.TEXT)) {
            buf.append(
                ((ExportSupport) innerComponent)
                    .exportAsText(0, ExportSupport.ExportFormat.TEXT, null));
          } else {
            buf.append(((Subview) outerComponent).exportAsText());
          }
        }
      }
    }

    return buf.toString();
  }

  /** Dump views structure. Used in unit tests. */
  public String dumpSubviewsVisibility() {
    StringBuilder buf = new StringBuilder();

    buf.append(subviewArea);
    buf.append("\n");
    Component selectedComponent = tabbedPane.getSelectedComponent();
    for (Component component : tabbedPane.getComponents()) {
      if (component instanceof Subview) {
        if (component == selectedComponent) {
          buf.append("  *");
        } else {
          buf.append("   ");
        }
        buf.append(((Subview) component).dumpName());
      }
    }

    return buf.toString();
  }
}

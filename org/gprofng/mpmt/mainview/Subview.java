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
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.mainview.MainViewPanel.SubviewArea;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class Subview extends AnJScrollPane {

  private final String viewName;
  private final String displayName;
  private final SubviewArea subviewArea;
  private boolean readyToShow = true;

  private Component component;
  private final JPanel internalPanel;

  public Subview(
      String viewName, String displayName, final Component component, SubviewArea subviewArea) {
    this.viewName = viewName;
    this.displayName = displayName;
    this.subviewArea = subviewArea;

    setBackground(AnEnvironment.SUBVIEW_PANEL_BACKGROUND);
    setBorder(null);
    internalPanel = new JPanel();
    internalPanel.setLayout(new BorderLayout());
    internalPanel.setBackground(AnEnvironment.SUBVIEW_PANEL_BACKGROUND);
    setComponent(component);
    setViewportView(internalPanel);
    getMainViewPanel().addSubview(this);

    getVerticalScrollBar().setUnitIncrement(6);

    registerKeyboardAction(
        new SubviewHelpAction(),
        "help",
        KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
  }

  public static final class SubviewHelpAction implements ActionListener {

    @Override
    public void actionPerformed(final ActionEvent event) {
      // Delegate help to current view.
      AnWindow.getInstance().getViews().getCurrentViewDisplay().showHelp();
    }
  }

  public String exportAsText() {
    StringBuilder buf = new StringBuilder();
    //        buf.append(displayName);
    //        buf.append("\n");
    exportAsText(buf, this);
    return buf.toString();
  }

  private void exportAsText(StringBuilder buf, Component component) {
    if (component instanceof JLabel) {
      buf.append("Label:" + ((JLabel) component).getText());
      buf.append("\n");
    }
    if (component instanceof JTextField) {
      buf.append("TextField:" + ((JTextField) component).getText());
      buf.append("\n");
    }
    if (component instanceof JTextArea) {
      buf.append("JTextArea:" + ((JTextArea) component).getText());
      buf.append("\n");
    }
    if (component instanceof JList) {
      JList list = (JList) component;
      for (int i = 0; i < list.getModel().getSize(); i++) {
        buf.append(list.getModel().getElementAt(i).toString());
        buf.append("\n");
      }
    }
    if (component instanceof Container) {
      Component[] components = ((Container) component).getComponents();
      for (Component c : components) {
        exportAsText(buf, c);
      }
    }
  }

  public String getViewName() {
    return viewName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setComponent(Component component) {
    internalPanel.removeAll();
    if (component != null) {
      internalPanel.add(component, BorderLayout.CENTER);
    }
    this.component = component;
  }

  public void removeComponent() {
    remove(component);
    this.component = null;
  }

  public Component getComponent() {
    return component;
  }

  public SubviewArea getSubviewArea() {
    return subviewArea;
  }

  public void setHidden(boolean hidden) {
    getMainViewPanel().setHidden(subviewArea, this, hidden);
  }

  public void setSelected() {
    getMainViewPanel().setSelected(subviewArea, this);
  }

  public boolean isSelected() {
    return getMainViewPanel().isSelected(subviewArea, this);
  }

  private MainViewPanel getMainViewPanel() {
    return AnWindow.getInstance().getMainViewPanel();
  }

  /**
   * @return the readyToShow
   */
  public boolean isReadyToShow() {
    return readyToShow;
  }

  /**
   * @param readyToShow the readyToShow to set
   */
  public void setReadyToShow(boolean readyToShow) {
    this.readyToShow = readyToShow;
  }

  public void showSubview(boolean show) {
    boolean toShow = isReadyToShow() && show;
    setHidden(!toShow);
  }

  /** Dump views structure. Used in unit tests. */
  public String dumpName() {
    StringBuilder buf = new StringBuilder();

    buf.append(getViewName());
    buf.append("\n");

    return buf.toString();
  }
}

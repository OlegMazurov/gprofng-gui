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
package org.gprofng.mpmt;

import org.gprofng.mpmt.settings.ViewsSetting;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class ViewDisplayPanel extends JPanel {

  private ViewsSetting.View currentView = null;

  public ViewDisplayPanel() {
    setOpaque(false);
    setBorder(null);
    setLayout(new BorderLayout());

    // Focus
    final KeyStroke viewsFocusShortCut = KeyboardShortcuts.viewsFocusShortCut;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(viewsFocusShortCut, viewsFocusShortCut);
    getActionMap()
        .put(
            viewsFocusShortCut,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                AnWindow.getInstance().getNavigationPanel().getViewsPanel().requestFocus();
              }
            });

    // Focus
    KeyStroke mainViewFocusShortcut = KeyboardShortcuts.mainViewFocusShortcut;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(mainViewFocusShortcut, mainViewFocusShortcut);
    getActionMap()
        .put(
            mainViewFocusShortcut,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                getCurrentViewDisplay().requestFocus();
              }
            });
  }

  public AnDisplay getCurrentViewDisplay() {
    AnDisplay ad = null;
    if (currentView != null) {
      ad = currentView.getAnDispTab().getTComp();
    }
    return ad;
  }

  public String getCurrentViewName() {
    String ad = null;
    if (currentView != null) {
      ad = currentView.getAnDispTab().getTCmd();
    }
    return ad;
  }

  public boolean viewComponent(String cmd) {
    if (cmd == null) {
      return false;
    }
    if (currentView != null && currentView.getAnDispTab().getTCmd().equals(cmd)) {
      return false;
    }
    ViewsSetting.View view = AnWindow.getInstance().getSettings().getViewsSetting().findView(cmd);
    if (view == null) {
      return false;
    }
    removeAll();
    add(view.getAnDispTab().getTComp(), BorderLayout.CENTER);
    currentView = view;
    view.getAnDispTab()
        .getTComp()
        .repaint(); // Necessary, otherwise views don't paint correctly if changing views
    // rapitly....
    return true;
  }
}

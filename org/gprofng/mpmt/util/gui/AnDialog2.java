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

package org.gprofng.mpmt.util.gui;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.KeyboardShortcuts;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class AnDialog2 extends JDialog {

  private Font font = null;
  private AnDialogOuterPanel anDialogOuterPanel;

  public enum Status {
    OK,
    CANCEL
  };

  private Status status = Status.CANCEL;
  private Component relativeToComponent;
  private boolean locationSet = false;

  public AnDialog2(Frame owner, Component relativeToComponent, String title) {
    super(owner, title);
    init(relativeToComponent);
  }

  public AnDialog2(Dialog owner, Component relativeToComponent, String title) {
    super(owner, title);
    init(relativeToComponent);
  }

  private void init(Component relativeToComponent) {
    this.relativeToComponent = relativeToComponent;
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    anDialogOuterPanel = new AnDialogOuterPanel(this);
    AnInsetsPanel anInsetsPanel = new AnInsetsPanel(anDialogOuterPanel, 12, 12, 12, 12);
    anInsetsPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    anDialogOuterPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);

    Container contentPane = getContentPane();
    contentPane.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    contentPane.setLayout(new BorderLayout());
    contentPane.add(anInsetsPanel, BorderLayout.CENTER);
    setModal(true);
    // close/dispose when ESC
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc"); // NOI18N
    getRootPane()
        .getActionMap()
        .put(
            "esc",
            new AbstractAction() { // NOI18N
              @Override
              public void actionPerformed(ActionEvent e) {
                setVisible(false);
              }
            });
    pack();
  }

  public void setCustomPanel(JPanel customPanel) {
    anDialogOuterPanel.setCustomPanel(customPanel);
    pack();
  }

  public void addAuxButton(JButton auxButton) {
    anDialogOuterPanel.addAuxButton(auxButton);
  }

  public void setHelpTag(final String helpTag) {
    anDialogOuterPanel.setHelpTag(helpTag);
    if (helpTag != null) {
      // F1 help
      getRootPane()
          .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
          .put(
              KeyboardShortcuts.helpActionShortcut, KeyboardShortcuts.helpActionShortcut); // NOI18N
      getRootPane()
          .getActionMap()
          .put(
              KeyboardShortcuts.helpActionShortcut,
              new AbstractAction() { // NOI18N
                private final String helpId = helpTag;

                @Override
                public void actionPerformed(ActionEvent e) {
                  Analyzer.showHelp(helpId);
                }
              });
    }
    pack();
  }

  /**
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * @param status the status to set
   */
  protected void setStatus(Status status) {
    this.status = status;
  }

  public JButton getOKButton() {
    return anDialogOuterPanel.getOKButton();
  }

  public JButton getCancelButton() {
    return anDialogOuterPanel.getCancelButton();
  }

  public JButton getHelpButton() {
    return anDialogOuterPanel.getHelpButton();
  }

  public Font getDialogFont() {
    if (font == null) {
      font = getFont().deriveFont(Font.PLAIN);
    }
    return font;
  }

  public void busyCursor(boolean set) {
    setCursor(set ? AnUtility.wait_cursor : AnUtility.norm_cursor);
  }

  @Override
  public void setVisible(boolean b) {
    if (b && !locationSet) {
      setLocationRelativeTo(relativeToComponent);
      locationSet = true;
    }
    super.setVisible(b);
  }
}

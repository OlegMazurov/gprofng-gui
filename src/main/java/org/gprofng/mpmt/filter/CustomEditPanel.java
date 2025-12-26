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

package org.gprofng.mpmt.filter;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnIconButton;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class CustomEditPanel extends JPanel {
  private static final String help_id = AnVariable.HELP_Filter;
  MyUndoManager manager = null;
  JTextPane pane = null;
  Action undoAction = null;
  Action redoAction = null;
  Action clearAction = null;

  public CustomEditPanel(Filters filter) {
    pane = new JTextPane();
    pane.setEditable(true); // Editable
    pane.setBorder(new LineBorder(AnEnvironment.SPLIT_PANE_BORDER_COLOR, 1));
    pane.setForeground(AnEnvironment.FILTER_FOREGROUND_COLOR);
    setLayout(new BorderLayout());
    add(new AnJScrollPane(pane), BorderLayout.CENTER);

    // Create the undo manager and actions
    manager = new MyUndoManager();
    pane.getDocument().addUndoableEditListener(manager);

    undoAction = new UndoAction(manager);
    redoAction = new RedoAction(manager);
    clearAction = new ClearAction(manager);

    // Add the actions to buttons
    JButton undoButton = new AnIconButton(AnUtility.undo_icon);
    undoButton.setToolTipText(AnLocale.getString("Undo"));

    JButton redoButton = new AnIconButton(AnUtility.redo_icon);
    redoButton.setToolTipText(AnLocale.getString("Redo"));

    undoButton.addActionListener(undoAction);
    redoButton.addActionListener(redoAction);

    JPanel panel = new JPanel();
    panel.setLayout(new java.awt.GridBagLayout());
    GridBagConstraints gridBagConstraints;

    JLabel keywordsLabel = new JLabel();
    AnUtility.setTextAndAccessibleContext(keywordsLabel, AnLocale.getString("Keywords:"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(8, 0, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panel.add(keywordsLabel, gridBagConstraints);

    AnUtility.checkIPCOnWrongThread(false);
    KeywordsPanel keywordsPanel = new KeywordsPanel(filter);
    AnUtility.checkIPCOnWrongThread(true);
    keywordsPanel.setPreferredSize(new Dimension(100, 180));
    keywordsLabel.setLabelFor(keywordsPanel);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    panel.add(keywordsPanel, gridBagConstraints);

    add(panel, BorderLayout.SOUTH);

    panel = new JPanel();
    panel.setLayout(new java.awt.GridBagLayout());

    JLabel filterSpecLabel;
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(8, 0, 0, 0);
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    filterSpecLabel = new JLabel();
    AnUtility.setTextAndAccessibleContext(
        filterSpecLabel, AnLocale.getString("Filter Specification:"));
    AnUtility.setTextAndAccessibleContext(keywordsLabel, AnLocale.getString("Keywords:"));
    panel.add(filterSpecLabel, gridBagConstraints);
    filterSpecLabel.setDisplayedMnemonic(
        AnLocale.getString('F', "MNEMONIC_FILTER_SPECIFICATION_BUTTON"));
    filterSpecLabel.setLabelFor(pane);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(4, 4, 2, 0);
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    panel.add(undoButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(4, 4, 2, 0);
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    panel.add(redoButton, gridBagConstraints);

    add(panel, BorderLayout.NORTH);

    // Assign the actions to keys
    pane.registerKeyboardAction(
        undoAction,
        KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK),
        JComponent.WHEN_FOCUSED);
    pane.registerKeyboardAction(
        redoAction,
        KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK),
        JComponent.WHEN_FOCUSED);

    getAccessibleContext()
        .setAccessibleName(
            AnLocale.getString("Custom filter settings", "ACCESSIBILITY_Custom_Panel"));
    getAccessibleContext()
        .setAccessibleDescription(
            AnLocale.getString("Custom filter settings", "ACCESSIBILITY_Custom_Panel"));
    undoButton
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Undo button", "ACCESSIBILITY_Undo_button"));
    redoButton
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Redo button", "ACCESSIBILITY_Redo_button"));
  }

  //    class AnIconButton extends JButton implements MouseListener, FocusListener {
  //
  //        public AnIconButton(Icon icon) {
  //            super(icon);
  //            setBorder(javax.swing.BorderFactory.createLineBorder(Color.LIGHT_GRAY));
  //            setOpaque(false);
  //            setContentAreaFilled(false);
  //            setBorderPainted(false);
  //
  //            addMouseListener(this);
  //            addFocusListener(this);
  //        }
  //
  //        public Insets getInsets() {
  //            return new Insets(3,3,3,3);
  //        }
  //
  //        public void mouseExited(MouseEvent e) {
  //            setBorderPainted(false);
  //            validate();
  //            repaint();
  //        }
  //
  //        public void mouseClicked(MouseEvent e) {
  //        }
  //
  //        public void mouseEntered(MouseEvent e) {
  //            setBorderPainted(true);
  //            validate();
  //            repaint();
  //        }
  //
  //        public void mousePressed(MouseEvent e) {
  //        }
  //
  //        public void mouseReleased(MouseEvent e) {
  //        }
  //
  //        public void focusGained(FocusEvent e) {
  //            setBorderPainted(true);
  //            validate();
  //            repaint();
  //        }
  //
  //        public void focusLost(FocusEvent e) {
  //            setBorderPainted(false);
  //            validate();
  //            repaint();
  //        }
  //    }
  class MyUndoManager extends UndoManager {

    boolean applied = true;
    UndoableEdit applyEdit = null;

    public MyUndoManager() {
      getTextPane()
          .getDocument()
          .addUndoableEditListener(
              new UndoableEditListener() {
                public void undoableEditHappened(UndoableEditEvent evt) {
                  UndoableEdit anEdit = evt.getEdit();
                  if (applied) {
                    applyEdit = anEdit;
                    applied = false;
                  }
                }
              });
    }

    public void undoToLastApply() {
      if (applyEdit != null) {
        applied = true;
        undoTo(applyEdit);
      }
      applyEdit = null;
    }

    public void apply() {
      applied = true;
      applyEdit = null;
    }
  }

  public JTextPane getTextPane() {
    return pane;
  }

  public void resetToFactorySetting() {
    clearAction.actionPerformed(null);
  }

  public void setFilter(String filter) {
    pane.setText(filter);
  }

  public void addText(String text) {
    int pos = pane.getDocument().getLength();
    try {
      pane.getDocument().insertString(pos, text, null);
    } catch (BadLocationException ble) {
    }
  }

  public void apply() {
    manager.apply();
  }

  public void undoToLastApply() {
    manager.undoToLastApply();
  }

  // The Undo action
  public class UndoAction extends AbstractAction {

    private UndoManager manager;

    public UndoAction(UndoManager manager) {
      this.manager = manager;
    }

    public void actionPerformed(ActionEvent evt) {
      try {
        manager.undo();
      } catch (CannotUndoException e) {
        Toolkit.getDefaultToolkit().beep();
      }
    }
  }

  // The Redo action
  public class RedoAction extends AbstractAction {

    private UndoManager manager;

    public RedoAction(UndoManager manager) {
      this.manager = manager;
    }

    public void actionPerformed(ActionEvent evt) {
      try {
        manager.redo();
      } catch (CannotRedoException e) {
        Toolkit.getDefaultToolkit().beep();
      }
    }
  }

  // The Clear action
  public class ClearAction extends AbstractAction {

    private UndoManager manager;

    public ClearAction(UndoManager manager) {
      this.manager = manager;
    }

    public void actionPerformed(ActionEvent evt) {
      try {
        int pos = pane.getDocument().getLength();
        pane.getDocument().remove(0, pos);
        //                pane.getDocument().insertString(pos, "dsadsada", null);
      } catch (BadLocationException be) {
      }
    }
  }
}

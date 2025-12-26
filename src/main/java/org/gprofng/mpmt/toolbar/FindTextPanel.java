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


package org.gprofng.mpmt.toolbar;

import org.gprofng.mpmt.AnIconButton;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.picklist.StringPickList;
import org.gprofng.mpmt.picklist.StringPickListElement;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class FindTextPanel extends JPanel {

  private JLabel findLabel;
  private JComboBox findComboBox;
  private AnIconButton findUp;
  private AnIconButton findDown;
  private JCheckBox findCase;
  public static final int MAX_FIND_ITEMS = 10;
  private ActionListener findActionListener;
  private DocumentListener findDocumentListener;

  public FindTextPanel() {
    GridBagConstraints gridBagConstraints;
    int gridx = 0;

    setLayout(new GridBagLayout());
    setOpaque(false);

    // Combobox
    findComboBox = new FindComboBox();
    findComboBox.setToolTipText(AnLocale.getString("Find text in view..."));
    AnUtility.setAccessibleContext(
        findComboBox.getAccessibleContext(), findComboBox.getToolTipText());
    findComboBox.setFont(findComboBox.getFont().deriveFont(Font.PLAIN));
    findComboBox.setMaximumRowCount(MAX_FIND_ITEMS);
    //        findComboBox.setEditable(true); // This causes the panel to 'jump' up/down. Moving to
    // setConrolsEnabled. Don't understand.....
    findComboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxx");
    initializeFindTexts(UserPref.getInstance().getFindPickList(), null);
    findComboBox.setSelectedItem("");
    findComboBox.setPreferredSize(new Dimension(findComboBox.getPreferredSize().width, 20));
    findComboBox.setEnabled(true);
    // Combobox Listeners
    findComboBox.addPopupMenuListener(
        new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            find(getFindComboBoxText(), true, findCase.isSelected());
          }

          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    findComboBox
        .getEditor()
        .addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(final ActionEvent event) {
                if (event.getID() == ActionEvent.ACTION_PERFORMED) {
                  find(getFindComboBoxText(), true, findCase.isSelected());
                }
              }
            });
    findDocumentListener =
        new DocumentListener() {
          @Override
          public void insertUpdate(DocumentEvent e) {
            resetComboBoxState();
          }

          @Override
          public void removeUpdate(DocumentEvent e) {
            resetComboBoxState();
          }

          @Override
          public void changedUpdate(DocumentEvent e) {
            resetComboBoxState();
          }
        };
    ((JTextField) findComboBox.getEditor().getEditorComponent())
        .getDocument()
        .addDocumentListener(findDocumentListener);
    findComboBox.addPopupMenuListener(
        new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            resetComboBoxState();
          }

          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    ((JTextField) findComboBox.getEditor().getEditorComponent())
        .addKeyListener(
            new KeyAdapter() {
              @Override
              public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER
                    && e.getModifiers() == KeyEvent.SHIFT_MASK) {
                  find(getFindComboBoxText(), false, findCase.isSelected());
                } else {
                  super.keyTyped(e);
                }
              }
            });

    findLabel = new JLabel();
    AnUtility.setTextAndAccessibleContext(findLabel, AnLocale.getString("Find:"));
    findLabel.setDisplayedMnemonic(AnLocale.getString('n', "MN_FindLabel"));
    findLabel.setLabelFor(findComboBox);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    add(findLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    add(findComboBox, gridBagConstraints);
    // Up
    findUp = new AnIconButton(AnUtility.prev_icon);
    findUp.setToolTipText(AnLocale.getString("Find Previous") + " (Shift+Enter)");
    findUp.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            find(getFindComboBoxText(), false, findCase.isSelected());
          }
        });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    add(findUp, gridBagConstraints);
    // Down
    findDown = new AnIconButton(AnUtility.next_icon);
    findDown.setToolTipText(AnLocale.getString("Find Next") + " (Enter)");
    findDown.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            find(getFindComboBoxText(), true, findCase.isSelected());
          }
        });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    add(findDown, gridBagConstraints);
    // Case
    findCase = new JCheckBox(AnLocale.getString("Match Case"));
    AnUtility.setAccessibleContext(findCase.getAccessibleContext(), findCase.getText());
    findCase.setMnemonic(AnLocale.getString('C', "FindMatchCaseMN"));
    findCase.setFont(findCase.getFont().deriveFont(Font.PLAIN));
    findCase.setOpaque(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 0, 0, 2);
    add(findCase, gridBagConstraints);
  }

  private class FindComboBox extends JComboBox {

    @Override
    public void paint(Graphics g) {
      super.paint(g); // To change body of generated methods, choose Tools | Templates.

      int yOffset = getSize().height - 6;
      String val = (String) findComboBox.getSelectedItem();
      if (val == null || val.length() == 0) {
        String helpText = AnLocale.getString("Find text in view");
        g.setColor(new Color(190, 190, 190));
        g.drawString(helpText, 7, yOffset);
      }
      ((JTextField) getEditor().getEditorComponent())
          .setToolTipText(AnLocale.getString("Find Text in View"));
    }
  }

  public void initializeFindTexts(StringPickList spl, String selected) {
    findComboBox.removeActionListener(findActionListener);
    ((JTextField) findComboBox.getEditor().getEditorComponent())
        .getDocument()
        .removeDocumentListener(findDocumentListener);
    findComboBox.removeAllItems();
    for (StringPickListElement elem : spl.getStringElements()) {
      findComboBox.addItem(elem.getString());
    }
    if (selected != null) {
      findComboBox.setSelectedItem(selected);
    }
    findComboBox.addActionListener(findActionListener);
    ((JTextField) findComboBox.getEditor().getEditorComponent())
        .getDocument()
        .addDocumentListener(findDocumentListener);
  }

  private void resetComboBoxState() {
    JTextField textField = (JTextField) findComboBox.getEditor().getEditorComponent();
    textField.setForeground(Color.BLACK);
    findComboBox.setToolTipText("");
  }

  public void setConrolsEnabled(boolean enabled) {
    findLabel.setEnabled(enabled);
    findComboBox.setEditable(enabled);
    findComboBox.setEnabled(enabled);
    findUp.setEnabled(enabled);
    findDown.setEnabled(enabled);
    findCase.setEnabled(enabled);
  }

  private String getFindComboBoxText() {
    JTextField textField = (JTextField) findComboBox.getEditor().getEditorComponent();
    return textField.getText();
  }

  private void updateFindComboBox(String str) {
    String selected = getFindComboBoxText();
    UserPref.getInstance().getFindPickList().addElement(str);
    initializeFindTexts(UserPref.getInstance().getFindPickList(), selected);
  }

  private void find(final String str, final boolean next, boolean caseSensitive) {
    final int status;
    if ((str != null) && (str.length() == 0)) {
      return;
    }
    Object progressBarHandle =
        AnWindow.getInstance()
            .getSystemProgressPanel()
            .progressBarStart(AnLocale.getString("Searching"));
    status =
        AnWindow.getInstance().getViews().getCurrentViewDisplay().find(str, next, caseSensitive);
    JTextField findTextField = (JTextField) findComboBox.getEditor().getEditorComponent();
    if (status == -1) {
      findTextField.setForeground(Color.RED);
      findComboBox.setToolTipText(AnLocale.getString("Text not found"));
    } else {
      findTextField.setForeground(Color.BLACK);
      findComboBox.setToolTipText(AnLocale.getString(""));
    }
    updateFindComboBox(str);
    AnWindow.getInstance().getSystemProgressPanel().progressBarStop(progressBarHandle);
  }
}

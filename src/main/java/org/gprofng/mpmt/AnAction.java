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

import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class AnAction extends AbstractAction {

  private String text;
  private Icon icon;
  private String tooltipText;
  private String accDescription;
  private Action action;
  private KeyStroke keyStroke = null;
  private Character mnemonic = null;

  private JMenuItem menuItem = null;

  public AnAction(String text, Action action) {
    this(text, null, null, null, action);
  }

  public AnAction(String text, Icon icon, Action action) {
    this(text, icon, null, null, action);
  }

  public AnAction(
      String text, Icon icon, String tooltipText, String accDescription, Action action) {
    super(text, icon);
    this.text = text;
    this.icon = icon;
    this.tooltipText = tooltipText;
    if (accDescription != null) {
      this.accDescription = accDescription;
    } else {
      this.accDescription = text;
    }
    this.action = action;
  }

  public JButton createActionButton() {
    JButton actionButton = new AnIconButton(icon);
    actionButton.setMargin(new Insets(4, 5, 4, 5));
    actionButton.setHideActionText(true);
    actionButton.setAction(this);
    actionButton.setActionCommand((String) getValue(Action.NAME));
    actionButton.setToolTipText(getTooltipText());
    AccessibleContext context = actionButton.getAccessibleContext();
    context.setAccessibleName(text);
    context.setAccessibleDescription(accDescription);

    return actionButton;
  }

  public JMenuItem getMenuItem() {
    return getMenuItem(true);
  }

  public JMenuItem getMenuItem(boolean addAccelerator) {
    if (menuItem == null) {
      menuItem = new JMenuItem();
      menuItem.setText(text);
      menuItem.setAction(this);
      if (keyStroke != null) {
        if (addAccelerator) {
          menuItem.setAccelerator(keyStroke);
        } else {
          menuItem.setText(text + AnUtility.keyStrokeToStringFormatted(keyStroke));
        }
      }
      if (mnemonic != null) {
        menuItem.setMnemonic(mnemonic);
      }
    }
    return menuItem;
  }

  public void setKeyboardShortCut(KeyStroke keyStroke) {
    setKeyboardShortCut(AnWindow.getInstance().getMainPanel(), keyStroke.toString(), keyStroke);
  }

  public void setKeyboardShortCut(JPanel panel, String id, KeyStroke keyStroke) {
    this.keyStroke = keyStroke;
    if (panel != null) {
      panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, id);
      panel
          .getActionMap()
          .put(
              id,
              new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ev) {
                  AnAction.this.actionPerformed(ev);
                }
              });
    }
  }

  public void setMnemonic(char m) {
    mnemonic = m;
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    action.actionPerformed(event);
  }

  public String getTooltipText() {
    String tt = tooltipText;
    if (tt == null) {
      tt = text;
    }
    if (tt != null && keyStroke != null) {
      tt = tt + AnUtility.keyStrokeToStringFormatted(keyStroke);
    }
    return tt;
  }

  public String getText() {
    return text;
  }

  public String getTextNoDots() {
    String textnd = text;
    if (textnd != null && textnd.endsWith("...")) {
      textnd = textnd.substring(0, textnd.length() - 3);
    }
    return textnd;
  }
}

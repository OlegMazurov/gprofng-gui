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


package org.gprofng.mpmt.filter;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.util.gui.ActionTextField;
import org.gprofng.mpmt.util.gui.ToolTipPopup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * @author tpreisle
 */
public class FilterTextField extends ActionTextField {
  private Color background;
  private FilterClause clause;
  private Filters filter;
  private ToolTipPopup tooltipPopup = null;

  public FilterTextField(Filters filter, Color background, FilterClause clause) {
    this.filter = filter;
    this.background = background;
    this.clause = clause;
    setForeground(AnEnvironment.FILTER_FOREGROUND_COLOR);
    setBackground(background);
    setFont(getFont().deriveFont(Font.BOLD));
    setBorder(null);
    setEditable(false);
    setActionToolTipText(AnLocale.getString("Remove this filter"));

    String clauseTT = clause.getClause();
    if (clauseTT.length() > 100) {
      clauseTT = clauseTT.substring(0, 100) + "...";
    }
    setToolTipText(
        "<html><b>"
            + (clause.getLongName() != null ? clause.getLongName() : "???")
            + "</b>"
            + ": "
            + clauseTT
            + "</html>");
  }

  @Override
  public void paint(Graphics g) {
    if (focused) {
      setBackground(AnEnvironment.FILTER_BACKGROUND_SELECTED_COLOR);
    } else {
      setBackground(background);
    }
    super.paint(g);
  }

  // @Override
  @Override
  public void mouseClicked(MouseEvent me) {
    if (me.getButton() == MouseEvent.BUTTON3) {
      showContextMenu(FilterTextField.this, me.getPoint().x + 5, me.getPoint().y + 5);
    }
  }

  // @Override
  @Override
  public void mouseExited(MouseEvent me) {
    super.mouseExited(me);
    hideTooltipPopup();
  }

  // @Override
  @Override
  public void mouseReleased(MouseEvent me) {
    super.mouseReleased(me);
    if (!me.isConsumed()) {
      showTooltipPopup();
    }
  }

  private void showTooltipPopup() {
    if (tooltipPopup != null) {
      return;
    }

    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new GridBagLayout());

    Font font = getFont().deriveFont(Font.PLAIN);

    int gridy = 0;
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;

    JLabel label1 = new JLabel();
    label1.setFont(label1.getFont().deriveFont(Font.BOLD));
    String viewName = clause.getViewName();
    if (viewName != null) {
      label1.setText(viewName + ": " + clause.getLongName());
    } else {
      label1.setText(clause.getLongName());
    }
    label1.setForeground(AnEnvironment.FILTER_FOREGROUND_COLOR);
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    panel.add(label1, gridBagConstraints);

    JLabel label2 = new JLabel();
    label2.setFont(font);
    label2.setText(AnLocale.getString("Expression: ") + clause.getClause(110));
    label2.setForeground(AnEnvironment.TOOLTIP_POPUP_FOREGROUND_COLOR);
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.insets = new Insets(4, 0, 0, 0);
    panel.add(label2, gridBagConstraints);

    tooltipPopup = new ToolTipPopup(this, panel, ToolTipPopup.Location.EAST, false);
    tooltipPopup.show();
  }

  private void hideTooltipPopup() {
    if (tooltipPopup != null) {
      tooltipPopup.hide();
      tooltipPopup = null;
    }
  }

  protected void showTooltipPopupAdded() {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new GridBagLayout());

    int gridx = 0;
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;

    JLabel label0 = new JLabel();
    label0.setText(AnLocale.getString("New filter added:"));
    label0.setForeground(AnEnvironment.TOOLTIP_POPUP_FOREGROUND_COLOR);
    label0.setFont(label0.getFont().deriveFont(Font.BOLD));
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    panel.add(label0, gridBagConstraints);

    JLabel label1 = new JLabel();
    label1.setFont(label1.getFont().deriveFont(Font.BOLD));
    label1.setText(clause.getShortName());
    label1.setForeground(AnEnvironment.FILTER_FOREGROUND_COLOR);
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    panel.add(label1, gridBagConstraints);

    ToolTipPopup toolTipPopup = new ToolTipPopup(this, panel, ToolTipPopup.Location.EAST, true);
    toolTipPopup.show(0, 3000);
  }

  protected void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {
    filter.removeClause(clause);
    hideTooltipPopup();
  }

  private void showContextMenu(Component component, int x, int y) {
    JPopupMenu popup = new JPopupMenu();
    JMenuItem removeItem = new JMenuItem(AnLocale.getString("Remove this filter"));
    popup.add(removeItem);
    removeItem.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            removeButtonActionPerformed(null);
          }
        });
    popup.show(component, x, y);
  }
}

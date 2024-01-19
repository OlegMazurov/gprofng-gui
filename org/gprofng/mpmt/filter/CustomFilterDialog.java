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

import org.gprofng.mpmt.AnDialog;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * @author tpreisle
 */
public class CustomFilterDialog extends AnDialog {
  private Frame frame;
  private AnWindow window;
  private JPanel mainPanel;
  private CustomPanel customPanel;
  private static int customFilterNumber = 1;

  public CustomFilterDialog(Frame frame, AnWindow window) {
    super(
        window,
        frame,
        AnLocale.getString("Advanced Custom Filter"),
        false,
        null,
        null,
        AnVariable.HELP_Filter);
    this.window = window;
    setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

    customPanel = new CustomPanel(window.getFilters(), this);

    mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Custom Filter"));
    mainPanel.getAccessibleContext().setAccessibleName(AnLocale.getString("Custom Filter"));
    mainPanel.add(customPanel, BorderLayout.CENTER);
    mainPanel.setPreferredSize(new Dimension(700, 500));

    setAccessory(mainPanel);
    updateButtonStates();
    close.setText(AnLocale.getString("Cancel"));
    //        close.setMnemonic(AnLocale.getString('C', "MNEM_DIALOG_CANCEL"));
    apply.setVisible(false);
  }

  public void setFilter(String filters) {
    customPanel.setFilter(filters);
  }

  public String getFilter() {
    return customPanel.getFilter();
  }

  @Override
  public void setVisible(boolean val) {
    super.setVisible(val);
    updateButtonStates();
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    final String cmd = event.getActionCommand();
    if (cmd.equals(AnLocale.getString("Cancel", "FILTER_DATA_CANCEL"))) {
      setVisible(false);
    } else if (cmd.equals(AnLocale.getString("OK", "FILTER_DATA_OK"))) {
      createCustomFilterFromDialog();
    }
  }

  @Override
  protected boolean vetoClose() {
    return true;
  }

  protected final void updateButtonStates() {
    boolean hasChanges = customPanel.hasChanges();
    ok.setEnabled(hasChanges);
  }

  private void createCustomFilterFromDialog() {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            String customFilter = getFilter();
            if (customFilter.length() > 0) {
              if (!window
                  .getFilters()
                  .validateFilterExpression(window.getFilters().trim(customFilter))) { // IPC
                invalidFilterDialog(customFilter);
                return;
              }
            }
            window
                .getFilters()
                .addClause(
                    AnLocale.getString("Custom Filter " + customFilterNumber),
                    AnLocale.getString("Custom Filter " + customFilterNumber),
                    customFilter,
                    FilterClause.Kind.CUSTOM);
            customFilterNumber++;
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  @Override
                  public void run() {
                    setVisible(false);
                  }
                });
          }
        },
        "Filter Worker Thread");
  }

  private void invalidFilterDialog(String filter) {
    String filterTxt = filter;
    if (filter.length() > 200) {
      filterTxt = filter.subSequence(0, 199) + "...";
    }
    String errmsg = AnLocale.getString("Invalid filter:") + "\n" + filterTxt;
    AnUtility.showMessage(frame, errmsg, JOptionPane.ERROR_MESSAGE);
  }
}

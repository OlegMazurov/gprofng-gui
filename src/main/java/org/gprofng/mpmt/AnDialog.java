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

import org.gprofng.mpmt.util.gui.AnJScrollPane;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.accessibility.AccessibleContext;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

public abstract class AnDialog extends JDialog implements ActionListener {

  protected int win_id;
  protected AnWindow window;
  protected final AnDialog dialog;
  protected boolean close_on_enter;
  private final Frame frame;
  private JPanel work_area;
  private JPanel response;

  protected ResponseAction ok, apply, close;
  private ResponseAction help;
  public ResponseAction[] aux_actions;
  private final String help_id;

  private boolean locationSet = false;

  public final AnWindow getWindow() {
    return window;
  }

  // Constructor
  public AnDialog(
      final AnWindow awindow,
      final Frame frame,
      final String title,
      final boolean has_scroll,
      final String[] aux,
      final char[] mnemonic,
      final String help_id) {
    super(frame, title);

    this.win_id = 0;
    this.frame = frame;
    window = awindow;
    dialog = this;
    close_on_enter = true;
    this.help_id = help_id;
    // Initialize GUI components & center it in frame
    initComponents(has_scroll, aux, mnemonic);
    final AccessibleContext context = dialog.getAccessibleContext();
    context.setAccessibleName(title);
    context.setAccessibleDescription(title);
  }

  // Constructor with modal setting
  public AnDialog(
      final AnWindow awindow,
      final Frame frame,
      final String title,
      final boolean has_scroll,
      final String[] aux,
      final char[] mnemonic,
      final String help_id,
      final boolean modal) {
    super(frame, title, modal);

    this.win_id = 0;
    this.frame = frame;
    window = awindow;
    dialog = this;
    close_on_enter = true;
    this.help_id = help_id;
    // Initialize GUI components & center it in frame
    initComponents(has_scroll, aux, mnemonic);
    final AccessibleContext context = dialog.getAccessibleContext();
    context.setAccessibleName(title);
    context.setAccessibleDescription(title);
  }

  // Initialize GUI components
  private void initComponents(final boolean has_scroll, final String[] aux, final char[] mnemonic) {
    final Container cnt;
    JPanel panel;
    final int nb;

    cnt = getContentPane();
    cnt.setLayout(new BorderLayout());

    // work area
    work_area = new JPanel(new BorderLayout(4, 4));
    work_area.setOpaque(false);
    work_area.setBorder(new EmptyBorder(12, 12, 0, 12));
    panel = new JPanel(new GridLayout(1, 1));
    panel.setOpaque(false);
    //	panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
    panel.add(has_scroll ? (JComponent) new AnJScrollPane(work_area) : (JComponent) work_area);
    cnt.add(panel, BorderLayout.CENTER);

    // response area
    GridBagConstraints gridBagConstraints;
    response = new JPanel(new GridBagLayout());
    response.setOpaque(false);
    //        response.setBackground(AnEnvironment.DEFAULT_DIALOG_BACKGROUND);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    // gridBagConstraints.weightx = 1.0;
    // Filler
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 12, 0);
    response.add(new JLabel(), gridBagConstraints);
    gridBagConstraints.insets = new java.awt.Insets(8, 4, 12, 0);
    // Add online help
    if (aux == null) {
      aux_actions = null;
    } else {
      nb = aux.length;
      aux_actions = new ResponseAction[nb];
      for (int i = 0; i < nb; i++) {
        response.add(aux_actions[i] = new ResponseAction(aux[i], mnemonic[i]), gridBagConstraints);
      }
    }
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    response.add(new JLabel(""), gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 12, 4);

    response.add(ok = new ResponseAction(AnLocale.getString("OK")), gridBagConstraints);
    response.add(
        apply =
            new ResponseAction(
                AnLocale.getString("Apply"), AnLocale.getString('A', "MNEM_DIALOG_APPLY")),
        gridBagConstraints);
    response.add(
        close = new ResponseAction(AnLocale.getString("Close", "DIALOG_CLOSE"), ' '),
        gridBagConstraints);
    if (help_id != null) {
      final String command = AnLocale.getString("Help", "DIALOG_HELP");
      response.add(
          help = new ResponseAction(command, AnLocale.getString('H', "MNEM_DIALOG_HELP")),
          gridBagConstraints);
      help.setEnabled(false);
      //            ((JComponent) cnt).registerKeyboardAction(help, command,
      // KeyboardShortcuts.helpActionShortcut, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      //            ((JComponent) cnt).registerKeyboardAction(help, command,
      // KeyboardShortcuts.helpActionShortcut, JComponent.WHEN_FOCUSED);
      ((JComponent) cnt)
          .registerKeyboardAction(
              help,
              command,
              KeyboardShortcuts.helpActionShortcut,
              JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    // Filler
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 12, 8);
    response.add(new JLabel(), gridBagConstraints);

    getRootPane().setDefaultButton(ok);
    ((JComponent) cnt)
        .registerKeyboardAction(
            ok,
            AnLocale.getString("OK"),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ((JComponent) cnt)
        .registerKeyboardAction(
            close,
            AnLocale.getString("Close", "DIALOG_CLOSE"),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.add(response);
    cnt.add(response, BorderLayout.SOUTH);
  }

  public final void setWin(final AnWindow awin) {
    window = awin;
    win_id = 0;
  }

  // Get win_id
  public final int getID() {
    return win_id;
  }

  public final JPanel getWorkArea() {
    return work_area;
  }

  /**
   * Gets default buttons
   *
   * @return
   */
  public JButton[] getDefaultButtons() {
    JButton[] buttons = new JButton[3];
    buttons[0] = (JButton) ok;
    buttons[1] = (JButton) apply;
    buttons[2] = (JButton) close;
    return buttons;
  }

  // Set visible
  @Override
  public void setVisible(final boolean aFlag) {
    if (aFlag) {
      if (!locationSet) {
        setLocationRelativeTo(frame);
        locationSet = true;
      }
    }
    super.setVisible(aFlag);

    if (aFlag) {
      work_area.requestFocus();
    }
  }

  // Add component in work area
  protected final void setAccessory(final JComponent work) {
    work_area.add(work, BorderLayout.CENTER);
    pack();
  }

  protected boolean vetoClose() {
    return false;
  }

  protected final class ResponseAction extends JButton implements ActionListener {

    public ResponseAction(final String text) {
      super(text);
      final AccessibleContext context = this.getAccessibleContext();
      context.setAccessibleName(text);
      context.setAccessibleDescription(text);
      addActionListener(this);
    }

    public ResponseAction(final String text, final char mnemonic) {
      this(text);

      if (mnemonic != ' ') {
        setMnemonic(mnemonic);
      }
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      final String cmd = event.getActionCommand();

      if (cmd.equals(AnLocale.getString("OK"))) {
        dialog.actionPerformed(event);
        if (!vetoClose() && (close_on_enter || (event.getSource() != getContentPane()))) {
          dialog.setVisible(false);
        }
      } else if (cmd.equals(AnLocale.getString("Apply"))) {
        dialog.actionPerformed(event);
      } else if (cmd.equals(AnLocale.getString("Close", "DIALOG_CLOSE"))) {
        if (close_on_enter || (event.getSource() != getContentPane())) {
          dialog.setVisible(false);
        }
      } else if (cmd.equals(AnLocale.getString("Help", "DIALOG_HELP"))) {
        Analyzer.showHelp(help_id);
      } else {
        dialog.actionPerformed(event);
      }
    }
  }
}

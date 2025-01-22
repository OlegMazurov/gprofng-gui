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

package org.gprofng.mpmt;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.ipc.IPCProtocol;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnDialog2;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.HeadlessException;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

public class AboutPanel extends JPanel {
  private static final String GUI_VERSION = "gui_version";
  private static final String REGULAR = "regular";
  private static final String BOLD = "bold";
  private static final String ITALIC = "italic";
  private static final String strLegalNotice =
      "Copyright (C) 2022-2025 Free Software Foundation\n\n"
      + "This program is free software; you can redistribute it and/or modify\n"
      + "it under the terms of the GNU General Public License as published by\n"
      + "the Free Software Foundation, either version 3 of the License, or\n"
      + "(at your option) any later version.\n\n"
      + "This program is distributed in the hope that it will be useful,\n"
      + "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
      + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
      + "GNU General Public License for more details.\n\n"
      + "You should have received a copy of the GNU General Public License\n"
      + "along with this program. If not, see <http://www.gnu.org/licenses>.\n";

  public AboutPanel() {
    initComponents();
    iconLabel.setBorder(BorderFactory.createLineBorder(AnEnvironment.ABOUT_BOX_BORDER_COLOR));
    copyrightScrollPane.setBorder(
        BorderFactory.createLineBorder(AnEnvironment.ABOUT_BOX_BORDER_COLOR));
    infoScrollPane.setBorder(BorderFactory.createLineBorder(AnEnvironment.ABOUT_BOX_BORDER_COLOR));
    iconLabel.setIcon(
        new javax.swing.ImageIcon(
            getClass().getResource("/org/gprofng/mpmt/icons/performanceAnalyzerSplash.gif")));

    copyrightTextArea.setWrapStyleWord(true);
    copyrightTextArea.setLineWrap(true);
    copyrightTextArea.setText(strLegalNotice);
    copyrightTextArea.setCaretPosition(0);
    copyrightTextArea.setBackground(new Color(254, 254, 254));
    //        copyrightTextArea.setOpaque(false);
    String cn = AnLocale.getString("Copyright Notice");
    copyrightTextArea.getAccessibleContext().setAccessibleName(cn);
    copyrightTextArea.getAccessibleContext().setAccessibleDescription(cn);
    copyrightTextArea.setFont(
        copyrightTextArea.getFont().deriveFont((float) copyrightTextArea.getFont().getSize() - 1));

    StyledDocument doc = infoTextPane.getStyledDocument();
    Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
    Style regularStyle = doc.addStyle(REGULAR, def);
    Style italicStyle = doc.addStyle(ITALIC, regularStyle);
    StyleConstants.setItalic(italicStyle, true);
    Style boldStyle = doc.addStyle(BOLD, regularStyle);
    StyleConstants.setBold(boldStyle, true);

    String gui_version = AnLocale.getString(GUI_VERSION);
    if (gui_version.compareTo(GUI_VERSION) == 0) // Did not find
      gui_version = "";
    try {
      doc.insertString(doc.getLength(),
          AnLocale.getString("gprofng-display-gui version: "), boldStyle);
      doc.insertString(doc.getLength(), gui_version + "\n", regularStyle);
      doc.insertString(doc.getLength(),
          AnLocale.getString("GUI protocol version: "), boldStyle);
      doc.insertString(doc.getLength(),
          IPCProtocol.version + "\n", regularStyle);
      doc.insertString(doc.getLength(),
          AnLocale.getString("Install: "), boldStyle);
      doc.insertString(doc.getLength(), AnUtility.clearPath(
          Analyzer.fdhome) + "\n", regularStyle);
      doc.insertString(doc.getLength(),
          AnLocale.getString("Working directory: "), boldStyle);
      doc.insertString(doc.getLength(), AnUtility.clearPath(
          Analyzer.getInstance().getWorkingDirectory()) + "\n", regularStyle);
      doc.insertString(doc.getLength(), AnLocale.getString("Java: "), boldStyle);
      doc.insertString(doc.getLength(), Analyzer.jvm_ver + "\n", regularStyle);
      doc.insertString(doc.getLength(),
          AnLocale.getString("Java home: "), boldStyle);
      doc.insertString(doc.getLength(), AnUtility.clearPath(
          Analyzer.jvm_home) + "\n", regularStyle);
      doc.insertString(doc.getLength(),
          AnLocale.getString("User directory: "), boldStyle);
      doc.insertString(doc.getLength(), AnUtility.clearPath(
          UserPref.getAnalyzerDirPath()) + "\n", regularStyle);
      doc.insertString(doc.getLength(), "gprofng-display-text: ", boldStyle);
      doc.insertString(doc.getLength(), AnUtility.clearPath(
          Analyzer.getInstance().er_print) + "\n", regularStyle);
      doc.insertString(doc.getLength(),
          AnLocale.getString("gprofng-display-text version: "), boldStyle);
      doc.insertString(doc.getLength(), Analyzer.fdversion + "\n", regularStyle);
    } catch (BadLocationException ble) {
    }
    AnUtility.setAccessibleContext(
        infoTextPane.getAccessibleContext(), AnLocale.getString("Info Pane"));
  }

  /** Show Version and Copyright Notice */
  public static void showDialog() throws HeadlessException {
    String aTitle = AnLocale.getString("About gprofng GUI");
    AnDialog2 dialog2 =
        new AnDialog2(AnWindow.getInstance().getFrame(), AnWindow.getInstance().getFrame(), aTitle);
    dialog2.setCustomPanel(new AboutPanel());
    dialog2.setModal(false);
    dialog2.setAlwaysOnTop(true);
    dialog2.getAccessibleContext().setAccessibleName(aTitle);
    dialog2.getAccessibleContext().setAccessibleDescription(aTitle);
    dialog2.getCancelButton().setVisible(false);
    dialog2.pack();
    dialog2.setVisible(true);
    dialog2.getOKButton().requestFocus();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    innerPanel = new javax.swing.JPanel();
    iconLabel = new javax.swing.JLabel();
    infoScrollPane = new javax.swing.JScrollPane();
    infoTextPane = new javax.swing.JTextPane();
    copyrightScrollPane = new javax.swing.JScrollPane();
    copyrightTextArea = new javax.swing.JTextArea();

    setLayout(new java.awt.GridBagLayout());

    innerPanel.setLayout(new java.awt.GridBagLayout());

    iconLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/gprofng/mpmt/icons/performanceAnalyzerSplash.gif"))); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    innerPanel.add(iconLabel, gridBagConstraints);

    infoScrollPane.setPreferredSize(new java.awt.Dimension(300, 180));

    infoTextPane.setEditable(false);
    infoScrollPane.setViewportView(infoTextPane);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    innerPanel.add(infoScrollPane, gridBagConstraints);

    copyrightTextArea.setEditable(false);
    copyrightTextArea.setColumns(20);
    copyrightTextArea.setLineWrap(true);
    copyrightTextArea.setRows(8);
    copyrightTextArea.setMargin(new java.awt.Insets(4, 4, 4, 4));
    copyrightScrollPane.setViewportView(copyrightTextArea);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    innerPanel.add(copyrightScrollPane, gridBagConstraints);

    add(innerPanel, new java.awt.GridBagConstraints());
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JScrollPane copyrightScrollPane;
  private javax.swing.JTextArea copyrightTextArea;
  private javax.swing.JLabel iconLabel;
  private javax.swing.JScrollPane infoScrollPane;
  private javax.swing.JTextPane infoTextPane;
  private javax.swing.JPanel innerPanel;
  // End of variables declaration//GEN-END:variables
}

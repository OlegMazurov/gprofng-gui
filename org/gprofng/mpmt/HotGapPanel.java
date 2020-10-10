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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class HotGapPanel extends JPanel {

  int height = 0;
  private final FuncListDisp funcListDisp;

  public HotGapPanel(FuncListDisp funcListDisp) {
    this.funcListDisp = funcListDisp;
    init(47);
  }

  private void init(int height) {
    this.height = height;
    JPanel buttonPan = new JPanel(new GridBagLayout());
    buttonPan.setBackground(Color.WHITE);
    final JButton upButton = new AnIconButton(AnUtility.goUpIcon);
    final JButton downButton = new AnIconButton(AnUtility.goDownIcon);

    upButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            funcListDisp.find(null, false, true);
            funcListDisp.gap.update();
          }
        });
    downButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            funcListDisp.find(null, true, true);
            funcListDisp.gap.update();
          }
        });
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridy = 0;
    buttonPan.add(upButton, gridBagConstraints);
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    buttonPan.add(downButton, gridBagConstraints);
    Dimension dim = new Dimension(AnHotGap.HOTGAP_WIDTH, height);
    buttonPan.setPreferredSize(dim);

    setLayout(new BorderLayout());
    add(buttonPan, BorderLayout.NORTH);
    final AnHotGap gap = new AnHotGap(funcListDisp, height);
    add(gap, BorderLayout.CENTER);
    funcListDisp.gap = gap; // Hmmm?????
    setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, AnEnvironment.SCROLLBAR_BORDER_COLOR));

    // Previous Hot Line
    KeyStroke keyStrokeUp = KeyboardShortcuts.sourcePreviousHotLineActionShortcut;
    upButton.setToolTipText(
        AnLocale.getString("Previous Hot Line")
            + AnUtility.keyStrokeToStringFormatted(keyStrokeUp));
    // Next Hot Line
    KeyStroke keyStrokeDown = KeyboardShortcuts.sourceNextHotLineActionShortcut;
    downButton.setToolTipText(
        AnLocale.getString("Next Hot Line") + AnUtility.keyStrokeToStringFormatted(keyStrokeDown));
  }

  public void headerheightChanged(int height) {
    if (height < 40) {
      height = 40;
    }
    if (this.height == height) {
      return;
    }
    removeAll();
    init(height);
    funcListDisp.updateGap();
  }
}

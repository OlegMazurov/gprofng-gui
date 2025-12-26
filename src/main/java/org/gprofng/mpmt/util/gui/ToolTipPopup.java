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

package org.gprofng.mpmt.util.gui;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;

public class ToolTipPopup {

  public enum Location {
    NORTH,
    NORTHEAST,
    EAST,
    CENTER
  };

  private Popup tooltipPopup = null;
  private final Component parent;
  private final Component component;
  private final Location location;
  private final boolean bubble;

  private JPanel panel = null;

  public ToolTipPopup(Component parent, Component component, Location location, boolean bubble) {
    this.parent = parent;
    this.component = component;
    this.location = location;
    this.bubble = bubble;
  }

  private JPanel getPanel() {
    if (panel == null) {
      panel = new JPanel();
      panel.setBackground(AnEnvironment.TOOLTIP_POPUP_BACKGROUND_COLOR);
      panel.setBorder(BorderFactory.createLineBorder(AnEnvironment.TOOLTIP_POPUP_BORDER_COLOR, 1));
      panel.setLayout(new GridBagLayout());

      GridBagConstraints gridBagConstraints;

      if (bubble) {
        JLabel iconLabel = new JLabel(AnUtility.bubble_icon);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new Insets(8, 8, 8, 0);
        panel.add(iconLabel, gridBagConstraints);
      }

      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.anchor = GridBagConstraints.WEST;
      gridBagConstraints.gridx = 1;
      gridBagConstraints.insets = new Insets(8, 8, 8, 8);
      panel.add(component, gridBagConstraints);
    }
    return panel;
  }

  /** Show the dialog */
  public void show() {
    if (tooltipPopup != null) {
      return;
    }

    if (!parent.isShowing()) {
      return;
    }

    Point locationOnScreen = parent.getLocationOnScreen();
    int locationx = 0;
    int locationy = 0;
    if (null != location)
      switch (location) {
        case NORTH:
          locationx =
              locationOnScreen.x
                  + parent.getSize().width / 2
                  - getPanel().getPreferredSize().width / 2;
          locationy = locationOnScreen.y - getPanel().getPreferredSize().height + 10;
          break;
        case NORTHEAST:
          locationx = locationOnScreen.x + parent.getSize().width - 1;
          locationy = locationOnScreen.y - getPanel().getPreferredSize().height + 1;
          break;
        case EAST:
          locationx = locationOnScreen.x + parent.getSize().width - 1;
          locationy =
              locationOnScreen.y
                  + parent.getSize().height / 2
                  - getPanel().getPreferredSize().height / 2;
          break;
        case CENTER:
          locationx =
              locationOnScreen.x
                  + parent.getSize().width / 2
                  - getPanel().getPreferredSize().width / 2;
          locationy =
              locationOnScreen.y
                  + parent.getSize().height / 2
                  - getPanel().getPreferredSize().height / 2;
          break;
        default:
          assert false;
          break;
      }

    tooltipPopup =
        PopupFactory.getSharedInstance().getPopup(parent, getPanel(), locationx, locationy);
    tooltipPopup.show();
  }

  /** hide the dialog */
  public void hide() {
    if (tooltipPopup != null) {
      tooltipPopup.hide();
      tooltipPopup = null;
    }
  }

  /**
   * show tooltip for ms milliseconds
   *
   * @param ms ms to show the dialog
   */
  public void show(final int waitms, final int showms) {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            try {
              Thread.sleep(waitms);
            } catch (InterruptedException ie) {
            }
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  @Override
                  public void run() {
                    show();
                  }
                });
            AnUtility.dispatchOnAWorkerThread(
                new Runnable() {
                  @Override
                  public void run() {
                    try {
                      Thread.sleep(showms);
                    } catch (InterruptedException ie) {
                    }
                    AnUtility.dispatchOnSwingThread(
                        new Runnable() {
                          @Override
                          public void run() {
                            hide();
                          }
                        });
                  }
                },
                "tooltippopup2"); // NOI18N
          }
        },
        "tooltippopup1"); // NOI18N
  }
}

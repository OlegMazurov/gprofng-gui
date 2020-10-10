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

package org.gprofng.mpmt.control_panel;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnAction;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.toolbar.ToolBarFiller;
import org.gprofng.mpmt.toolbar.ToolBarSeparator;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.zoomruler.ZoomRulerComponent;
import org.gprofng.mpmt.util.zoomruler.ZoomRulerEvent;
import org.gprofng.mpmt.util.zoomruler.ZoomRulerListener;
import org.gprofng.mpmt.util.zoomruler.ZoomRulerOverlay;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

public class CPToolbar extends JToolBar {

  // components
  private JToggleButton grabButton;
  private CPToolbarHandler handler;
  private final boolean showZoomUndo;
  private final boolean showZoomRedo;
  private final boolean showZoomControls;
  private ZoomRulerComponent hZoomRuler;
  private ZoomRulerComponent vZoomRuler;

  // Actions
  // FIXUP: actions should be moved to TimelineView and the action buttons should be added here in
  // this file.
  private AnAction zoomAction;
  private AnAction undoAction;
  private AnAction redoAction;
  private AnAction upOneAction;
  private AnAction downOneAction;
  private AnAction leftOneAction;
  private AnAction rightOneAction;

  /** Creates a new instance of MPIControlPanel */
  public CPToolbar(
      CPToolbarHandler handler,
      boolean showZoomControls,
      boolean showZoomUndo,
      boolean showZoomRedo) {
    super();
    this.handler = handler;
    this.showZoomUndo = showZoomUndo;
    this.showZoomRedo = showZoomRedo;
    this.showZoomControls = showZoomControls;
    init();
    setOpaque(false);
    setBorder(null);
  }

  private void init() {
    JToolBar tb = this;
    tb.setFloatable(false);

    int mnem;
    String bname;
    String keytext;
    String modtext;
    JComponent tmp;

    AnUtility.setAccessibleContext(getAccessibleContext(), AnLocale.getString("Toolbar"));

    downOneAction =
        new AnAction(
            AnLocale.getString("Down One Event"),
            AnUtility.down_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (downOneAction.isEnabled()) {
                  selectDown();
                }
              }
            });
    downOneAction.setKeyboardShortCut(
        null, "TIMELINE_DOWN", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false));

    leftOneAction =
        new AnAction(
            AnLocale.getString("Left One Event"),
            AnUtility.back_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (leftOneAction.isEnabled()) {
                  selectLeft();
                }
              }
            });
    leftOneAction.setKeyboardShortCut(
        null, "TIMELINE_LEFT", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false));

    rightOneAction =
        new AnAction(
            AnLocale.getString("Right One Event"),
            AnUtility.forw_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (rightOneAction.isEnabled()) {
                  selectRight();
                }
              }
            });
    rightOneAction.setKeyboardShortCut(
        null, "TIMELINE_RIGHT", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false));

    upOneAction =
        new AnAction(
            AnLocale.getString("Up One Event"),
            AnUtility.up_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (upOneAction.isEnabled()) {
                  selectUp();
                }
              }
            });
    upOneAction.setKeyboardShortCut(
        null, "TIMELINE_UP", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false));

    zoomAction =
        new AnAction(
            AnLocale.getString("Zoom to Selected Time Range"),
            AnUtility.mtall_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (zoomAction.isEnabled()) {
                  zoomToCaliper();
                }
              }
            });
    zoomAction.setKeyboardShortCut(
        null, "TIMELINE_ZOOM", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));

    grabButton = new JToggleButton();
    grabButton.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
    mnem = KeyEvent.VK_T; // TimelineView.java installs real handler
    keytext = KeyEvent.getKeyText(mnem);
    bname = AnLocale.getString("Toggle between Zoom and Pan mode") + "  (" + keytext + ")";
    grabButton.setName(bname);
    grabButton.getAccessibleContext().setAccessibleName(bname);
    grabButton.getAccessibleContext().setAccessibleDescription(bname);
    grabButton.setToolTipText(bname);
    //        grabButton.setMnemonic(mnem);
    grabButton.setEnabled(true);
    grabButton.setIcon(AnUtility.panhand_icon);
    grabButton.setMargin(AnVariable.buttonMargin);
    grabButton.setBorderPainted(false);
    //        grabButton.setSelected(true);
    grabButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            // toggle between rubberbanding and grabbing mode
            toggleMouseMode();
          }
        });

    undoAction =
        new AnAction(
            AnLocale.getString("Undo Zoom Action"),
            AnUtility.undo_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (undoAction.isEnabled()) {
                  undoZoom();
                }
              }
            });
    undoAction.setKeyboardShortCut(
        null, "TIMELINE_UNDO", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, false));

    redoAction =
        new AnAction(
            AnLocale.getString("Redo Zoom Action"),
            AnUtility.redo_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (redoAction.isEnabled()) {
                  redoZoom();
                }
              }
            });
    redoAction.setKeyboardShortCut(
        null,
        "TIMELINE_REDO",
        KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.SHIFT_DOWN_MASK, false));

    if (showZoomControls) {
      vZoomRuler =
          new ZoomRulerComponent(
              ZoomRulerOverlay.VERTICAL_ORIENTATION,
              2, // ZoomRulerOverlay.RULER_LD_DEFAULT,
              false,
              false,
              true);
      AnUtility.setAccessibleContext(
          vZoomRuler.getAccessibleContext(), AnLocale.getString("Vertical Zoom"));
      vZoomRuler.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
      final ZoomRulerListener vzrl =
          new ZoomRulerListener() {
            @Override
            public void zoomChanged(ZoomRulerEvent event) {
              if (event.isUndo()) {
                undoZoom();
              } else if (event.isRedo()) {
                redoZoom();
              } else if (event.isLevelChange()) {
                int lev = event.getZoomLevel();
                genericVZoomToLevel(lev);
              } else if (event.isLevelPlus()) {
                genericVZoomIn();
              } else if (event.isLevelMinus()) {
                genericVZoomOut();
              } else if (event.isReset()) {
                //                    resetZoom();
              } else if (event.isPan()) {
                //                    if (event.getDirection() == ZoomRulerEvent.PAN_UP)
                //                        panUp();
                //                    else if (event.getDirection() == ZoomRulerEvent.PAN_RIGHT)
                //                        panRight();
                //                    else if (event.getDirection() == ZoomRulerEvent.PAN_DOWN)
                //                        panDown();
                //                    else // PAN_LEFT
                //                        panLeft();
              }
            }
          };
      vZoomRuler.addZoomRulerListener(vzrl);
      tmp = vZoomRuler;
      //      tmp.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
      bname = AnLocale.getString("Adjust the Veritcal Zoom");
      keytext = KeyEvent.getKeyText(KeyEvent.VK_PLUS); // TimelineView.java installs real handler
      modtext = KeyEvent.getKeyModifiersText(InputEvent.CTRL_MASK);
      bname += "  (" + modtext + "-" + keytext;
      keytext = KeyEvent.getKeyText(KeyEvent.VK_MINUS); // TimelineView.java installs real handler
      bname += ", " + modtext + "-" + keytext + ")";
      //        tmp.getAccessibleContext().setAccessibleName(bname);
      //        tmp.getAccessibleContext().setAccessibleDescription(bname);
      tmp.setToolTipText(bname);
      //        tmp.setMnemonic(mnem);
      //        tmp.setEnabled(true);

      hZoomRuler =
          new ZoomRulerComponent(
              ZoomRulerOverlay.HORIZONTAL_ORIENTATION,
              ZoomRulerOverlay.RULER_LD_DEFAULT,
              true,
              false,
              false);
      AnUtility.setAccessibleContext(
          hZoomRuler.getAccessibleContext(), AnLocale.getString("Horizontal Zoom"));
      hZoomRuler.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
      final ZoomRulerListener hzrl =
          new ZoomRulerListener() {
            @Override
            public void zoomChanged(ZoomRulerEvent event) {
              if (event.isUndo()) {
                undoZoom();
              } else if (event.isRedo()) {
                redoZoom();
              } else if (event.isLevelChange()) {
                int lev = event.getZoomLevel();
                genericZoomToLevel(lev);
              } else if (event.isLevelPlus()) {
                genericZoomIn();
              } else if (event.isLevelMinus()) {
                genericZoomOut();
              } else if (event.isReset()) {
                resetZoom();
              } else if (event.isPan()) {
                //                    if (event.getDirection() == ZoomRulerEvent.PAN_UP)
                //                        panUp();
                //                    else if (event.getDirection() == ZoomRulerEvent.PAN_RIGHT)
                //                        panRight();
                //                    else if (event.getDirection() == ZoomRulerEvent.PAN_DOWN)
                //                        panDown();
                //                    else // PAN_LEFT
                //                        panLeft();
              }
            }
          };
      hZoomRuler.addZoomRulerListener(hzrl);
      tmp = hZoomRuler;
      //      tmp.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
      bname = AnLocale.getString("Adjust the Horizontal Zoom");
      keytext = KeyEvent.getKeyText(KeyEvent.VK_PLUS); // TimelineView.java installs real handler
      bname += "  (" + keytext;
      keytext = KeyEvent.getKeyText(KeyEvent.VK_MINUS); // TimelineView.java installs real handler
      bname += ", " + keytext + ")";
      //        tmp.getAccessibleContext().setAccessibleName(bname);
      //        tmp.getAccessibleContext().setAccessibleDescription(bname);
      tmp.setToolTipText(bname);
      //        tmp.setMnemonic(mnem);
      //        tmp.setEnabled(true);

      tb.add(vZoomRuler);
      tb.add(new ToolBarFiller(2));
      tb.add(hZoomRuler);
      tb.add(new ToolBarFiller(2));
      tb.add(zoomAction.createActionButton());
    } // showZoomControls
    if (showZoomUndo) {
      tb.add(undoAction.createActionButton());
    }
    if (showZoomRedo) {
      tb.add(redoAction.createActionButton());
    }
    if (showZoomControls || showZoomUndo || showZoomRedo) {
      tb.add(new ToolBarSeparator(7, 6));
    }
    tb.add(upOneAction.createActionButton());
    tb.add(downOneAction.createActionButton());
    tb.add(leftOneAction.createActionButton());
    tb.add(rightOneAction.createActionButton());
    //        tb.add(grabButton); TBR?
    add(AnWindow.getInstance().getFunctionColorsAction().createActionButton());
    tb.revalidate();

    KeyStroke ks = KeyboardShortcuts.contextMenuActionShortcut;
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, ks);
    this.getActionMap()
        .put(
            ks,
            new AbstractAction() {

              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != handler) {
                  handler.showContextMenu(ev);
                }
              }
            });
  }

  // --- Public API ---
  /** Turn on navigation controls */
  public void enableNavigationButtons(
      boolean bleft,
      boolean bright,
      boolean bup,
      boolean bdown,
      boolean bfind) { // YXXX remove bfind?
    leftOneAction.setEnabled(bleft);
    rightOneAction.setEnabled(bright);
    upOneAction.setEnabled(bup);
    downOneAction.setEnabled(bdown);
    //        zoomAction.setEnabled(bfind);
  }

  public void enableZoomToCaliperButton(boolean bfind) {
    zoomAction.setEnabled(bfind);
  }

  public void setZoomLevel(int level) {
    if (hZoomRuler == null) {
      return; // weird, caller thinks CPToolbar showZoomControls is true?
    }
    hZoomRuler.setZoomLevel(level);
  }

  public void setVZoomLevel(int level) {
    if (vZoomRuler == null) {
      return; // weird, caller thinks CPToolbar showZoomControls is true?
    }
    vZoomRuler.setZoomLevel(level);
  }

  public void setNumberOfLevels(int levels) {
    if (hZoomRuler == null) {
      return; // weird, caller thinks CPToolbar showZoomControls is true?
    }
    hZoomRuler.setNumberOfLevels(levels);
  }

  public void setVNumberOfLevels(int levels) {
    if (vZoomRuler == null) {
      return; // weird, caller thinks CPToolbar showZoomControls is true?
    }
    vZoomRuler.setNumberOfLevels(levels);
  }

  /** Turn on navigation controls */
  public void enableGrabButton(boolean grab) {
    grabButton.setEnabled(grab);
  }

  public void addToolBarButton(JButton button) {
    JToolBar tb = this;
    tb.add(button);
    tb.revalidate();
  }

  public void enableZoomUndoButton(boolean bfind) {
    undoAction.setEnabled(bfind);
  }

  public void enableZoomRedoButton(boolean bfind) {
    redoAction.setEnabled(bfind);
  }

  // --- wrappers for handler; allows handler to be dynamically replaced
  private void selectLeft() {
    handler.selectLeft();
  }

  private void selectRight() {
    handler.selectRight();
  }

  private void selectDown() {
    handler.selectDown();
  }

  private void selectUp() {
    handler.selectUp();
  }

  private void selectFind() {
    handler.selectFind();
  }

  private void selectFirst() { // no button for this at this time
    handler.selectFirst();
  }

  private void toggleMouseMode() {
    handler.toggleMouseMode();
  }

  private void genericZoomIn() {
    handler.genericZoomIn();
  }

  private void genericZoomOut() {
    handler.genericZoomOut();
  }

  private void genericZoomToLevel(int n) {
    handler.genericZoomToLevel(n);
  }

  private void genericVZoomIn() {
    handler.genericVZoomIn();
  }

  private void genericVZoomOut() {
    handler.genericVZoomOut();
  }

  private void genericVZoomToLevel(int n) {
    handler.genericVZoomToLevel(n);
  }

  private void resetZoom() {
    handler.resetZoom();
  }

  private void zoomToCaliper() {
    handler.zoomToCaliper();
  }

  private void undoZoom() {
    handler.undoZoom();
  }

  private void redoZoom() {
    handler.redoZoom();
  }

  private void chooseColors() {
    handler.chooseColors();
  }
}

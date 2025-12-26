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

package org.gprofng.mpmt.navigation;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

public class ViewPanel extends javax.swing.JPanel
    implements MouseListener, MouseMotionListener, Transferable {

  private static Image removeImage = null;
  private static Image removeImageFocused = null;
  private Image buttonImage = null;
  private Image selectedArrowImage = null;
  private static int removeImageWidth = 0;
  private static int removeImageHeight = 0;
  private int removeImageXOffset = 4;
  private int removeImageYOffset = 8;
  private static int selectedArrowImageWidth = 0;
  private static int selectedArrowImageHeight = 0;
  private int selectedArrowXOffset = 4;
  private int selectedArrowYOffset = 8;
  private static int scrollbarWidth = 0;
  private View view;
  private ViewsPanel sidePanel;
  private boolean focused = false;
  private boolean selected = false;
  private boolean keyboardFocus = false;
  private boolean shown;
  private int position;
  private static Integer panelHeight = null;

  public ViewPanel(View view) {
    initComponents();
    this.view = view;
    setBorder(null);
    nameLabel.setText(view.getDisplayName());
    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
    AnUtility.setAccessibleContext(nameLabel.getAccessibleContext(), nameLabel.getText());
    //        nameLabel.setIcon(new javax.swing.ImageIcon(view.getImage()));
    setBackground(
        AnEnvironment.NAVIGATION_PANEL_BACKGROUND_COLOR /*AnEnvironment.DEFAULT_PANEL_BACKGROUND*/);
    addMouseListener(this);
    addMouseMotionListener(this);

    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
              final Point loc1 = MouseInfo.getPointerInfo().getLocation();
              final Point loc2 = getLocationOnScreen();
              sidePanel.showViewsConfigurationPopup(
                  ViewPanel.this, loc1.x - loc2.x + 5, loc1.y - loc2.y + 5);
            }
          }
        });

    // A11Y
    KeyStroke keyStroke = KeyboardShortcuts.contextMenuActionShortcut;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, keyStroke);
    getActionMap()
        .put(
            keyStroke,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                final Point loc1 = MouseInfo.getPointerInfo().getLocation();
                final Point loc2 = getLocationOnScreen();
                sidePanel.showViewsConfigurationPopup(
                    ViewPanel.this, loc1.x - loc2.x + 5, loc1.y - loc2.y + 5);
              }
            });

    this.shown = false;
    if (panelHeight == null) {
      panelHeight = getPreferredSize().height;
    }
  }

  public View getView() {
    return view;
  }

  public void setSidePanel(ViewsPanel sidePanel) {
    this.sidePanel = sidePanel;
    addMouseMotionListener(new MouseDraggedListener());
    setTransferHandler(new DADTransferHandler());
    setDropTarget(new DropTarget(this, new PanelDropTargetListener(sidePanel)));
  }

  // @Override
  @Override
  public void mouseClicked(MouseEvent me) {}

  // @Override
  @Override
  public void mouseDragged(MouseEvent me) {}

  // @Override
  @Override
  public void mouseEntered(MouseEvent me) {
    focused = true;
    refresh();
  }

  // @Override
  @Override
  public void mouseExited(MouseEvent me) {
    focused = false;
    refresh();
  }

  // @Override
  @Override
  public void mouseReleased(MouseEvent me) {}

  public void setSelected(boolean selected) {
    this.selected = selected;
    refresh();
  }

  private void refresh() {
    // Set background
    Color backgroundColor;
    if (isSelected()) {
      backgroundColor = AnEnvironment.VIEW_SELECTED_BACKGROUND_COLOR;
    } else if (focused) {
      backgroundColor = AnEnvironment.VIEW_FOCUSED_BACKGROUND_COLOR;
    } else {
      backgroundColor = AnEnvironment.NAVIGATION_PANEL_BACKGROUND_COLOR;
    }
    setBackground(backgroundColor);

    repaint();
  }

  // @Override
  @Override
  public void mousePressed(MouseEvent me) {
    if (me.getButton() == MouseEvent.BUTTON1) {
      int scrollbarOffset = 0; // getScrollbarOffset();
      int x = me.getPoint().x;
      int y = me.getPoint().y;
      if (x >= getSize().width - removeImageWidth - removeImageXOffset - scrollbarOffset
          && x <= getSize().width - removeImageXOffset - scrollbarOffset
          && y >= removeImageYOffset
          && y <= removeImageHeight + removeImageYOffset) {
        closeButtonActionPerformed(null);
      } else {
        sidePanel.setSelected(this, true);
      }
    }
  }

  @Override
  public JToolTip createToolTip() {
    JToolTip toolTip = super.createToolTip();
    toolTip.setBackground(AnEnvironment.TOOLTIP_VIEW_BACKGROUND_COLOR);
    return toolTip;
  }

  // @Override
  @Override
  public void mouseMoved(MouseEvent me) {
    Image newButtonImage;
    int scrollbarOffset = 0; // getScrollbarOffset();
    int x = me.getPoint().x;
    int y = me.getPoint().y;
    if (x >= getSize().width - removeImageWidth - removeImageXOffset - scrollbarOffset
        && x <= getSize().width - removeImageXOffset - scrollbarOffset
        && y >= removeImageYOffset
        && y <= removeImageHeight + removeImageYOffset) {
      newButtonImage = getRemoveTabFocusedImage();
      setToolTipText(AnLocale.getString("Remove this view"));
    } else {
      newButtonImage = getRemoveTabImage();
      //            setToolTipText(getView().getLongDescr());
    }
    if (newButtonImage != getRemoveButtonImage()) {
      buttonImage = newButtonImage;
      repaint();
    }
  }

  //    private boolean isScrollbarShowing() {
  //        return sidePanel.getScrollPane().getVerticalScrollBar().isShowing();
  //    }

  //    private int getScrollbarWidth() {
  //        if (scrollbarWidth == 0) {
  //            scrollbarWidth = sidePanel.getScrollPane().getVerticalScrollBar().getWidth();
  //        }
  //        return scrollbarWidth;
  //    }

  //    private int getScrollbarOffset() {
  //        if (isScrollbarShowing()) {
  //            return getScrollbarWidth();
  //        }
  //        else {
  //            return 0;
  //        }
  //    }
  @Override
  public void paint(Graphics g) {
    if (selected || focused) {
      Graphics2D g2d = (Graphics2D) g;
      //            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      // RenderingHints.VALUE_ANTIALIAS_ON);
      Color topColor =
          selected ? AnEnvironment.VIEW_SELECTED_TOP_COLOR : AnEnvironment.VIEW_FOCUSED_TOP_COLOR;
      Color bottomColor =
          selected
              ? AnEnvironment.VIEW_SELECTED_BOTTOM_COLOR
              : AnEnvironment.VIEW_FOCUSED_BOTTOM_COLOR;
      GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
      g2d.setPaint(gp);
      g2d.fillRect(0, 0, getWidth(), getHeight());

      // Set border
      Color borderColor;
      if (selected || focused) {
        borderColor = AnEnvironment.VIEW_SELECTED_BORDER_COLOR;
      } else {
        borderColor = AnEnvironment.NAVIGATION_PANEL_BACKGROUND_COLOR;
      }
      g.setColor(borderColor);
      g.drawLine(0, 0, getWidth(), 0);
      g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

      setOpaque(false);
    } else {
      setOpaque(true);
    }
    super.paint(g);
    int scrollbarOffset =
        0; // getScrollbarOffset(); // FIXUP: doesn't affect rendering here anymore ?
    if (focused) {
      g.drawImage(
          getRemoveButtonImage(),
          getSize().width - removeImageWidth - removeImageXOffset - scrollbarOffset,
          removeImageYOffset,
          this);
    } else if (selected) {
      g.drawImage(
          getSelectedArrowImage(),
          getSize().width - selectedArrowImageWidth - selectedArrowXOffset - scrollbarOffset,
          selectedArrowYOffset,
          this);
    }
    String labelText = displayText(g, view.getDisplayName(), true);
    nameLabel.setText(labelText);
    setToolTipText(null);
    if (labelText.endsWith("...")) {
      setToolTipText(view.getDisplayName());
    }
    if (keyboardFocus) {
      g.setColor(new Color(180, 180, 180));
      g.drawRect(3, 3, getWidth() - 7, getHeight() - 7);
    }
    //        int w = getWidth();
    //        int h = getHeight();
    //        g.setColor(/*selected ? AnEnvironment.DEFAULT_PANEL_BACKGROUND : */selectedBorder);
    //        g.drawLine(w-1, 0, w-1, h);
  }

  private String displayText(Graphics g, String name, boolean shortName) {
    int maxTextWidth = getWidth();
    maxTextWidth -= 20; // make room for spaces around text
    //        if (shortName) {
    //            maxTextWidth -= 15; // make room for '>' arrow or 'x'
    //        }
    //        if (isScrollbarShowing()) {
    //            maxTextWidth -= getScrollbarWidth();
    //        }
    FontMetrics fontMetrics = g.getFontMetrics(nameLabel.getFont());
    int nameWidth = fontMetrics.stringWidth(name);

    String ret = name;
    if (nameWidth > maxTextWidth) {
      ret = shortenText(fontMetrics, name, maxTextWidth);
    }

    return ret;
  }

  private String shortenText(FontMetrics fontMetrics, String name, int maxTextWidth) {
    String ret = name;
    for (int newLength = name.length(); newLength > 0; newLength--) {
      ret = name.substring(0, newLength) + "...";
      ;
      if (fontMetrics.stringWidth(ret) < maxTextWidth) {
        break;
      }
    }
    return ret;
  }

  private Image getRemoveButtonImage() {
    if (buttonImage == null) {
      buttonImage = getRemoveTabImage();
    }
    return buttonImage;
  }

  private Image getRemoveTabImage() {
    if (removeImage == null) {
      removeImage = AnUtility.removeIcon.getImage();
      removeImageWidth = removeImage.getWidth(this);
      removeImageHeight = removeImage.getHeight(this);
    }
    return removeImage;
  }

  private Image getRemoveTabFocusedImage() {
    if (removeImageFocused == null) {
      removeImageFocused = AnUtility.removeFocusedIcon.getImage();
    }
    return removeImageFocused;
  }

  private Image getSelectedArrowImage() {
    if (selectedArrowImage == null) {
      URL url = this.getClass().getResource("/org/gprofng/mpmt/icons/nav_bar_arrow.png");
      if (url != null) {
        ImageIcon imageIcon = new ImageIcon(url);
        selectedArrowImage = imageIcon.getImage();
        selectedArrowImageWidth = selectedArrowImage.getWidth(this);
        selectedArrowImageHeight = selectedArrowImage.getHeight(this);
      }
    }
    return selectedArrowImage;
  }

  protected static int getPanelHeight() {
    if (panelHeight != null) {
      return panelHeight;
    } else {
      return 27;
    }
  }

  /**
   * @return the shown
   */
  public boolean isShown() {
    return shown;
  }

  /**
   * @param shown the shown to set
   */
  public void setShown(boolean shown) {
    this.shown = shown;
  }

  /**
   * @return the position
   */
  public int getPosition() {
    return position;
  }

  /**
   * @param position the position to set
   */
  public void setPosition(int position) {
    this.position = position;
  }

  class MouseDraggedListener extends MouseMotionAdapter {

    // @Override
    @Override
    public void mouseDragged(MouseEvent e) {
      JComponent c = (JComponent) e.getSource();
      TransferHandler handler = c.getTransferHandler();
      handler.exportAsDrag(c, e, TransferHandler.COPY);
    }
  }

  private static DataFlavor dadDataFlavor = null;

  public static DataFlavor getDADDataFlavor() throws Exception {
    if (dadDataFlavor == null) {
      dadDataFlavor =
          new DataFlavor(
              DataFlavor.javaJVMLocalObjectMimeType
                  + ";class=org.gprofng.mpmt.navigation.ViewPanel");
    }

    return dadDataFlavor;
  }

  // @Override
  @Override
  public Object getTransferData(DataFlavor flavor) {
    DataFlavor thisFlavor;

    try {
      thisFlavor = getDADDataFlavor();
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      return null;
    }

    if (thisFlavor != null && flavor.equals(thisFlavor)) {
      return ViewPanel.this;
    }

    return null;
  }

  // @Override
  @Override
  public DataFlavor[] getTransferDataFlavors() {
    DataFlavor[] flavors = {null};

    try {
      flavors[0] = getDADDataFlavor();
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      return null;
    }

    return flavors;
  }

  // @Override
  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    DataFlavor[] flavors = {null};
    try {
      flavors[0] = getDADDataFlavor();
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      return false;
    }

    for (DataFlavor f : flavors) {
      if (f.equals(flavor)) {
        return true;
      }
    }

    return false;
  }

  class DADTransferHandler extends TransferHandler {

    public DADTransferHandler() {
      super();
    }

    // @Override()
    @Override
    public Transferable createTransferable(JComponent c) {
      if (c instanceof ViewPanel) {
        Transferable tip = (ViewPanel) c;
        return tip;
      }
      return null;
    }

    // @Override()
    @Override
    public int getSourceActions(JComponent c) {
      if (c instanceof ViewPanel) {
        return TransferHandler.COPY;
      }

      return TransferHandler.NONE;
    }
  }

  // @Override
  @Override
  public String toString() {
    return nameLabel.getText();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    buttonGroup1 = new javax.swing.ButtonGroup();
    nameLabel = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    nameLabel.setText("Functions");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 6, 6, 0);
    add(nameLabel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {
    AnWindow.getInstance()
        .getSettings()
        .getViewsSetting()
        .toggleTab(getView().getName(), AnWindow.getInstance().getViews().getCurrentViewName());
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.ButtonGroup buttonGroup1;
  private javax.swing.JLabel nameLabel;
  // End of variables declaration//GEN-END:variables

  public boolean isSelected() {
    return selected;
  }

  public boolean isFocused() {
    return focused;
  }

  public boolean hasKeyboardFocus() {
    return keyboardFocus;
  }

  public void setKeyboardFocused(boolean keyboardFocused) {
    this.keyboardFocus = keyboardFocused;
  }
}

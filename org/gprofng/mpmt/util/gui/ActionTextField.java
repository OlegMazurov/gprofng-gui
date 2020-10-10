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


package org.gprofng.mpmt.util.gui;

import org.gprofng.mpmt.AnLocale;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JTextField;

/**
 * @author tpreisle
 */
public class ActionTextField extends JTextField implements MouseListener, MouseMotionListener {
  private String toolTipText = null;
  private String actionToolTipText = null;
  private String fullText = null;
  private String displayText = null;
  protected boolean focused = false;
  private static Image removeImage = null;
  private static Image removeImageFocused = null;
  private Image buttonImage = null;
  private static int removeImageWidth = 0;
  private static int removeImageHeight = 0;
  private static int removeImageXOffset = 2;
  private static int removeImageYOffset = 3;
  private ToolTipPopup tooltipPopup = null;

  public ActionTextField() {
    init();
  }

  public ActionTextField(String text) {
    setText(text);
    init();
  }

  private void init() {
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  @Override
  public void setToolTipText(String text) {
    this.toolTipText = text;
    super.setToolTipText(text); // To change body of generated methods, choose Tools | Templates.
  }

  public void setActionToolTipText(String text) {
    this.actionToolTipText = text;
  }

  @Override
  public void setText(String text) {
    fullText = text;
  }

  @Override
  public void paint(Graphics g) {
    String newDisplayText = displayText(g, fullText);
    if (displayText == null || !displayText.equals(newDisplayText)) {
      displayText = newDisplayText;
      super.setText(displayText);
    }
    super.paint(g);
    if (focused) {
      removeImageYOffset = (getSize().height - removeImageHeight) / 2;
      g.drawImage(
          getRemoveButtonImage(),
          getSize().width - removeImageWidth - removeImageXOffset,
          removeImageYOffset,
          this);
    }
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(0, 0);
  }

  @Override
  public String getText() {
    return fullText;
  }

  private String displayText(Graphics g, String text) {
    int maxTextWidth = getWidth();
    maxTextWidth -= 20; // make room for spaces around text
    FontMetrics fontMetrics = g.getFontMetrics(getFont());
    int nameWidth = fontMetrics.stringWidth(text);

    String ret = text;
    if (nameWidth > maxTextWidth) {
      ret = shortenText(fontMetrics, text, maxTextWidth) + "..."; // NOI18N
    }

    return ret;
  }

  private String shortenText(FontMetrics fontMetrics, String text, int maxTextWidth) {
    String ret = text;
    for (int newLength = text.length(); newLength > 0; newLength--) {
      ret = text.substring(0, newLength);
      if (fontMetrics.stringWidth(ret) < maxTextWidth) {
        break;
      }
    }
    return ret;
  }

  // @Override
  @Override
  public void mousePressed(MouseEvent me) {}

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
      newButtonImage = getRemoveFocusedImage();
      if (actionToolTipText != null) {
        super.setToolTipText(actionToolTipText);
      } else {
        super.setToolTipText(AnLocale.getString("Remove"));
      }

    } else {
      newButtonImage = getRemoveUnfocusedImage();
      super.setToolTipText(toolTipText);
    }
    if (newButtonImage != getRemoveButtonImage()) {
      buttonImage = newButtonImage;
      repaint(100);
    }
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
  public void mouseReleased(MouseEvent me) {
    int scrollbarOffset = 0; // getScrollbarOffset();
    int x = me.getPoint().x;
    int y = me.getPoint().y;
    if (x >= getSize().width - removeImageWidth - removeImageXOffset - scrollbarOffset
        && x <= getSize().width - removeImageXOffset - scrollbarOffset
        && y >= removeImageYOffset
        && y <= removeImageHeight + removeImageYOffset) {
      removeButtonActionPerformed(null);
      me.consume();
    }
  }

  private void refresh() {
    repaint(100);
  }

  protected Image getRemoveButtonImage() {
    if (buttonImage == null) {
      buttonImage = getRemoveUnfocusedImage();
    }
    return buttonImage;
  }

  protected Image getRemoveUnfocusedImage() {
    if (removeImage == null) {
      removeImage = AnUtility.removeIcon.getImage();
      removeImageWidth = removeImage.getWidth(this);
      removeImageHeight = removeImage.getHeight(this);
    }
    return removeImage;
  }

  protected Image getRemoveFocusedImage() {
    if (removeImageFocused == null) {
      removeImageFocused = AnUtility.removeFocusedIcon.getImage();
    }
    return removeImageFocused;
  }

  protected void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {
    ActionListener[] actionListeners = getActionListeners();
    for (ActionListener actionListener : actionListeners) {
      actionListener.actionPerformed(evt);
    }
  }
}

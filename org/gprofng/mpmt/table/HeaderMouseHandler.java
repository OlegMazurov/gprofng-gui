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


package org.gprofng.mpmt.table;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class HeaderMouseHandler extends MouseAdapter {

  private MetricPanel metricPanel;
  private AttributePanel mouseOverAttributePanel = null;

  public HeaderMouseHandler(MetricPanel metricPanel) {
    this.metricPanel = metricPanel;
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    //            System.out.println("mouseEntered " + e.getComponent());
    metricPanel.mouseEntered(e);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    //            System.out.println("mouseExited " + e.getComponent());
    metricPanel.mouseExited(e);
    if (mouseOverAttributePanel != null) {
      mouseOverAttributePanel.mouseExited(null);
      mouseOverAttributePanel = null;
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    //            System.out.println("mouseClicked " + " " + e.getX() + " " + e.getComponent());
    if (e.getButton() == MouseEvent.BUTTON3) {
      metricPanel.showPopup(e);
    } else {
      AttributePanel attributePanel = findAttributePanel(e);
      //                System.out.println(attributePanel != null ? attributePanel.column : "-");
      if (attributePanel != null) {
        attributePanel.mouseClicked(null);
      }
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    //            System.out.println("mouseMoved " + " " + e.getX() + " " + e.getComponent());
    AttributePanel attributePanel = findAttributePanel(e);
    //            System.out.println(attributePanel != null ? attributePanel.column : "-");
    if (attributePanel != null) {
      if (attributePanel != mouseOverAttributePanel) {
        if (mouseOverAttributePanel != null) {
          mouseOverAttributePanel.mouseExited(null);
          mouseOverAttributePanel = null;
        }
      }
      attributePanel.mouseEntered(null);
      mouseOverAttributePanel = attributePanel;
    }
  }

  private AttributePanel findAttributePanel(MouseEvent e) {
    int x = e.getX();
    AttributePanel attributePanel = null;
    if (e.getComponent() instanceof AttributePanel) {
      attributePanel = (AttributePanel) e.getComponent();
    } else if (e.getComponent() instanceof ExclusiveOrInclusivePanel) {
      ExclusiveOrInclusivePanel exclusiveOrInclusivePanel =
          (ExclusiveOrInclusivePanel) e.getComponent();
      attributePanel = exclusiveOrInclusivePanel.findAttributePanel(x);
    } else if (e.getComponent() instanceof MetricPanel) {
      ExclusiveOrInclusivePanel exclusiveOrInclusivePanel =
          metricPanel.getExclusiveAndInclusivePanel().findExclusiveOrInclusivePanel(x);
      if (exclusiveOrInclusivePanel != null) {
        // Non Name metrics
        int offset =
            metricPanel.getExclusiveAndInclusivePanel().findExclusiveOrInclusivePanelOffset(x);
        attributePanel = exclusiveOrInclusivePanel.findAttributePanel(offset);
      } else {
        // Name metric
        attributePanel = metricPanel.getExclusiveAndInclusivePanel().findAttributePanel();
      }
    }
    return attributePanel;
  }
}

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

import org.gprofng.mpmt.AnWindow;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

public class MetricPanelDropTargetListener implements DropTargetListener {

  public MetricPanelDropTargetListener() {}

  // @Override
  @Override
  public void dragEnter(DropTargetDragEvent dtde) {}

  // @Override
  @Override
  public void dragOver(DropTargetDragEvent dtde) {}

  // @Override
  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {}

  // @Override
  @Override
  public void dragExit(DropTargetEvent dte) {}

  @Override
  public void drop(DropTargetDropEvent dtde) {
    DataFlavor dadPanelFlavor = null;

    Object transferableObj = null;
    Component targetComponent = null;
    Transferable transferable = null;

    try {
      dadPanelFlavor = MetricPanel.getDADDataFlavor();

      transferable = dtde.getTransferable();
      DropTargetContext c = dtde.getDropTargetContext();
      targetComponent = c.getComponent();

      if (transferable.isDataFlavorSupported(dadPanelFlavor)) {
        transferableObj = dtde.getTransferable().getTransferData(dadPanelFlavor);
      }
    } catch (Exception e) {
    }

    if (transferableObj == null) {
      return;
    }

    MetricPanel droppedPanel = (MetricPanel) transferableObj;
    MetricPanel targetPanel = (MetricPanel) targetComponent;
    //        System.out.println("droppedPanel: " + droppedPanel.dump());
    //        System.out.println("targetPanel: " + targetPanel.dump());
    //        System.out.println("location: " + dtde.getLocation());

    String metricNameDropping = droppedPanel.getAnMetric().getComd();
    String metricNameTarget = targetPanel.getAnMetric().getComd();
    boolean before = dtde.getLocation().x < targetPanel.getPreferredSize().getWidth() / 2;

    //        System.out.println("metricNameDropping: " + metricNameDropping);
    //        System.out.println("metricNameTarget: " + metricNameTarget);
    //        System.out.println("targetPanel.getPreferredSize(): " +
    // targetPanel.getPreferredSize());
    //        System.out.println("before: " + before);

    AnWindow.getInstance()
        .getSettings()
        .getMetricsSetting()
        .setMetricOrder(this, metricNameDropping, metricNameTarget, before);
  }
}

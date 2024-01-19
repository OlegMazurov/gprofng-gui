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

package org.gprofng.mpmt.compare;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

abstract class AnTransferHandler extends TransferHandler {
  protected abstract String exportString(JComponent c);

  protected abstract void importString(JComponent c, String str);

  protected abstract void cleanup(JComponent c, boolean remove);

  protected Transferable createTransferable(JComponent c) {
    return new StringSelection(exportString(c));
  }

  public int getSourceActions(JComponent c) {
    debug("AnTransferHandler::getSourceActions:");
    return COPY_OR_MOVE;
  }

  public boolean importData(JComponent c, Transferable t) {
    debug("AnTransferHandler::importData:");
    if (canImport(c, t.getTransferDataFlavors())) {
      try {
        String str = (String) t.getTransferData(DataFlavor.stringFlavor);
        importString(c, str);
        return true;
      } catch (UnsupportedFlavorException ex) {
        debug("AnTransferHandler::importData: " + ex);
      } catch (IOException ex) {
        debug("AnTransferHandler::importData: " + ex);
      }
    }
    return false;
  }

  protected void exportDone(JComponent c, Transferable data, int action) {
    debug("AnTransferHandler::exportDone: " + (action == MOVE) + " " + action);
    debug("AnTransferHandler::exportDone: " + data);
    cleanup(c, action == MOVE);
  }

  public boolean canImport(JComponent c, DataFlavor[] flavors) {
    for (int i = 0; i < flavors.length; i++) {
      if (DataFlavor.stringFlavor.equals(flavors[i])) {
        debug("AnTransferHandler::canImport: true");
        return true;
      }
    }
    debug("AnTransferHandler::canImport: false");
    return false;
  }

  protected void debug(final String msg) {
    // System.out.println(msg);
  }
}

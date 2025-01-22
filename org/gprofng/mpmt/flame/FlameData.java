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

package org.gprofng.mpmt.flame;

import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FlameData {

  private FlameView flameView;
  private AnMetric anMetric = null;
  private FlameRow[] flameRows = null;
  private Map<Long, StackState> functionIdNameMap = null;
  private FlameBlock baseFlameBlock = null;
  private FlameBlock mouseOverFlameBlock = null;
  private FlameBlock selectedFlameBlock = null;

  private static Object selectedObjectLock = new Object();
  private static UpdateSelectedObjectThread selectedObjectThread;

  protected FlameData(FlameView flameView, AnMetric anMetric, int rowCount) {
    this.flameView = flameView;
    this.anMetric = anMetric;
    flameRows = new FlameRow[rowCount];
  }

  protected void rowDone(FlameRow flameRow) {
    flameRows[flameRow.getRow()] = flameRow;
    if (flameRow.getRow() == 0) {
      if (!flameRow.getFlameBlocks().isEmpty() && !flameRows[0].getFlameBlocks().isEmpty()) {
        baseFlameBlock = getTotalFlameBlock();
        baseFlameBlock.resetPaintBlock();
        baseFlameBlock.setPaintBlock(true);
        baseFlameBlock.setFlameRow(flameRow);
        flameRow.setFirstPaintedFlameBlock(baseFlameBlock);
        setBaseFlameBlock(baseFlameBlock);
        flameView.getFlameToolBar().initSetBaseStack();
        flameView.getFlameToolBar().pushToSetBaseStack(baseFlameBlock);
      }
    }
  }

  protected FlameBlock getTotalFlameBlock() {
    if (flameRows[0].getFlameBlocks().isEmpty()) {
      return null;
    } else {
      return flameRows[0].getFlameBlocks().get(0); // Only one block here!!!!!!
    }
  }

  protected FlameRow[] getFlameRows() {
    return flameRows;
  }

  protected boolean allRowsDone() {
    boolean allDone = false;
    if (flameRows != null) {
      allDone = true;
      for (FlameRow flameRow : flameRows) {
        if (flameRow == null) {
          allDone = false;
          break;
        }
      }
    }
    return allDone;
  }

  protected void resetReadyToPaint() {
    for (FlameRow flameRow : flameRows) {
      if (flameRow != null) {
        flameRow.resetReadyToPaint();
      }
    }
  }

  protected void resetBlockColors() {
    for (FlameRow flameRow : flameRows) {
      if (flameRow != null) {
        flameRow.resetBlockColors();
      }
    }
  }

  protected void recalculateFlameBlockWidths(int width) {
    resetReadyToPaint();
    calculateFlameBlockWidths(width);
  }

  protected void calculateFlameBlockWidths(int width) {
    Date start = new Date();
    FlameRow[] flameRows = getFlameRows();
    FlameBlock baseFlameBlock = getBaseFlameBlock();
    for (int row = 0; row < flameRows.length; row++) {
      if (flameRows[row] == null) {
        break;
      }

      if (flameRows[row].isReadyToPaint()) {
        continue;
      }

      FlameRow flameRow = flameRows[row];
      if (flameRow == null) {
        break;
      }
      List<FlameBlock> flameBlocks = flameRow.getFlameBlocks();
      if (flameBlocks.isEmpty()) {
        break;
      }

      if (row <= baseFlameBlock.getFlameRow().getRow()) {
        flameRow.setFirstPaintedFlameBlock(null);
        for (FlameBlock fb : flameRow.getFlameBlocks()) {
          if (fb == baseFlameBlock) {
            fb.setPaintBlock(true);
            fb.setXPosAndWidth(0, width - 1);
            fb.setFlameRow(flameRow);
            flameRow.setFirstPaintedFlameBlock(fb);

            fb.initNext();
            int r = row;
            while (r > 0) {
              FlameRow fr = flameRows[r - 1];
              FlameBlock pfb = fr.findFlameBlock(fb.getParentID());
              pfb.setPaintBlock(true);
              pfb.setXPosAndWidth(0, width - 1);
              pfb.setFlameRow(fr);
              fr.setFirstPaintedFlameBlock(pfb);
              pfb.initNext();
              pfb.setNextFlameBlock(FlameBlock.UP, fb);
              fb.setNextFlameBlock(FlameBlock.DOWN, pfb);

              fb = pfb;
              r--;
            }
          } else {
            fb.setPaintBlock(false);
          }
          flameRow.setReadyToPaint();
        }
      } else {
        double xFactor = ((double) width) / ((double) baseFlameBlock.getInclusiveValue());
        FlameRow parentFlameRow = flameRows[row - 1];
        if (parentFlameRow == null || !parentFlameRow.isReadyToPaint()) {
          break;
        }
        int lastXPos = -1;
        int lastParentID = -1;
        int childOffset = 0;
        flameRow.setFirstPaintedFlameBlock(null);
        FlameBlock lastPaintedFlameBlock = null;
        for (int n = flameBlocks.size() - 1; n >= 0; n--) {
          FlameBlock flameBlock = flameBlocks.get(n);
          flameBlock.resetPaintBlock();
          FlameBlock parentFlameBlock = parentFlameRow.findFlameBlock(flameBlock.getParentID());
          if (parentFlameBlock == null) {
            System.out.println(
                "Error: can't find flameblock row: "
                    + parentFlameRow.getRow()
                    + " id="
                    + flameBlock.getParentID());
            break;
          }
          if (!parentFlameBlock.paintBlock()) {
            flameBlock.setPaintBlock(false);
            continue;
          }
          if (flameBlock.getParentID() != lastParentID) {
            childOffset = 0;
            lastParentID = flameBlock.getParentID();
          }
          int xWidth = (int) (flameBlock.getInclusiveValue() * xFactor + 0.5); // round up/down ????
          int xPos =
              parentFlameBlock.getXPos() + parentFlameBlock.getXWidth() - xWidth - childOffset;
          //                    if (flameBlock.getInclusiveValue() <
          // (baseFlameBlock.getInclusiveValue() * 0.001)) { // <==== Threshold (0.001)!
          //                        flameBlock.setPaintBlock(false);
          //                    }
          //                    else
          if (xPos < parentFlameBlock.getXPos()) {
            // Because of accumulating rounding, we can end up with a block to the left of it's
            // parent. Just adjust it here.....
            xWidth -= parentFlameBlock.getXPos() - xPos;
            xPos = parentFlameBlock.getXPos();
          }
          if (xWidth <= 0 && xPos == lastXPos) {
            // Don't paint blocks with zero width if one already painted at same x position
            flameBlock.setPaintBlock(false);
          } else {
            flameBlock.setPaintBlock(true);
            flameBlock.setXPosAndWidth(xPos, xWidth);
            flameBlock.setFlameRow(flameRow);
            lastXPos = xPos;
            childOffset = childOffset + xWidth;

            flameBlock.initNext();
            parentFlameBlock.setNextFlameBlock(FlameBlock.UP, flameBlock);
            flameBlock.setNextFlameBlock(FlameBlock.DOWN, parentFlameBlock);
            if (lastPaintedFlameBlock != null) {
              lastPaintedFlameBlock.setNextFlameBlock(FlameBlock.LEFT, flameBlock);
              flameBlock.setNextFlameBlock(FlameBlock.RIGHT, lastPaintedFlameBlock);
            }
            //                        flameRow.setFirstPaintedFlameBlock(flameBlock); // XXX
            lastPaintedFlameBlock = flameBlock;
          }
        }

        // Update LEFT/RIGHT links. Up until here, only blocks with same parent are linked
        // correctly. Blocks with different
        // paraents are shuffled due to the sorting.
        FlameBlock fbChild = null;
        FlameBlock pfb = flameRows[row - 1].getFirstPaintedFlameBlock();
        FlameBlock lastFBChild = null;
        while (pfb != null) {
          FlameBlock pfbFirstChild = pfb.getNextFlameBlock(FlameBlock.UP);
          if (pfbFirstChild != null) {
            if (fbChild == null) {
              flameRow.setFirstPaintedFlameBlock(pfbFirstChild);
              pfbFirstChild.setNextFlameBlock(FlameBlock.LEFT, null);
            }
            if (lastFBChild != null) {
              lastFBChild.setNextFlameBlock(FlameBlock.RIGHT, pfbFirstChild);
              pfbFirstChild.setNextFlameBlock(FlameBlock.LEFT, lastFBChild);
            }
            fbChild = pfbFirstChild;
            while (fbChild != null
                && fbChild.getNextFlameBlock(FlameBlock.RIGHT) != null
                && fbChild.getNextFlameBlock(FlameBlock.RIGHT).getParentID() == pfb.getID()) {
              fbChild = fbChild.getNextFlameBlock(FlameBlock.RIGHT);
            }
            if (fbChild != null) {
              fbChild.setNextFlameBlock(FlameBlock.RIGHT, null);
            }
            lastFBChild = fbChild;
          }
          pfb = pfb.getNextFlameBlock(FlameBlock.RIGHT);
        }
        flameRow.setReadyToPaint();
      }
    }
    //        System.out.println("calculateFlameBlockWidths() " + (new Date().getTime() -
    // start.getTime()));
  }

  private int getBlockCount() {
    int blockCount = 0;
    for (FlameRow flameRow : flameRows) {
      blockCount += flameRow.getFlameBlocks().size();
    }
    return blockCount;
  }

  protected Map<Long, StackState> getFunctionIdNameMap() {
    return functionIdNameMap;
  }

  protected double getTotalValue() {
    if (baseFlameBlock != null) {
      return baseFlameBlock.getInclusiveValue();
    } else {
      // FIXUP: Error!!!!!
      return 0;
    }
  }

  protected void setFunctionIdNameMap(Map<Long, StackState> functionIdNameMap) {
    this.functionIdNameMap = functionIdNameMap;
  }

  protected FlameBlock findFlameBlock(int row, int x) {
    FlameBlock fb = null;
    if (row >= 0 && row < flameRows.length) {
      FlameRow flameRow = flameRows[row];
      if (flameRow != null) {
        for (FlameBlock flameBlock : flameRow.getFlameBlocks()) {
          if (flameBlock.paintBlock()) {
            if (x >= flameBlock.getXPos() && x <= flameBlock.getXPos() + flameBlock.getXWidth()) {
              fb = flameBlock;
              break;
            }
          }
        }
      }
    }
    return fb;
  }

  /**
   * Search all painted flame blocks for function id.
   *
   * @param functionID
   * @return
   */
  protected FlameBlock findFunctionFlameBlock(long functionID) {
    for (FlameRow flameRow : flameRows) {
      for (FlameBlock flameBlock = flameRow.getFirstPaintedFlameBlock();
          flameBlock != null;
          flameBlock = flameBlock.getNextFlameBlock(FlameBlock.RIGHT)) {
        if (flameBlock.getFunctionID() == functionID) {
          return flameBlock;
        }
      }
    }
    return null;
  }

  protected FlameBlock getSelectedFlameBlock() {
    return selectedFlameBlock;
  }

  protected boolean setSelectedFlameBlock(FlameBlock selectedFlameBlock) {
    if (selectedFlameBlock != null && selectedFlameBlock != this.selectedFlameBlock) {
      handleSelectedObject(selectedFlameBlock);
      this.selectedFlameBlock = selectedFlameBlock;
      flameView.updateSelection(selectedFlameBlock);
      return true;
    } else {
      return false;
    }
  }

  /*
   Setting selected object is slow so delay and interrupt if another selection follows within xxx ms.
  */
  private static void handleSelectedObject(final FlameBlock selectedFlameBlock) {
    synchronized (selectedObjectLock) {
      if (selectedObjectThread != null /* && selectedObjectThread.isAlive()*/) {
        selectedObjectThread.doInterrupt();
      }
      selectedObjectThread = new UpdateSelectedObjectThread(selectedFlameBlock);

      AnUtility.dispatchOnAWorkerThread(
          new Runnable() {
            @Override
            public void run() {
              selectedObjectThread.run();
            }
          },
          "handleSelectedObject");
    }
  }

  static class UpdateSelectedObjectThread extends Thread {

    private FlameBlock flameBlock;
    private boolean interrupted = false;

    public UpdateSelectedObjectThread(FlameBlock flameBlock) {
      this.flameBlock = flameBlock;
    }

    @Override
    public void run() {
      int loops = 10;
      int n = 0;
      while (n < loops && !interrupted) {
        try {
          Thread.sleep(40);
        } catch (InterruptedException ie) {
        }
        ;
        n++;
      }
      if (!interrupted) {
        //                System.out.println("select " + flameBlock.getFunctionDisplayName());
        AnWindow.getInstance().getSelectedObject().setSelObjV2(flameBlock.getFunctionID());
        AnWindow.getInstance().getSelectionManager().updateSelection();
      }
    }

    public void doInterrupt() {
      interrupted = true;
    }
  }

  protected FlameBlock getMouseOverFlameBlock() {
    return mouseOverFlameBlock;
  }

  protected boolean setMouseOverFlameBlock(FlameBlock mouseOverFlameBlock) {
    if (mouseOverFlameBlock != this.mouseOverFlameBlock) {
      this.mouseOverFlameBlock = mouseOverFlameBlock;
      return true;
    } else {
      return false;
    }
  }

  protected FlameBlock getBaseFlameBlock() {
    return baseFlameBlock;
  }

  protected void setBaseFlameBlock(FlameBlock baseFlameBlock) {
    this.baseFlameBlock = baseFlameBlock;
    setSelectedFlameBlock(baseFlameBlock);
    flameView.updateSelection(baseFlameBlock);
  }

  protected AnMetric getAnMetric() {
    return anMetric;
  }

  protected FlameBlock nextFlameBlock(FlameBlock flameBlock, boolean forward) {
    FlameBlock nextFlameBlock = null;
    if (!forward) {
      nextFlameBlock = flameBlock.getNext()[FlameBlock.RIGHT];
      if (nextFlameBlock == null) {
        int row = flameBlock.getFlameRow().getRow();
        row++;
        if (row < getFlameRows().length) {
          nextFlameBlock = getFlameRows()[row].getFirstPaintedFlameBlock();
        }
      }
      if (nextFlameBlock == null) {
        nextFlameBlock = getFlameRows()[0].getFirstPaintedFlameBlock();
      }
    } else {
      nextFlameBlock = flameBlock.getNext()[FlameBlock.LEFT];
      if (nextFlameBlock == null) {
        int row = flameBlock.getFlameRow().getRow();
        row--;
        if (row >= 0) {
          nextFlameBlock = getFlameRows()[row].getFirstPaintedFlameBlock();
          if (nextFlameBlock != null) {
            while (nextFlameBlock.getNext()[FlameBlock.RIGHT] != null) {
              nextFlameBlock = nextFlameBlock.getNext()[FlameBlock.RIGHT];
            }
          }
        }
      }
      if (nextFlameBlock == null) {
        int row = getFlameRows().length - 1;
        nextFlameBlock = null;
        while (nextFlameBlock == null && row > 0) {
          nextFlameBlock = getFlameRows()[row].getFirstPaintedFlameBlock();
          row--;
        }
        if (nextFlameBlock != null) {
          while (nextFlameBlock.getNext()[FlameBlock.RIGHT] != null) {
            nextFlameBlock = nextFlameBlock.getNext()[FlameBlock.RIGHT];
          }
        }
      }
    }
    return nextFlameBlock;
  }

  protected String dump(int rows) {
    StringBuilder buf = new StringBuilder();

    buf.append("metric=" + anMetric.getUserName());
    buf.append("\n");
    buf.append("rows=" + flameRows.length);
    buf.append("\n");
    for (int i = 0; i < rows && i < flameRows.length; i++) {
      buf.append(flameRows[i].dump());
    }

    return buf.toString();
  }
}

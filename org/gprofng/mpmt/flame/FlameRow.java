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

package org.gprofng.mpmt.flame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlameRow {

  private FlameData flameData;
  private int row;
  private List<FlameBlock> flameBlocks;
  private boolean readyToPaint;
  private FlameBlock firstPaintedFlameBlock;

  private Map<Integer, FlameBlock> idToBlockMap;

  protected FlameRow(FlameData flameData, int row, List<FlameBlock> flameBlocks) {
    this.flameData = flameData;
    this.row = row;
    this.flameBlocks = flameBlocks;
    readyToPaint = false;

    idToBlockMap = new HashMap<Integer, FlameBlock>();

    if (flameBlocks != null) {
      for (FlameBlock flameBlock : flameBlocks) {
        idToBlockMap.put(flameBlock.getID(), flameBlock);
      }
    }
  }

  protected FlameBlock findFlameBlock(int id) {
    FlameBlock block = idToBlockMap.get(id);
    return block;
  }

  protected int getRow() {
    return row;
  }

  protected List<FlameBlock> getFlameBlocks() {
    return flameBlocks;
  }

  protected FlameData getFlameData() {
    return flameData;
  }

  protected boolean isReadyToPaint() {
    return readyToPaint;
  }

  protected void setReadyToPaint() {
    readyToPaint = true;
  }

  protected void resetReadyToPaint() {
    readyToPaint = false;
  }

  protected void resetBlockColors() {
    for (FlameBlock flameBlock : flameBlocks) {
      flameBlock.resetColor();
    }
  }

  protected FlameBlock getFirstPaintedFlameBlock() {
    return firstPaintedFlameBlock;
  }

  protected void setFirstPaintedFlameBlock(FlameBlock firstPaintedFlameBlock) {
    this.firstPaintedFlameBlock = firstPaintedFlameBlock;
  }

  protected String dump() {
    StringBuilder buf = new StringBuilder();

    buf.append("======================================== ");
    buf.append("row=" + row);
    buf.append("  ");
    buf.append("blocks=" + flameBlocks.size());
    buf.append("\n");
    for (FlameBlock flameBlock : flameBlocks) {
      buf.append(flameBlock.dump());
    }
    return buf.toString();
  }
}

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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;

public class FlameBlock {

  protected static final int UP = 0;
  protected static final int DOWN = 1;
  protected static final int LEFT = 2;
  protected static final int RIGHT = 3;
  protected static final int LAST_UP = 4;

  private int id;
  private int parentID;
  private long functionID;
  private double exclusiveValue;
  private double attributedValue;

  private PaintDetails paintDetails; // Only available (!= null) if block shuld be painted.

  class PaintDetails {

    private FlameRow flameRow;
    private boolean paintBlock;
    private int xPos; // relative to FlamePanel
    private int xWidth;
    private Color color;
    private String functionName;
    private FlameBlock[] next;

    private PaintDetails() {
      next = new FlameBlock[5];
    }

    private FlameRow getFlameRow() {
      return flameRow;
    }

    private void setFlameRow(FlameRow flameRow) {
      this.flameRow = flameRow;
    }

    private int getXPos() {
      return xPos;
    }

    private void setXPos(int xPos) {
      this.xPos = xPos;
    }

    private int getXWidth() {
      return xWidth;
    }

    private void setXWidth(int xWidth) {
      this.xWidth = xWidth;
    }

    private Color getColor() {
      Color returnColor = null;
      if (flameRow.getFlameData().getBaseFlameBlock() == FlameBlock.this) {
        // Special case
        returnColor = AnEnvironment.FLAME_BLOCK_BASE_BACKGROUND;
      }
      if (color == null) {
        if (xWidth >= 1) {
          StackState state = flameRow.getFlameData().getFunctionIdNameMap().get(getFunctionID());
          if (state != null) {
            long function = state.getNumber(); // Histable::Function*
            color = AnWindow.getInstance().getColorChooser().getColorMap().getFuncColor(function);
          } else {
            returnColor = Color.LIGHT_GRAY; // weird
          }
        }
      }
      if (returnColor == null) {
        returnColor = color;
      }
      if (flameRow.getFlameData().getSelectedFlameBlock() == FlameBlock.this
          && flameRow.getFlameData().getBaseFlameBlock() != FlameBlock.this) {
        returnColor = AnEnvironment.FLAME_BLOCK_SELECTED_BACKGROUND;
      }
      if (flameRow.getFlameData().getMouseOverFlameBlock() == FlameBlock.this) {
        returnColor = returnColor.brighter();
      }

      return returnColor;
    }

    protected void resetColor() {
      color = null;
    }

    private String getFunctionName() {
      if (functionName == null) {
        StackState state = flameRow.getFlameData().getFunctionIdNameMap().get(getFunctionID());
        if (state != null) {
          functionName = state.getName();
        }
        if (functionName == null && flameRow.getRow() == 0) {
          // YLM: not sure if this still can happen, keeping it:
          // Special case
          functionName = "<Total>";
        }
        if (functionName == null) {
          functionName = "" + getFunctionID();
        }
      }
      return functionName;
    }

    private String getFunctionDisplayName() {
      String fn = getFunctionName();
      String prefix = "";
      //            if (getFlameRow().getFlameData().getBaseFlameBlock() == FlameBlock.this) {
      //                prefix = AnLocale.getString("Base: ");
      //            }
      return prefix + fn;
    }

    private FlameBlock[] getNext() {
      return next;
    }

    private void setNextFlameBlock(int direction, FlameBlock flameBlock) {
      next[direction] = flameBlock;
    }

    private FlameBlock getNextFlameBlock(int direction) {
      return next[direction];
    }

    private boolean paintBlock() {
      return paintBlock;
    }

    private void setPaintBlock(boolean paintBlock) {
      this.paintBlock = paintBlock;
    }
  }

  protected FlameBlock(
      int id, int parentID, long functionID, double exclusiveValue, double attributedValue) {
    this.id = id;
    this.parentID = parentID;
    this.functionID = functionID;
    this.exclusiveValue = exclusiveValue;
    this.attributedValue = attributedValue;
  }

  protected int getID() {
    return id;
  }

  protected int getParentID() {
    return parentID;
  }

  protected long getFunctionID() {
    return functionID;
  }

  protected double getInclusiveValue() {
    return exclusiveValue + attributedValue;
  }

  protected double getExclusiveValue() {
    return exclusiveValue;
  }

  protected double getAttributedValue() {
    return attributedValue;
  }

  protected String getInclusiveValueFormatted() {
    return getValueFormatted(getInclusiveValue());
  }

  protected String getExclusiveValueFormatted() {
    return getValueFormatted(getExclusiveValue());
  }

  protected String getAttributedValueFormatted() {
    return getValueFormatted(getAttributedValue());
  }

  private String getValueFormatted(double value) {
    // FIXUP: This logic is not always correct.
    AnMetric anMetric = getFlameRow().getFlameData().getAnMetric();
    if (anMetric.isTimeMetric()) {
      return String.format("%.3f", value);
    } else {
      //            // YXXX Format with '@' to indicate half-spaces between digits
      //            AnLong anLong = new AnLong((long) value);
      //            return anLong.toFormString();
      return "" + (long) value;
    }
  }

  protected Color getColor() {
    return paintDetails.getColor();
  }

  protected void resetColor() {
    if (paintDetails != null) {
      paintDetails.resetColor();
    }
  }

  protected void setXPosAndWidth(int xPos, int xWidth) {
    paintDetails.setXPos(xPos);
    paintDetails.setXWidth(xWidth);
  }

  protected int getXPos() {
    return paintDetails.getXPos();
  }

  protected int getXWidth() {
    return paintDetails.getXWidth();
  }

  protected boolean paintBlock() {
    return paintDetails != null && paintDetails.paintBlock();
  }

  protected void setPaintBlock(boolean paintBlock) {
    paintDetails.setPaintBlock(paintBlock);
  }

  protected void resetPaintBlock() {
    paintDetails = new PaintDetails();
  }

  protected String getFunctionName() {
    return paintDetails.getFunctionName();
  }

  protected String getFunctionDisplayName() {
    return paintDetails.getFunctionDisplayName();
  }

  protected FlameRow getFlameRow() {
    return paintDetails.getFlameRow();
  }

  protected void setFlameRow(FlameRow flameRow) {
    paintDetails.setFlameRow(flameRow);
  }

  protected FlameBlock[] getNext() {
    return paintDetails.getNext();
  }

  protected void setNextFlameBlock(int direction, FlameBlock flameBlock) {
    paintDetails.setNextFlameBlock(direction, flameBlock);
  }

  protected FlameBlock getNextFlameBlock(int direction) {
    if (paintDetails != null) {
      return paintDetails.getNextFlameBlock(direction);
    } else {
      return null;
    }
  }

  protected String getToolTipTextHTML() {
    String toolTipText = "";
    if (getFlameRow() != null && getFlameRow().getFlameData() != null) {
      double totalValue = getFlameRow().getFlameData().getTotalValue();
      StringBuilder buf = new StringBuilder();
      buf.append("<html>");
      buf.append("<font " + "face=\"monospaced\">");
      buf.append("<b>");
      buf.append("<nobr>");
      buf.append(AnUtility.escapeSpecialHTMLCharacters(getFunctionName()));
      buf.append("</b>");

      String inclExclText;
      if (getNextFlameBlock(UP) == null) {
        inclExclText = AnLocale.getString("Exclusive");
      } else {
        inclExclText = AnLocale.getString("Inclusive");
      }
      buf.append("<br>");
      buf.append("<nobr>");
      buf.append(
          formatValueHTML(
              inclExclText, totalValue, getInclusiveValue(), getInclusiveValueFormatted()));
      if (getExclusiveValue() > 0) {
        buf.append("<br>");
        buf.append("<nobr>");
        buf.append(
            formatValueHTML(
                AnLocale.getString("Exclusive"),
                totalValue,
                getExclusiveValue(),
                getExclusiveValueFormatted()));
        buf.append("<br>");
        buf.append("<nobr>");
        buf.append(
            formatValueHTML(
                AnLocale.getString("Calls"),
                totalValue,
                getAttributedValue(),
                getAttributedValueFormatted()));
      } else {
        buf.append("<br>");
        buf.append("<&nbsp;");
        buf.append("<br>");
        buf.append("<&nbsp;");
      }
      buf.append("</html>");
      toolTipText = buf.toString();
    }
    return toolTipText;
  }

  private String formatValueHTML(
      String what, double totalValue, double value, String formattedValue) {
    String percentText = "";
    if (totalValue != 0) {
      double percent = value / totalValue * 100;
      percentText = String.format("%.2f", percent);
    }
    String text =
        String.format(
            "%-9s: %7s%% %10s %s",
            what,
            percentText,
            formattedValue,
            getFlameRow().getFlameData().getAnMetric().getUnit());
    text = text.replaceAll(" ", "&nbsp;");
    return text;
  }

  protected String getDetailsTextHTML() {
    return getToolTipTextHTML();
  }

  protected static String getDetailsEmptyTextHTML() {
    String text;
    StringBuilder buf = new StringBuilder();
    buf.append("<html>");
    buf.append("<&nbsp;");
    buf.append("<br>");
    buf.append("<&nbsp;");
    buf.append("<br>");
    buf.append("<&nbsp;");
    buf.append("<br>");
    buf.append("<&nbsp;");
    buf.append("<br>");
    buf.append("</html>");
    text = buf.toString();
    return text;
  }

  protected String getValueText() {
    String text = "";
    if (getFlameRow() != null && getFlameRow().getFlameData() != null) {
      double totalValue = getFlameRow().getFlameData().getTotalValue();
      StringBuilder buf = new StringBuilder();

      String percentText = null;
      if (totalValue != 0) {
        double percent = getInclusiveValue() / totalValue * 100;
        percentText = String.format("%.2f", percent);
      }
      if (percentText != null) {
        buf.append(percentText);
        buf.append("%");
      }
      buf.append("  ");
      buf.append(getInclusiveValueFormatted());
      buf.append(getFlameRow().getFlameData().getAnMetric().getUnit());

      text = buf.toString();
    }
    return text;
  }

  protected void initNext() {
    setNextFlameBlock(FlameBlock.UP, null);
    setNextFlameBlock(FlameBlock.DOWN, null);
    setNextFlameBlock(FlameBlock.LEFT, null);
    setNextFlameBlock(FlameBlock.RIGHT, null);
  }

  protected String dump() {
    StringBuilder buf = new StringBuilder();

    buf.append("-------------------");
    buf.append("id=" + id);
    buf.append("\n");
    buf.append("parentID=" + parentID);
    buf.append("\n");
    buf.append("functionID=" + functionID);
    buf.append("\n");

    //        buf.append("function name=" +
    // AnWindow.getInstance().getFlameView().getFlameData().getFunctionIdNameMap().get(functionID).getName());
    StackState state =
        AnWindow.getInstance()
            .getFlameView()
            .getFlameData()
            .getFunctionIdNameMap()
            .get(getFunctionID());
    final String name;
    if (state != null) {
      name = state.getName();
    } else {
      name = null;
    }
    buf.append("function name=" + name);

    buf.append("\n");
    buf.append("unformatted metric value=" + String.format("%.3f", exclusiveValue));
    buf.append("\n");
    buf.append("block width=" + (paintDetails != null ? paintDetails.getXWidth() : ""));
    buf.append("\n");
    buf.append(
        "block next="
            + " "
            + dumpNext(UP)
            + " "
            + dumpNext(DOWN)
            + " "
            + dumpNext(LEFT)
            + " "
            + dumpNext(RIGHT));
    buf.append("\n");

    return buf.toString();
  }

  private String dumpNext(int direction) {
    FlameBlock fb = getNextFlameBlock(direction);
    if (fb != null) {
      return "" + fb.getID();
    } else {
      return "-";
    }
  }
}

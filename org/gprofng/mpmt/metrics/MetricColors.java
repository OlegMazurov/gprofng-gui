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

package org.gprofng.mpmt.metrics;

import java.awt.Color;
import java.util.HashMap;

public class MetricColors {

  private static Color defaultSingle = new Color(145, 145, 145);
  private static Color defaultMissing = defaultSingle;

  public static final Color GREEN = new Color(95, 200, 48);
  public static final Color LIGHT_GREEN = new Color(0xcc, 0xff, 0x00);
  public static final Color GOLDEN_YELLOW = new Color(0xfd, 0xde, 0x00);
  public static final Color FUSCHIA = new Color(242, 0, 255);
  public static final Color PLUM = new Color(169, 19, 182);
  public static final Color BLUE_VIOLET = new Color(116, 1, 223);
  public static final Color ORANGE = new Color(0xff, 0x96, 0x09);
  public static final Color RED = new Color(255, 18, 0);
  public static final Color COBALT = new Color(0, 34, 255);
  public static final Color DARK_GREY = new Color(112, 119, 137);
  public static final Color CYAN = Color.CYAN;
  public static final Color BLUE = Color.BLUE;
  public static final Color GRAY = Color.GRAY;
  public static final Color BLACK = Color.BLACK;

  private static final HashMap<Integer, Color> colorHashMap = new HashMap<Integer, Color>();

  static {
    // SOLARIS MICROSTATES
    colorHashMap.put("user".hashCode(), GREEN);
    colorHashMap.put("system".hashCode(), LIGHT_GREEN);
    colorHashMap.put("trap".hashCode(), GOLDEN_YELLOW);
    colorHashMap.put("datapfault".hashCode(), FUSCHIA);
    colorHashMap.put("textpfault".hashCode(), PLUM);
    colorHashMap.put("kernelpfault".hashCode(), BLUE_VIOLET);
    colorHashMap.put("stop".hashCode(), ORANGE);
    colorHashMap.put("wait".hashCode(), RED);
    colorHashMap.put("sleep".hashCode(), COBALT);
    colorHashMap.put("lock".hashCode(), DARK_GREY);

    //
    colorHashMap.put("totalcpu".hashCode(), colorHashMap.get("user".hashCode())); // TBR?
    colorHashMap.put("owait".hashCode(), colorHashMap.get("wait".hashCode())); // TBR?

    // DEAD_CODE?
    colorHashMap.put("readiobytes".hashCode(), new Color(219, 87, 100));
    colorHashMap.put("writeiobytes".hashCode(), new Color(252, 88, 78));
    // DEAD_CODE END

    colorHashMap.put("heapalloccnt".hashCode(), defaultSingle);
    colorHashMap.put("heapallocbytes".hashCode(), defaultSingle);
    colorHashMap.put("heapleakcnt".hashCode(), defaultSingle);
    colorHashMap.put("heapleakbytes".hashCode(), defaultSingle);
  }

  public static void setCustomMetricColor(String name, Color color) {
    if (name.equals("default")) {
      defaultSingle = color;
      defaultMissing = color;
    } else {
      colorHashMap.put(name.hashCode(), color);
    }
  }

  public static Color getColor(String name) {
    int hashCode = name.hashCode();
    Color color = colorHashMap.get(hashCode);
    if (color == null) {
      //            System.err.println("MetricsColors.getColor \'" + name + "\' not found in color
      // map!");
      color = defaultMissing;
    }
    return color;
  }

  public static class MetricColor {
    private String metricName;
    private Color color;

    public MetricColor(String metricName, Color color) {
      this.metricName = metricName;
      this.color = color;
    }

    /**
     * @return the metricName
     */
    public String getMetricName() {
      return metricName;
    }

    /**
     * @return the color
     */
    public Color getColor() {
      return color;
    }
  }
}

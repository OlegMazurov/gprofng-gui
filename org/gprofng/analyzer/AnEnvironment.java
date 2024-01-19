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

package org.gprofng.analyzer;

import org.gprofng.mpmt.guitesting.GUITesting;
import org.gprofng.mpmt.util.gui.AnScrollBarUI;
import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public class AnEnvironment {

  public static final Color NIMBUS_PANEL_BACKGROUND = new Color(236, 236, 240);
  public static final Color NIMBUS_PANEL_LIGHT_BACKGROUND = new Color(249, 249, 251);
  public static final Color NIMBUS_TABLE_BACKGROUND = new Color(252, 253, 254);
  public static final Color NIMBUS_SCROLLPANE_BACKGROUND = NIMBUS_TABLE_BACKGROUND;
  public static final Color NIMBUS_TEXT_BACKGROUND = NIMBUS_TABLE_BACKGROUND;
  public static final Color NIMBUS_LIST_BACKGROUND = NIMBUS_TABLE_BACKGROUND;
  public static final Color NIMBUS_TREE_BACKGROUND = NIMBUS_TABLE_BACKGROUND;
  public static final Color NIMBUS_TABLE_HEADER_BACKGROUND = new Color(230, 230, 234);
  public static final Color NIMBUS_TABLE_SELECTION_BACKGROUND = new Color(190, 210, 229);

  public static final Color SCROLLPANE_BACKGROUND = Color.WHITE;
  public static final Color INVISIBLE_BACKGROUND = new Color(0, 0, 0, 0);

  public static final Color DEFAULT_BACKGROUND_COLOR =
      UIManager.getLookAndFeel().getDefaults().getColor("Panel.background");
  public static final Color DEFAULT_PANEL_BACKGROUND = new Color(251, 251, 251);
  public static final Color DEFAULT_DIALOG_BACKGROUND = new Color(248, 248, 248);
  public static final Color DEFAULT_TEXT_PANE_RO_BACKGROUND = new Color(250, 250, 250);

  public static final Color NAVIGATION_PANEL_BACKGROUND_COLOR = new Color(221, 222, 227);
  public static final Color NAVIGATION_PANEL_SECTION_BACKGROUND_COLOR = new Color(199, 200, 205);
  public static final Color NAVIGATION_PANEL_BORDER_COLOR = new Color(179, 178, 178);
  public static final Color NAVIGATION_SPLIT_PANE_DIVIDER_COLOR = new Color(146, 146, 148);

  public static final Color VIEW_SELECTED_TOP_COLOR = new Color(209, 225, 239);
  public static final Color VIEW_SELECTED_BOTTOM_COLOR = new Color(176, 189, 201);
  public static final Color VIEW_FOCUSED_TOP_COLOR = new Color(239, 239, 239);
  public static final Color VIEW_FOCUSED_BOTTOM_COLOR = new Color(218, 218, 218);
  public static final Color VIEW_FOCUSED_BACKGROUND_COLOR = new Color(215, 220, 230);
  public static final Color VIEW_SELECTED_BACKGROUND_COLOR = new Color(183, 196, 220);
  public static final Color VIEW_SELECTED_BORDER_COLOR = new Color(190, 190, 200);

  public static final Color STATUS_SELECTED_BORDER_COLOR = new Color(150, 150, 160);
  public static final Color DROP_LINE_COLOR = new Color(255, 165, 0);
  public static final Color SUBVIEW_PANEL_BACKGROUND = new Color(242, 242, 244);
  public static final Color ABOUT_BOX_BORDER_COLOR = new Color(204, 204, 204);

  public static final Color SPLIT_PANE_BORDER_COLOR = new Color(206, 208, 210);
  public static final Color SPLIT_PANE_BORDER_COLOR_INSIDE =
      new Color(206 + 20, 208 + 20, 210 + 20);
  public static final Color SPLIT_PANE_DIVIDER_BACKGROUND_COLOR = new Color(249, 249, 251);
  public static final Color SPLIT_PANE_DIVIDER_BACKGROUND_COLOR_INSIDE = new Color(210, 210, 212);
  public static final Color SPLIT_PANE_ONE_TOUCH_BUTTON_COLOR = new Color(120, 120, 120);
  public static final Color SPLIT_PANE_ONE_TOUCH_BUTTON_FOCUSED_COLOR = new Color(10, 10, 10);

  public static final Color TABBED_PANE_BORDER_COLOR = new Color(206, 208, 210);
  public static final Color TABBED_PANE_BACKGROUND_COLOR = new Color(218, 218, 218);
  public static final Color TABBED_PANE_DEFAULT_COLOR = new Color(218, 218, 218);
  public static final Color TABBED_PANE_TAB_AREA_TOP_COLOR = new Color(244, 244, 245);
  public static final Color TABBED_PANE_TAB_AREA_BOTTOM_COLOR = new Color(218, 218, 218);
  public static final Color TABBED_PANE_DIVIDER_COLOR = new Color(163, 163, 163);
  public static final Color TABBED_PANE_LINE_COLOR = new Color(238, 238, 238);
  public static final Color TABBED_PANE_SELECTED_TAB_COLOR = new Color(238, 238, 238);
  public static final Color TABBED_PANE_FOCUSED_TAB_TOP_COLOR = new Color(221, 229, 236);
  public static final Color TABBED_PANE_FOCUSED_TAB_BOTTOM_COLOR = new Color(207, 215, 222);
  public static final Color TABBED_PANE_DESELECTED_TAB_TOP_COLOR = new Color(244, 244, 245);
  public static final Color TABBED_PANE_DESELECTED_TAB_BOTTOM_COLOR = new Color(218, 218, 218);
  public static final Color TABBED_PANE_DESELECTED_FORGROUND_COLOR = new Color(150, 150, 150);
  public static final Color TABBED_PANE_TAB_FOCUS_COLOR = new Color(180, 180, 180);

  public static final Color FILTER_FOREGROUND_COLOR = new Color(44, 121, 6);
  public static final Color FILTER_BACKGROUND1_COLOR = DEFAULT_TEXT_PANE_RO_BACKGROUND;
  public static final Color FILTER_BACKGROUND2_COLOR = new Color(251 - 4, 251 - 4, 251 - 4);
  public static final Color FILTER_BACKGROUND_SELECTED_COLOR = new Color(233, 238, 244);
  public static final Color FILTER_INFO_TEXT_COLOR = new Color(160, 160, 160);
  public static final Color FILTER_DIVIDER_BACKGROUND_COLOR = new Color(196 + 5, 197 + 5, 201 + 5);
  public static final Color FILTER_HEADER_COLOR = new Color(51, 51, 51);
  public static final Color FILTER_TOOLBAR_BACKGROUND1_COLOR = new Color(238, 238, 238);
  public static final Color FILTER_TOOLBAR_BACKGROUND2_COLOR = new Color(219, 219, 219);
  public static final Color FILTER_STATUS_BORDER_COLOR = new Color(182, 181, 186);

  public static final Color WELCOME_BUTTON_FOCUSED_COLOR = new Color(100, 130, 220);
  public static final Color WELCOME_BUTTON_DISABLED_COLOR = new Color(190, 190, 190);
  public static final Color WELCOME_BUTTON_ACTION_COLOR = new Color(125, 125, 125);
  public static final Color WELCOME_BUTTON_EXPERIMENT_COLOR = new Color(125, 125, 125);
  public static final Color WELCOME_MAIN_LABEL_COLOR = new Color(102, 102, 102);

  public static final Color STATUS_PANEL_BORDER_COLOR = new Color(179, 178, 178);
  public static final Color STATUS_PANEL_BACKGROUND_COLOR1 = new Color(228, 228, 228);
  public static final Color STATUS_PANEL_BACKGROUND_COLOR2 = new Color(191, 190, 191);

  public static final Color TOOLBAR_BACKGROUND_COLOR = new Color(178, 178, 178);
  public static final Color TOOLBAR_BORDER_COLOR = new Color(133 + 30, 133 + 30, 133 + 30);

  public static final Color MENUBAR_BACKGROUND_COLOR1 = new Color(228, 228, 228);
  public static final Color MENUBAR_BACKGROUND_COLOR2 = new Color(191, 190, 191);
  public static final Color MENUBAR_BORDER_COLOR = new Color(179, 178, 178);
  public static final Color MENUBAR_BACKGROUND_COLOR = new Color(207, 207, 207);

  public static final Color BAR_BORDER_COLOR = new Color(190, 190, 190);
  public static final Color BAR_BACKGROUND_COLOR = Color.WHITE;
  public static final Color BAR_COMPARE_COLOR1 = new Color(195, 195, 195);
  public static final Color BAR_COMPARE_COLOR2 = new Color(215, 215, 215);

  public static final Color METRIC_HOT_HIGHLIGHT = new Color(255, 200, 0);

  public static final Color TOOLTIP_POPUP_BACKGROUND_COLOR = new Color(223, 236, 243);
  public static final Color TOOLTIP_POPUP_FOREGROUND_COLOR = new Color(51, 77, 100);
  public static final Color TOOLTIP_POPUP_BORDER_COLOR = new Color(144, 145, 149);
  public static final Color TOOLTIP_VIEW_BACKGROUND_COLOR = new Color(247, 247, 255);
  // Scroll bars
  public static final Color SCROLLBAR_TRACK_COLOR = new Color(248, 248, 248);
  public static final Color SCROLLBAR_BORDER_COLOR = new Color(199, 200, 202);
  public static final Color SCROLLBAR_THUMB_COLOR = new Color(179, 178, 178);
  public static final Color SCROLLBAR_THUMB_FOCUSED_COLOR = new Color(129, 129, 129);
  public static final Color SCROLLBAR_INC_DEC_COLOR = new Color(129, 129, 129);
  public static final Color SCROLLBAR_INC_DEC_FOCUSED_COLOR = new Color(55, 55, 55);
  // Tables
  public static final Color TABLE_HEADER_FOREGROUND_COLOR = new Color(86, 101, 125);
  public static final Color TABLE_HEADER_BACKGROUND_COLOR_1 = new Color(248, 248, 251);
  public static final Color TABLE_HEADER_BACKGROUND_COLOR_2 = new Color(248 - 6, 248 - 6, 251 - 6);
  public static final Color TABLE_HEADER_SELECTED_COLOR = new Color(209, 225, 239);
  public static final Color TABLE_LINE_BACKGROUND_COLOR_2 = new Color(249, 249, 249);
  public static final Color TABLE_LINE_BACKGROUND_SELECTED_COLUMN_COLOR_1 =
      new Color(238, 244, 249);
  public static final Color TABLE_LINE_BACKGROUND_SELECTED_COLUMN_COLOR_2 =
      new Color(228, 233, 238);
  public static final Color TABLE_VERTICAL_GRID_COLOR = new Color(230 - 5, 230 - 5, 230 - 5);
  public static final Color TABLE_VERTICAL_GRID_METRIC_COLOR =
      new Color(200 - 20, 200 - 20, 200 - 20);
  public static final Color TABLE_VERTICAL_GRID_ATTRIBUTE_COMP_COLOR =
      new Color(210 - 20, 210 - 20, 210 - 20);
  public static final Color TABLE_VERTICAL_GRID_METRIC_COMP_COLOR =
      new Color(180 - 20, 180 - 20, 180 - 20);
  // Metric
  public static final Color METRIC_MENU_SECTION_COLOR = new Color(80, 80, 80);
  public static final Color METRIC_SELCTOR_MENU_SEPARATOR_COLOR = new Color(220, 220, 220);
  // Compare status panel
  public static final Color COMPARE_TOGGLE_ICON_BORDER_COLOR = new Color(150, 150, 150);
  public static final Color COMPARE_TOGGLE_ICON_BORDER_FOCUSED_COLOR = new Color(100, 100, 100);
  public static final Color COMPARE_TOGGLE_ICON_SELECTED_COLOR = new Color(209, 225, 239);
  public static final Color COMPARE_ICON_BORDER_COLOR = new Color(219, 219, 219);
  public static final Color COMPARE_ICON_MOUSE_PRESSED_BACKGROUND_COLOR =
      new Color(209 - 20, 225 - 20, 239 - 20);
  public static final Color COMPARE_ICON_TOGGLE_BACKGROUND_COLOR = new Color(217, 219, 224);

  public static final Color FLAME_TOP_COLOR = new Color(254, 254, 254);
  public static final Color FLAME_BOTTOM_COLOR = new Color(230, 230, 230);
  public static final Color FLAME_INFO_TEXT_COLOR = new Color(120, 120, 120);
  public static final Color FLAME_BLOCK_BORDER_COLOR = new Color(180, 180, 180);
  public static final Color FLAME_BLOCK_BASE_BACKGROUND = new Color(255, 157, 14);
  public static final Color FLAME_BLOCK_SELECTED_BACKGROUND = new Color(210, 210, 210);
  public static final Color FLAME_BLOCK_SELECTED_BORDER1 = new Color(35, 85, 135);
  public static final Color FLAME_BLOCK_SELECTED_BORDER2 = new Color(210, 210, 210);
  public static final Color FLAME_BLOCK_MOUSEOVER_BORDER1 = new Color(0, 102, 51);

  public static final String DefaultLFName = "Metal";

  public enum LF {
    Metal,
    Nimbus,
    Windows,
    AppleAqua,
    Motif,
    GTK
  };

  private static LF lf = null;

  public static void setLookAndFeel() {
    // Always set L&F to Metal, if available
    String lfName = UIManager.getLookAndFeel().getName();
    if (!lfName.equals(DefaultLFName)) {
      try {
        UIManager.LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelInfo : installedLookAndFeels) {
          if (lookAndFeelInfo.getName().equals(DefaultLFName)) {
            javax.swing.UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
            break;
          }
        }
      } catch (ClassNotFoundException ex) {
      } catch (InstantiationException ex) {
      } catch (IllegalAccessException ex) {
      } catch (javax.swing.UnsupportedLookAndFeelException ex) {
      }
    }

    lfName = UIManager.getLookAndFeel().getName();
    if (lfName.equals("Metal")) {
      lf = LF.Metal;
      setAnalyzerMetalTheme();
    } else if (lfName.equals("Nimbus")) {
      lf = LF.Nimbus;
      setAnalyzerNimbusTheme();
    } else if (lfName.startsWith("Mac")) {
      lf = LF.AppleAqua;
    } else if (lfName.startsWith("Windows")) {
      lf = LF.Windows;
    } else if (lfName.equals("CDE/Motif")) {
      lf = LF.Motif;
    } else if (lfName.equals("GTK+")) {
      lf = LF.GTK;
    } else {
      lf = LF.Metal;
    }
  }

  /**
   * @return true if current L&F is Metal
   */
  public static boolean isLFMetal() {
    return lf == LF.Metal;
  }

  /**
   * @return true if current L&F is Nimbus
   */
  public static boolean isLFNimbus() {
    return lf == LF.Nimbus;
  }

  /**
   * @return the current L&F
   */
  public static LF getLF() {
    return lf;
  }

  private static void setAnalyzerNimbusTheme() {
    UIManager.getLookAndFeel().getDefaults().put("FileChooser.background", NIMBUS_PANEL_BACKGROUND);
    UIManager.getLookAndFeel().getDefaults().put("Panel.background", NIMBUS_PANEL_BACKGROUND);
  }

  private static void setAnalyzerMetalTheme() {
    // Set all fonts to PLAIN
    Enumeration keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);
      if (value instanceof FontUIResource) {
        if (GUITesting.getInstance().isRunningUnderGUITesting()) {
          Font font = new Font("monospaced", Font.PLAIN, 12);
          FontUIResource fontResource = new FontUIResource(font);
          UIManager.put(key, fontResource);
        } else {
          FontUIResource font = (FontUIResource) value;
          UIManager.put(key, new FontUIResource(font.deriveFont(Font.PLAIN)));
        }
      }
    }

    // Install our own scroll bar UI
    UIManager.put("ScrollBarUI", AnScrollBarUI.class.getName());
    UIManager.put("ScrollBar.width", 14);
  }
}

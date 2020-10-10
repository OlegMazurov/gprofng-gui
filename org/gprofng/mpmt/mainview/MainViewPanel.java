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

package org.gprofng.mpmt.mainview;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnSplitPaneFixedRightSize;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

// +----------+-------------------------------------------------+---------------+
// |     N    |                       M                         |      S1       |
// |          |                                                 |               |
// |          |                                                 |               |
// |          |                                                 |               |
// |          |                                                 D1              |
// |          |                                                 |               |
// |          |                                                 |               |
// |          |                                                 |               |
// |          |                                                 +-D3------------+
// |          |                                                 |      S3       |
// |          |                                                 |               |
// |          |                                                 |               |
// |          |                                                 |               |
// |          |                                                 |               |
// |          |                                                 |               |
// |          +-D2----------------------------------------------+               |
// |          |                        S2                       |               |
// |          |                                                 |               |
// +----------+-------------------------------------------------+---------------+
//
// MainViewPanel manages the M+S1+S2+S3 area
//
public class MainViewPanel extends javax.swing.JPanel {

  public enum SubviewArea {
    SUBVIEW_AREA_1(0),
    SUBVIEW_AREA_2(1),
    SUBVIEW_AREA_3(2);
    private final int index;

    SubviewArea(int index) {
      this.index = index;
    }

    public int index() {
      return index;
    }
  };
  // Areas
  private final BorderPanel mainviewArea = new BorderPanel();
  private final SubviewAreaPanel subviewArea1 = new SubviewAreaPanel(SubviewArea.SUBVIEW_AREA_1);
  private final SubviewAreaPanel subviewArea2 = new SubviewAreaPanel(SubviewArea.SUBVIEW_AREA_2);
  private final SubviewAreaPanel subviewArea3 = new SubviewAreaPanel(SubviewArea.SUBVIEW_AREA_3);
  private final List<SubviewAreaPanel> subviewAreas =
      new ArrayList<SubviewAreaPanel>(Arrays.asList(subviewArea1, subviewArea2, subviewArea3));
  // List of all added subviews (including custom subviews)
  private final List<Subview> subviews = new ArrayList<Subview>();
  private final AnSplitPaneFixedRightSize splitPane1;
  private final AnSplitPaneFixedRightSize splitPane2;
  private final AnSplitPaneFixedRightSize splitPane3;

  public MainViewPanel() {
    initComponents();
    setBackground(AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR);
    setBorder(null);
    setLayout(new GridBagLayout());

    splitPane3 =
        new AnSplitPaneFixedRightSize(
            JSplitPane.VERTICAL_SPLIT,
            subviewArea1,
            subviewArea3,
            UserPref.getInstance().getSplitPane3().getSize(),
            UserPref.getInstance().getSplitPane3().getDefaultSize());
    splitPane2 =
        new AnSplitPaneFixedRightSize(
            JSplitPane.VERTICAL_SPLIT,
            mainviewArea,
            subviewArea2,
            UserPref.getInstance().getSplitPane2().getSize(),
            UserPref.getInstance().getSplitPane2().getDefaultSize());
    splitPane1 =
        new AnSplitPaneFixedRightSize(
            JSplitPane.HORIZONTAL_SPLIT,
            splitPane2,
            splitPane3,
            UserPref.getInstance().getSplitPane1().getSize(),
            UserPref.getInstance().getSplitPane1().getDefaultSize());

    splitPane1.setHidden(true);
    splitPane2.setHidden(true);
    splitPane3.setHidden(true);

    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    //        gridBagConstraints.insets = new Insets(5, 0, 5, 4);
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(splitPane1, gridBagConstraints);
  }

  /**
   * @return the mainviewArea
   */
  public JPanel getMainview() {
    return mainviewArea;
  }

  /**
   * @return the splitPane1
   */
  public AnSplitPaneFixedRightSize getSplitPane1() {
    return splitPane1;
  }

  /**
   * @return the splitPane2
   */
  public AnSplitPaneFixedRightSize getSplitPane2() {
    return splitPane2;
  }

  /**
   * @return the splitPane3
   */
  public AnSplitPaneFixedRightSize getSplitPane3() {
    return splitPane3;
  }

  public final void addSubview(Subview subview) {
    subviewAreas.get(subview.getSubviewArea().index()).addSubview(subview);
    subviews.add(subview);
  }

  public final Subview findSubview(String viewName) {
    Subview subview = null;
    for (Subview sv : subviews) {
      if (sv.getViewName().equals(viewName)) {
        subview = sv;
        break;
      }
    }
    return subview;
  }

  public final Subview findSubview(Component component) {
    Subview subview = null;
    for (Subview sv : subviews) {
      if (sv.getComponent() == component) {
        subview = sv;
        break;
      }
    }
    return subview;
  }

  public void hideAllSubviews() {
    for (Subview subview : subviews) {
      subview.setHidden(true);
    }
  }

  /** Show/hide subviews area depending on any tabs are visible */
  public void showHideSubviewAreas() {
    // For now deal only with subviewArea2....
    getSplitPane1().setHidden(subviewArea1.getTabCount() == 0);
    getSplitPane2().setHidden(subviewArea2.getTabCount() == 0);
    getSplitPane3().setHidden(subviewArea3.getTabCount() == 0);
  }

  public void setHidden(SubviewArea subviewArea, Subview subview, boolean hidden) {
    subviewAreas.get(subviewArea.index()).setHidden(subview, hidden);
  }

  public void setSelected(SubviewArea subviewArea, Subview subview) {
    subviewAreas.get(subviewArea.index()).setSelected(subview);
  }

  public boolean isSelected(SubviewArea subviewArea, Subview subview) {
    return subviewAreas.get(subviewArea.index()).isSelected(subview);
  }

  /** Dump views structure. Used in unit tests. */
  public String dumpMainviewAreaVisibility() {
    StringBuilder buf = new StringBuilder();

    buf.append("MAINVIEW_AREA\n");
    buf.append("  ");
    buf.append(AnWindow.getInstance().getViews().getCurrentViewDisplay().getAnDispTab().getTCmd());
    buf.append("\n");

    return buf.toString();
  }

  /** Dump contents of subviews. Used in unit tests. */
  public String dumpSubviews() {
    StringBuilder buf = new StringBuilder();

    if (!getSplitPane1().isHidden()) {
      buf.append(subviewArea1.dumpSubviews());
      if (!getSplitPane3().isHidden()) {
        buf.append(subviewArea3.dumpSubviews());
      }
    }
    if (!getSplitPane2().isHidden()) {
      buf.append(subviewArea2.dumpSubviews());
    }

    return buf.toString();
  }

  /** Dump views structure. Used in unit tests. */
  public String dumpSubviewAreasVisibility() {
    StringBuilder buf = new StringBuilder();

    if (!getSplitPane1().isHidden()) {
      buf.append(subviewArea1.dumpSubviewsVisibility());
      if (!getSplitPane3().isHidden()) {
        buf.append(subviewArea3.dumpSubviewsVisibility());
      }
    }
    if (!getSplitPane2().isHidden()) {
      buf.append(subviewArea2.dumpSubviewsVisibility());
    }

    return buf.toString();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    setLayout(new java.awt.GridBagLayout());
  } // </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
}

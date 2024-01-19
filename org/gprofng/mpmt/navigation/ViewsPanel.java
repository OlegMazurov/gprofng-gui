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

package org.gprofng.mpmt.navigation;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.AnWindow.AnDispTab;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.persistence.UserPref.ViewPanelOrder;
import org.gprofng.mpmt.settings.ViewsSetting;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class ViewsPanel extends javax.swing.JPanel {

  private List<ViewPanel> viewPanelList = new ArrayList();
  private List<ViewPanel> viewPanelOriginalOrder = new ArrayList();
  private int dropLine = -1;
  private static final int dropLineOffset = 0;
  private int gridY = 2;
  private JDialog viewsSelectorDialog = null;
  private JPopupMenu viewsSelectorpopup = null;
  private Point cancelPoint = null;
  //    private List<ViewPanelOrder> viewPanelOrderList = null;
  private ViewPanel defaultViewPanel = null;
  private JScrollPane scrollPane;
  private ActionPanel moreViewsPanel;
  private static Integer maxMenus = null;
  private int keyboardFocusIndex = 0;

  private Stack<String> viewStack = new Stack<String>();
  private int viewStackPointer = -1;

  public String dumpAvailableViews() {
    StringBuilder buf = new StringBuilder();
    buf.append("--------------------------available views");
    buf.append("\n");
    for (ViewPanel viewPanel : viewPanelList) {
      buf.append((viewPanel.isShown() ? "*" : " ") + viewPanel.getView().getDisplayName());
      buf.append("\n");
    }
    return buf.toString();
  }

  public ViewsPanel(JScrollPane scrollPane) {
    this.scrollPane = scrollPane;
    initComponents();
    moreViewsPanel =
        new ActionPanel(
            AnLocale.getString("More..."),
            AnLocale.getString("Add more views..."),
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                if (source instanceof MouseEvent) {
                  // Mouse click
                  MouseEvent me = (MouseEvent) e.getSource();
                  AnWindow.getInstance()
                      .getViewsPanel()
                      .showViewsConfigurationPopup(
                          moreViewsPanel, me.getPoint().x + 5, me.getPoint().y + 5);
                } else if (source instanceof JComponent) {
                  // Keyboard
                  JComponent component = (JComponent) source;
                  AnWindow.getInstance()
                      .getViewsPanel()
                      .showViewsConfigurationPopup(
                          moreViewsPanel,
                          component.getX() + component.getWidth() - 5,
                          component.getY() - 5);
                }
              }
            });
    setBackground(
        AnEnvironment.NAVIGATION_PANEL_BACKGROUND_COLOR /*AnEnvironment.DEFAULT_PANEL_BACKGROUND*/);
    setDropTarget(new DropTarget(this, new PanelDropTargetListener(this)));

    DragSource.getDefaultDragSource()
        .addDragSourceListener(
            new DragSourceListener() {
              // @Override
              @Override
              public void dragDropEnd(DragSourceDropEvent dsde) {
                //                System.out.println("DragSource:dragDropEnd: " +
                // dsde.getDragSourceContext().getComponent());
                if (dsde.getDragSourceContext().getComponent() instanceof ViewPanel) {
                  dropLine = -1;
                  repaint();
                }
              }

              // @Override
              @Override
              public void dragExit(DragSourceEvent dse) {
                //                System.out.println("DragSource:dragExit: " + dse);
                if (dse.getDragSourceContext().getComponent() instanceof ViewPanel) {
                  dropLine = -1;
                  repaint();
                }
              }

              // @Override
              @Override
              public void dragEnter(DragSourceDragEvent dsde) {
                //                System.out.println("DragSource:dragEnter: " + dsde);
                if (dsde.getDragSourceContext().getComponent() instanceof ViewPanel) {
                  // This is the right kind of drop object
                  int y1 = 0;
                  int y2 = 0;
                  for (int i = 0; i < viewPanelList.size(); i++) {
                    ViewPanel viewPanel = (ViewPanel) viewPanelList.get(i);
                    if (!viewPanel.isShown()) {
                      continue;
                    }
                    y1 = viewPanelList.get(i).getLocationOnScreen().y;
                    y2 = y1 + viewPanelList.get(i).getHeight();
                    if (dsde.getLocation().y >= y1 && dsde.getLocation().y <= y2) {
                      dropLine = y1 - getYLocationOnScreenOfFirstShownView();
                      break;
                    }
                  }
                  if (dropLine < 0) {
                    dropLine = y2 - getYLocationOnScreenOfFirstShownView();
                  }
                  repaint();
                }
              }

              private int getYLocationOnScreenOfFirstShownView() {
                int y = 0;
                for (ViewPanel viewPanel : viewPanelList) {
                  if (viewPanel.isShowing()) {
                    y = viewPanel.getLocationOnScreen().y;
                    break;
                  }
                }
                return y;
              }

              @Override
              public void dragOver(DragSourceDragEvent dsde) {}

              @Override
              public void dropActionChanged(DragSourceDragEvent dsde) {}
            });

    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
              showViewsConfigurationPopup(ViewsPanel.this, e.getPoint().x + 5, e.getPoint().y + 5);
            }
          }
        });

    // A11Y
    KeyStroke keyStroke = KeyboardShortcuts.contextMenuActionShortcut;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, keyStroke);
    getActionMap()
        .put(
            keyStroke,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                final Point loc1 = MouseInfo.getPointerInfo().getLocation();
                final Point loc2 = getLocationOnScreen();
                showViewsConfigurationPopup(
                    ViewsPanel.this, loc1.x - loc2.x + 5, loc1.y - loc2.y + 5);
              }
            });

    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyboardShortcuts.helpActionShortcut, KeyboardShortcuts.helpActionShortcut);
    getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyboardShortcuts.helpActionShortcut, KeyboardShortcuts.helpActionShortcut);
    getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyboardShortcuts.helpActionShortcut, KeyboardShortcuts.helpActionShortcut);
    getActionMap()
        .put(
            KeyboardShortcuts.helpActionShortcut,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                ViewPanel viewPanel = getHasKeyboardFocus();
                if (viewPanel != null) {
                  // Show help for current keyboard focus
                  String name = viewPanel.getView().getName();
                  ViewsSetting.View view =
                      AnWindow.getInstance().getSettings().getViewsSetting().findView(name);
                  if (view != null) {
                    view.getAnDispTab().getTComp().showHelp();
                    return;
                  }
                }
                // Show help for current showing
                AnWindow.getInstance().getViews().getCurrentViewDisplay().showHelp();
              }
            });

    addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            handleKeyEvent(e);
          }
        });

    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            keyboardFocusIndex = getSelected();
            makeVisible(keyboardFocusIndex);
            viewPanelList.get(keyboardFocusIndex).setKeyboardFocused(true);
            viewPanelList.get(keyboardFocusIndex).repaint();
          }

          @Override
          public void focusLost(FocusEvent e) {
            resetKeyboardFocus();
          }
        });
  }

  void handleKeyEvent(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) {
      if (keyboardFocusIndex >= 0 && keyboardFocusIndex < viewPanelList.size()) {
        viewPanelList.get(keyboardFocusIndex).setKeyboardFocused(false);
        viewPanelList.get(keyboardFocusIndex).repaint();
      } else {
        moreViewsPanel.setKeyboardFocused(false);
        moreViewsPanel.repaint();
      }
      if (e.getKeyCode() == KeyEvent.VK_DOWN) {
        keyboardFocusIndex = nextKeyboardFocused(keyboardFocusIndex);
      } else if (e.getKeyCode() == KeyEvent.VK_UP) {
        keyboardFocusIndex = previousKeyboardFocused(keyboardFocusIndex);
      }
      if (keyboardFocusIndex >= 0 && keyboardFocusIndex < viewPanelList.size()) {
        viewPanelList.get(keyboardFocusIndex).setKeyboardFocused(true);
        viewPanelList.get(keyboardFocusIndex).repaint();
      } else {
        moreViewsPanel.setKeyboardFocused(true);
        moreViewsPanel.repaint();
      }
      e.consume();
      makeVisible(keyboardFocusIndex);
    } else if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
      if (keyboardFocusIndex < viewPanelList.size()) {
        ViewPanel viewPanel = viewPanelList.get(keyboardFocusIndex);
        String name = viewPanel.getView().getName();
        AnWindow.getInstance().setSelectedView(name);
      } else {
        moreViewsPanel.performAction();
      }
      e.consume();
    }
  }

  private int getRelativeYLocation(int index) {
    int y = 0;
    for (int i = 0; i < index; i++) {
      ViewPanel viewPanel = viewPanelList.get(i);
      if (viewPanel.isShowing()) {
        y += viewPanel.getHeight();
      }
    }
    return y;
  }

  private void makeVisible(int index) {
    int y = getRelativeYLocation(index);
    Rectangle rectangle = new Rectangle(0, y - 5, 10, ViewPanel.getPanelHeight() + 10);
    scrollRectToVisible(rectangle);
  }

  private int previousKeyboardFocused(int currentKeyboardFocusIndex) {
    int n = currentKeyboardFocusIndex - 1;
    while (n >= 0 && !viewPanelList.get(n).isShown()) {
      n--;
    }
    if (n >= 0) {
      return n;
    } else {
      return currentKeyboardFocusIndex;
    }
  }

  private int nextKeyboardFocused(int currentKeyboardFocusIndex) {
    int n = currentKeyboardFocusIndex + 1;
    while (n < viewPanelList.size() && !viewPanelList.get(n).isShown()) {
      n++;
    }
    if (n < viewPanelList.size()) {
      return n;
    } else {
      return viewPanelList.size();
    }
  }

  private void resetKeyboardFocus() {
    for (ViewPanel viewPanel : viewPanelList) {
      if (viewPanel.hasKeyboardFocus()) {
        viewPanel.setKeyboardFocused(false);
        viewPanel.repaint();
      }
    }
    if (moreViewsPanel.hasKeyboardFocus()) {
      moreViewsPanel.setKeyboardFocused(false);
      moreViewsPanel.repaint();
    }
    keyboardFocusIndex = -1;
  }

  public ViewPanel getHasKeyboardFocus() {
    for (ViewPanel viewPanel : viewPanelList) {
      if (viewPanel.hasKeyboardFocus()) {
        return viewPanel;
      }
    }
    return null;
  }

  public ViewPanel getIsFocused() {
    for (ViewPanel viewPanel : viewPanelList) {
      if (viewPanel.isFocused()) {
        return viewPanel;
      }
    }
    return null;
  }

  public ViewPanel getIsSelected() {
    for (ViewPanel viewPanel : viewPanelList) {
      if (viewPanel.isSelected()) {
        return viewPanel;
      }
    }
    return null;
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if (dropLine >= 0) {
      g.setColor(AnEnvironment.DROP_LINE_COLOR);
      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(new BasicStroke(2));
      g.drawLine(0, dropLine + dropLineOffset, getSize().width, dropLine + dropLineOffset);
    }

    int i;
    for (i = viewPanelList.size() - 1; i >= 0; i--) {
      ViewPanel viewPanel = (ViewPanel) viewPanelList.get(i);
      if (viewPanel.isShown()) {
        break;
      }
    }
  }

  private JComponent addViewPanelInternal(JComponent viewPanel) {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridY++;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    add(viewPanel, gridBagConstraints);
    return viewPanel;
  }

  private void removeAllPanel() {
    for (ViewPanel vp : viewPanelList) {
      remove(vp);
    }
    remove(moreViewsPanel);
  }

  // API begin ----------------------------------------------------
  public void removeAllViews() {
    removeAllPanel();
    viewPanelList.clear();
    viewPanelOriginalOrder.clear();
    relayout();
    defaultViewPanel = null;
    emptyViewStack();
  }

  public void createView(AnWindow anWindow, AnDispTab anDispTab, View.Type type) {
    //        System.out.println("ViewPanels:createView: " + anDispTab.getTCmd());
    if (findIndexViewPanel(anDispTab.getTCmd()) >= 0) {
      // Already there...
      return;
    }
    View view =
        new View(
            anDispTab.getTCmd(),
            anDispTab.getTName(),
            anDispTab.getShortDescr(),
            anDispTab.getLongDesc(),
            anWindow,
            type,
            null /*View.generateIconPath()*/);
    int oldPosition = -1;
    int thisPostion = 0;
    // anWindow.presentation may be null if IPC is not started yet
    boolean showTab = false;
    if (Analyzer.getInstance().IPC_started) {
      showTab = anWindow.getSettings().getViewsSetting().isAvailableAndShowing(anDispTab.getTCmd());
    }

    if (getSavedViewPanelOrderList() != null && !getSavedViewPanelOrderList().isEmpty()) {
      int index = 0;
      for (ViewPanelOrder cpo : getSavedViewPanelOrderList()) {
        if (cpo.getName().equals(anDispTab.getTCmd())) {
          oldPosition = index;
          showTab = cpo.isShown();
          break;
        }
        index++;
      }
      for (; thisPostion < viewPanelList.size(); thisPostion++) {
        if (viewPanelList.get(thisPostion).getPosition() > oldPosition) {
          break;
        }
      }
    }
    if (oldPosition == -1) {
      oldPosition = viewPanelList.size();
      thisPostion = viewPanelList.size();
    }
    ViewPanel viewPanel = new ViewPanel(view);
    viewPanel.setPosition(oldPosition);
    viewPanel.setSidePanel(this);
    viewPanel.setShown(showTab);
    viewPanelList.add(thisPostion, viewPanel);
    viewPanelOriginalOrder.add(viewPanel);

    // Make the first standard tab the default view to select.
    if (defaultViewPanel == null && type == View.Type.STANDARD) {
      defaultViewPanel = viewPanel;
    }
  }

  public ViewPanel getDefaultViewPanel() {
    return defaultViewPanel;
  }

  public void resetAllViews() {
    removeAllPanel();
    for (ViewPanel viewPanel : viewPanelList) {
      viewPanel.setShown(false);
    }
    relayout();
  }

  public void showView(AnDispTab anDispTab) {
    ViewPanel viewPanel = findViewPanel(anDispTab.getTCmd());
    if (viewPanel != null) {
      viewPanel.setShown(true);
    }
  }

  public void refresh() {
    //        int i = getSelected();
    //        if (i < 0 || !viewPanelList.get(i).isShown()) {
    //            for (ViewPanel viewPanel : viewPanelList) {
    //                if (viewPanel.isShown()) {
    //                    setSelected(viewPanel, true);
    //                    break;
    //                }
    //            }
    //        }
    relayout();

    getScrollPane().setVisible(viewPanelList.size() > 1);
  }

  public void setSelectedComponent(String cmd) {
    //        System.out.println("ViewsPanel setSelectedComponent " + cmd);
    for (int i = 0; i < viewPanelList.size(); i++) {
      ViewPanel viewPanel = viewPanelList.get(i);
      if (viewPanel.getView().getName().equals(cmd)) {
        if (!viewPanel.isSelected()) {
          setSelected(i);
        }
        makeVisible(i);

        if (viewStackPointer >= 0
            && viewStackPointer < viewStack.size()
            && viewStack.elementAt(viewStackPointer).equals(cmd)) {
          // nothing, just moving up or down the stack
        } else {
          // start a 'new' stack
          pushViewToViewStack(cmd);
        }

        break;
      }
    }
  }

  public void moveViewStackPointer(int x) {
    viewStackPointer = viewStackPointer + x;
  }

  private void emptyViewStack() {
    viewStack.removeAllElements();
    viewStackPointer = -1;
  }

  private void pushViewToViewStack(String cmd) {
    if (viewStackPointer != (viewStack.size() - 1)) {
      for (int index = viewStack.size() - 1; index > viewStackPointer; index--) {
        viewStack.remove(index);
      }
    }
    viewStack.push(cmd);
    viewStackPointer++;
  }

  public String nextViewName() {
    String nextViewName = null;
    if (viewStack.size() > 0
        && (viewStackPointer + 1) >= 0
        && (viewStackPointer + 1) < viewStack.size()) {
      nextViewName = viewStack.elementAt(viewStackPointer + 1);
    }
    return nextViewName;
  }

  public String previousViewName() {
    String previousViewName = null;
    if (viewStack.size() > 0
        && (viewStackPointer - 1) >= 0
        && (viewStackPointer - 1) < viewStack.size()) {
      previousViewName = viewStack.elementAt(viewStackPointer - 1);
    }
    return previousViewName;
  }

  public boolean hasNextViewName() {
    String nextViewName = nextViewName();
    return nextViewName != null;
  }

  public boolean hasPreviousViewName() {
    String previousViewName = previousViewName();
    return previousViewName != null;
  }

  /**
   * Called when saving to user pref
   *
   * @return
   */
  public List<ViewPanelOrder> getViewPanelOrderPref() {
    if (viewPanelList == null || viewPanelList.isEmpty()) {
      return getSavedViewPanelOrderList(); // from user pref.
    }
    List<ViewPanelOrder> list = new ArrayList();
    for (ViewPanel viewPanel : viewPanelList) {
      list.add(new ViewPanelOrder(viewPanel.getView().getName(), viewPanel.isShown()));
    }
    return list;
  }

  public boolean onlyStaticViews() {
    boolean ret = true;
    for (ViewPanel viewPanel : viewPanelList) {
      if (viewPanel.getView().getType() != View.Type.STATIC_VIEW) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  public boolean onlyWelcomeView() {
    boolean ret = true;
    for (ViewPanel viewPanel : viewPanelList) {
      if (!viewPanel.getView().getName().equals("welcome")) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  // API end ----------------------------------------------------
  private void relayout() {
    gridY = 2;
    for (ViewPanel vp : viewPanelList) {
      if (vp.isShown()) {
        addViewPanelInternal(vp);
      }
    }
    addViewPanelInternal(moreViewsPanel);
    refreshInternal();
  }

  private void refreshInternal() {
    validate();
    repaint(100);
  }

  private int findViewPanel(ViewPanel viewPanel) {
    int i = 0;
    for (i = 0; i < viewPanelList.size(); i++) {
      if (viewPanelList.get(i) == viewPanel) {
        return i;
      }
    }
    return -1;
  }

  private ViewPanel findViewPanel(String name) {
    for (int i = 0; i < viewPanelList.size(); i++) {
      if (viewPanelList.get(i).getView().getName().equals(name)) {
        return viewPanelList.get(i);
      }
    }
    return null;
  }

  private int findIndexViewPanel(String name) {
    for (int i = 0; i < viewPanelList.size(); i++) {
      if (viewPanelList.get(i).getView().getName().equals(name)) {
        return i;
      }
    }
    return -1;
  }

  public void addViewPanelAt(int index, ViewPanel viewPanel) {
    viewPanel.setSidePanel(this);
    viewPanelList.add(index, viewPanel);
    removeAllPanel();
    relayout();
  }

  public void removeViewPanel(ViewPanel viewPanel) {
    viewPanel.setShown(false);
    removeAllPanel();
    relayout();
    if (viewPanel.isSelected()) {
      for (ViewPanel vp : viewPanelList) {
        if (vp.isShown() && vp != viewPanel) {
          setSelected(vp, true);
          break;
        }
      }
    }
  }

  protected void setSelected(ViewPanel viewPanel, boolean performAction) {
    if (viewsSelectorDialog != null && viewsSelectorDialog.isVisible()) {
      viewsSelectorDialog.setVisible(false);
      viewsSelectorDialog = null;
    }
    for (ViewPanel vp : viewPanelList) {
      vp.setSelected(vp == viewPanel);
      if (performAction && vp == viewPanel) {
        View view = viewPanel.getView();
        viewPanel.getView().getAnWindow().setSelectedView(view.getName());
      }
    }
  }

  public int getSelected() {
    int sel = -1;
    int i = 0;
    for (ViewPanel vp : viewPanelList) {
      if (vp.isSelected()) {
        sel = i;
      }
      i++;
    }
    return sel;
  }

  public ViewPanel getSelectedViewPanel() {
    ViewPanel sel = null;
    for (ViewPanel vp : viewPanelList) {
      if (vp.isSelected()) {
        sel = vp;
        break;
      }
    }
    return sel;
  }

  private void setSelected(int panelNo) {
    ViewPanel vp = viewPanelList.get(panelNo);
    setSelected(vp, false);
  }

  protected void dropPanelTo(ViewPanel dropViewPanel, Component dropTargetComponent) {
    int moveFromIndex = findViewPanel(dropViewPanel);
    int moveToIndex;
    if (dropTargetComponent instanceof ViewPanel) {
      ViewPanel targetPanel = (ViewPanel) dropTargetComponent;
      moveToIndex = findViewPanel(targetPanel);
    } else {
      moveToIndex = viewPanelList.size();
      while (moveToIndex > 0 && !viewPanelList.get(moveToIndex - 1).isShown()) {
        moveToIndex--;
      }
    }

    if (moveToIndex < 0) {
      // something is wrong
      return;
    }
    if (moveFromIndex == moveToIndex) {
      return;
    }

    if (moveFromIndex >= 0) {
      viewPanelList.remove(moveFromIndex);
    }
    if (moveFromIndex < moveToIndex) {
      moveToIndex--;
    }
    addViewPanelAt(moveToIndex, dropViewPanel);
    refreshInternal();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    filler1 =
        new javax.swing.Box.Filler(
            new java.awt.Dimension(0, 0),
            new java.awt.Dimension(0, 0),
            new java.awt.Dimension(0, 32767));

    setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 100;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.weighty = 1.0;
    add(filler1, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  public void toggleViewsConfigurationPopup(Component component, int x, int y) {
    Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
    if (cancelPoint != null
        && mouseLocation.x == cancelPoint.x
        && mouseLocation.y == cancelPoint.y) {
      cancelPoint = null;
      return;
    }
    showViewsConfigurationPopup(component, x, y);
    cancelPoint = null;
  }

  public void showViewsConfigurationPopup(Component component, int x, int y) {
    ViewPanel viewPanel;
    viewPanel = getHasKeyboardFocus();
    if (viewPanel == null) {
      viewPanel = getIsFocused();
    }
    if (viewPanel == null) {
      viewPanel = getIsSelected();
    }
    viewsSelectorpopup = getViewSelectorPopup(viewPanel != null ? viewPanel.getView() : null);
    viewsSelectorpopup.show(component, x, y);
  }

  private int maxMenus() {
    if (maxMenus == null) {
      JPopupMenu popup = new JPopupMenu();
      for (int i = 0; i < 10; i++) {
        popup.add(new JMenu("XXX"));
      }
      int height = popup.getPreferredSize().height;
      maxMenus = AnVariable.SCREEN_SIZE.height / height * 10 - 5;
    }
    return maxMenus;
  }

  private JPopupMenu getViewSelectorPopup(final View view) {
    JPopupMenu popup = new JPopupMenu();
    JMenu moreMenu = new JMenu(AnLocale.getString("More..."));

    if (view != null) {
      String txt = String.format(AnLocale.getString("Help %s View"), view.getDisplayName());
      JMenuItem helpMenuItem = new JMenuItem(txt);
      helpMenuItem.setMnemonic(AnLocale.getString('H', "HelpMenuInViewContextMenu"));
      helpMenuItem.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              ViewsSetting.View view2 =
                  AnWindow.getInstance().getSettings().getViewsSetting().findView(view.getName());
              if (view2 != null) {
                view2.getAnDispTab().getTComp().showHelp();
              }
            }
          });
      popup.add(helpMenuItem);
      popup.add(new JSeparator());
    }
    int count = 0;
    for (ViewPanel viewPanel : viewPanelList) {
      if (count > maxMenus()) {
        moreMenu.add(addViewSelector(viewPanel));
      } else {
        popup.add(addViewSelector(viewPanel));
      }
      count++;
    }
    if (moreMenu.getSubElements().length > 0) {
      //            popup.addSeparator();
      popup.add(moreMenu);
    }
    popup.addPopupMenuListener(
        new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {
            cancelPoint = MouseInfo.getPointerInfo().getLocation();
          }
        });

    return popup;
  }

  private JCheckBoxMenuItem addViewSelector(final ViewPanel viewPanel) {
    JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(viewPanel.getView().getDisplayName());
    checkBox.setSelected(viewPanel.isShown());
    checkBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String preferredViewName;
            if (viewPanel.isShown()) {
              preferredViewName = AnWindow.getInstance().getViews().getCurrentViewName();
            } else {
              preferredViewName = viewPanel.getView().getName();
            }
            AnWindow.getInstance()
                .getSettings()
                .getViewsSetting()
                .toggleTab(viewPanel.getView().getName(), preferredViewName);
            SwingUtilities.invokeLater(
                new Runnable() {
                  @Override
                  public void run() {
                    refreshInternal();
                  }
                });
          }
        });
    return checkBox;
  }

  private class MenuItem extends JMenuItem {

    public MenuItem(String text) {
      super(text);
    }

    @Override
    public JToolTip createToolTip() {
      JToolTip toolTip = super.createToolTip();
      toolTip.setBackground(AnEnvironment.TOOLTIP_VIEW_BACKGROUND_COLOR);
      return toolTip;
    }
  }

  public void createViewsMenu(JMenu viewsMenu) {
    int count = 0;
    JMenu moreMenu = new JMenu(AnLocale.getString("More..."));
    viewsMenu.removeAll();
    if (!viewPanelOriginalOrder.isEmpty()) {
      View.Type type = viewPanelOriginalOrder.get(0).getView().getType();
      for (ViewPanel viewPanel : viewPanelOriginalOrder) {
        JMenuItem menuItem = new MenuItem(viewPanel.getView().getDisplayName());
        //                menuItem.setToolTipText(viewPanel.getView().getLongDescr());
        menuItem.addActionListener(new ViewMenuActionListener(viewPanel));
        if (count > maxMenus()) {
          moreMenu.add(menuItem);
        } else {
          if (viewPanel.getView().getType() != type) {
            viewsMenu.addSeparator();
          }
          viewsMenu.add(menuItem);
          type = viewPanel.getView().getType();
        }
        count++;
      }
      if (moreMenu.getSubElements().length > 0) {
        //                viewsMenu.addSeparator();
        viewsMenu.add(moreMenu);
      }
    }

    viewsMenu.add(new JSeparator(SwingConstants.HORIZONTAL));
    JMenuItem previousViewMenuItem = AnWindow.getInstance().getPreviousViewAction().getMenuItem();
    previousViewMenuItem.setIcon(null);
    viewsMenu.add(previousViewMenuItem);
    JMenuItem nextViewMenuItem = AnWindow.getInstance().getNextViewAction().getMenuItem();
    nextViewMenuItem.setIcon(null);
    viewsMenu.add(nextViewMenuItem);
    for (JComponent component :
        AnWindow.getInstance().getSettings().getViewsSetting().createViewsSettingsSelector()) {
      viewsMenu.add(component);
    }
  }

  /**
   * @return the scrollPane
   */
  public JScrollPane getScrollPane() {
    return scrollPane;
  }

  public void selectView(String name) {
    ViewPanel viewPanel = findViewPanel(name);
    if (viewPanel != null) {
      selectView(viewPanel);
    }
  }

  private void selectView(ViewPanel viewPanel) {
    if (!viewPanel.isShown()) {
      AnWindow.getInstance()
          .getSettings()
          .getViewsSetting()
          .toggleTab(viewPanel.getView().getName(), viewPanel.getView().getName());
    } else {
      setSelected(viewPanel, true);
    }
  }

  /**
   * @return the viewPanelOrderList
   */
  public List<ViewPanelOrder> getSavedViewPanelOrderList() {
    return AnWindow.getInstance().getSettings().getViewsSetting().getSavedViewOrderList();
  }

  class ViewMenuActionListener implements ActionListener {

    private ViewPanel viewPanel;

    public ViewMenuActionListener(ViewPanel viewPanel) {
      this.viewPanel = viewPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      selectView(viewPanel);
    }
  }

  class MoreViewsPanel extends JPanel {

    public MoreViewsPanel() {
      setLayout(new GridBagLayout());
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      JLabel label = new JLabel("More Views...");
      AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
      gridBagConstraints.insets = new Insets(4, 4, 4, 4);
      add(label, gridBagConstraints);
    }
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.Box.Filler filler1;
  // End of variables declaration//GEN-END:variables
}

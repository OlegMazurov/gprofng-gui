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


package org.gprofng.mpmt.filter;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnIconButton;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.statuspanel.StatusLabelValueHandle;
import org.gprofng.mpmt.util.gui.AnGradientPanel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class FilterStatusPanel extends JPanel implements AnChangeListener {

  private JButton undoButton;
  private JButton resetButton;
  private JButton redoButton;
  private JButton manageButton;
  private JTextArea infoArea;
  private final AnWindow anWindow;
  private List<FilterTextField> filterTextFields = new ArrayList<FilterTextField>();
  private JPanel listPanel;
  private JScrollPane scrollPane;
  private int gridy = 0;

  /** Creates new form FilterStatusPanel */
  public FilterStatusPanel(final AnWindow anWindow) {
    this.anWindow = anWindow;
    initComponents();

    filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));
    filterLabel.setForeground(AnEnvironment.FILTER_HEADER_COLOR);

    //        setBackground(AnEnvironment.NAVIGATION_PANEL_BACKGROUND_2_COLOR);
    setOpaque(false);
    //        setOpaque(true);
    //        setBackground(Color.yellow);
    updateLabel(0);
    GridBagConstraints gridBagConstraints;

    // Button panel
    undoButton = new AnIconButton(AnUtility.undo_icon);
    undoButton.setToolTipText(AnLocale.getString("Undo filter action"));
    undoButton.addActionListener(new UndoFilterAction());

    resetButton = new AnIconButton(AnUtility.restore_icon);
    resetButton.setToolTipText(AnLocale.getString("Remove all filters"));
    resetButton.addActionListener(new RemoveAllFilterAction());

    redoButton = new AnIconButton(AnUtility.redo_icon);
    redoButton.setToolTipText(AnLocale.getString("Redo filter action"));
    redoButton.addActionListener(new RedoFilterAction());

    manageButton = new AnIconButton(AnUtility.compareHamburgerIcon);
    manageButton.setToolTipText(AnLocale.getString("Add or remove filters"));
    manageButton.addActionListener(
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            anWindow.getViews().getCurrentViewDisplay().showFilterPopup(manageButton);
          }
        });

    JPanel buttonPanel =
        new AnGradientPanel(
            AnEnvironment.FILTER_TOOLBAR_BACKGROUND1_COLOR,
            AnEnvironment.FILTER_TOOLBAR_BACKGROUND2_COLOR);
    buttonPanel.setBackground(AnEnvironment.NAVIGATION_PANEL_SECTION_BACKGROUND_COLOR);
    buttonPanel.setLayout(new GridBagLayout());
    buttonPanel.setBorder(
        BorderFactory.createMatteBorder(1, 1, 1, 1, AnEnvironment.FILTER_STATUS_BORDER_COLOR));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(3, 3, 2, 0);
    buttonPanel.add(undoButton, gridBagConstraints);
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(3, 3, 2, 0);
    buttonPanel.add(redoButton, gridBagConstraints);
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(3, 3, 2, 0);
    buttonPanel.add(resetButton, gridBagConstraints);
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(3, 0, 2, 3);
    buttonPanel.add(manageButton, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    //        add(buttonPanel, gridBagConstraints);

    // Scroll pane
    scrollPane = new JScrollPane();
    scrollPane.setBorder(
        BorderFactory.createMatteBorder(0, 1, 1, 1, AnEnvironment.FILTER_STATUS_BORDER_COLOR));
    scrollPane.setHorizontalScrollBarPolicy(
        javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    //        add(scrollPane, gridBagConstraints);

    // Buttons and filters panel
    JPanel buttonAndFiltersPanel = new JPanel(new GridBagLayout());
    buttonAndFiltersPanel.setBackground(AnEnvironment.NAVIGATION_PANEL_SECTION_BACKGROUND_COLOR);
    //        buttonAndFiltersPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
    // AnEnvironment.NAVIGATION_SPLIT_PANE_DIVIDER_COLOR));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(3, 5, 0, 4);
    buttonAndFiltersPanel.add(buttonPanel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(0, 5, 5, 4);
    buttonAndFiltersPanel.add(scrollPane, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    add(buttonAndFiltersPanel, gridBagConstraints);

    // List panel
    listPanel = new JPanel();
    listPanel.setLayout(new java.awt.GridBagLayout());
    listPanel.setBackground(AnEnvironment.FILTER_BACKGROUND1_COLOR);

    // Info text area
    String infoText =
        AnLocale.getString(
            "To add a filter, select a row from a view (such as Functions) and then click on the"
                + " toolbar Filters icon.");
    infoArea = new JTextArea();
    AnUtility.setAccessibleContext(
        infoArea.getAccessibleContext(), AnLocale.getString("Info Area"));
    infoArea.setEditable(false);
    infoArea.setWrapStyleWord(true);
    infoArea.setLineWrap(true);
    infoArea.setBackground(AnEnvironment.FILTER_BACKGROUND1_COLOR);
    infoArea.setForeground(AnEnvironment.FILTER_INFO_TEXT_COLOR);
    infoArea.setMargin(new Insets(2, 2, 2, 2));
    infoArea.setText(infoText);
    infoArea.setToolTipText(infoText);
    infoArea.setFont(getFont().deriveFont((float) (getFont().getSize() - 1)));

    AnEventManager.getInstance().addListener(this);
    updateStatus(false);

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
                Analyzer.getInstance().showHelp(null);
              }
            });
  }

  /**
   * AnChangeEvent handler. Events are always dispatched on AWT thread. Requirements to an event
   * handler like this one: o When it done handling the event and returns, all it's states need to
   * be *consistent* and *well-defined*. It cannot dispatch that or part of that work to a different
   * (worker) thread and not wait for it to finish. o It cannot spend significant time handling the
   * event. Significant work like doCompute needs to be dispatched to a worker thread.
   *
   * @param e the event
   */
  @Override
  public void stateChanged(AnChangeEvent e) {
    //        System.out.println("FilterStatusPanel stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
        undoButton.setEnabled(false);
        resetButton.setEnabled(false);
        redoButton.setEnabled(false);
        manageButton.setEnabled(false);
        removeAllFilterTextFields();
        listPanel.validate();
        listPanel.repaint();
        break;
      case EXPERIMENTS_LOADED_FAILED:
      case EXPERIMENTS_LOADED:
        manageButton.setEnabled(true);
        // Nothing
        break;
      case FILTER_CHANGED:
        Boolean showTooltip = (Boolean) e.getSource();
        updateStatus(showTooltip);
        break;
      case FILTER_CHANGING:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case SETTING_CHANGED:
      case SOURCE_FINDING_CHANGING:
      case SOURCE_FINDING_CHANGED:
      case SETTING_CHANGING:
      case SELECTED_OBJECT_CHANGED:
      case SELECTION_CHANGING:
      case SELECTION_CHANGED:
      case SELECTION_UPDATE:
        // Nothing
        break;
      case DEBUG:
        debug();
        break;
      default:
        System.err.println("AnChanged event " + e.getType() + " not handled: " + this);
        break;
    }
  }

  private void debug() {
    System.out.println(this.getClass().getSimpleName());
  }

  private void removeAllFilterTextFields() {
    listPanel.removeAll();
    filterTextFields = new ArrayList<FilterTextField>();
    gridy = 0;
  }

  private FilterTextField addFilterTextField(FilterClause clause) {
    Color background;
    if (gridy % 2 == 0) {
      background = AnEnvironment.FILTER_BACKGROUND1_COLOR;
    } else {
      background = AnEnvironment.FILTER_BACKGROUND2_COLOR;
    }
    FilterTextField filterTextField =
        new FilterTextField(anWindow.getFilters(), background, clause);
    filterTextField.setText(clause.getShortNameWithSerialNumber());
    //        filterTextField.setToolTipText((clause.getLongName() != null ? clause.getLongName() :
    // "???") + ": " + clause.getClause());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    listPanel.add(filterTextField, gridBagConstraints);
    filterTextFields.add(filterTextField);
    return filterTextField;
  }

  private void addFiller() {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 100;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    listPanel.add(new JLabel(), gridBagConstraints);
  }

  private void updateLabel(int noFilters) {
    StringBuilder text = new StringBuilder();
    text.append(AnLocale.getString("Filters"));
    if (noFilters == 1) {
      text.append(": " + noFilters + " ");
      text.append(AnLocale.getString("active filter"));
    } else if (noFilters > 1) {
      text.append(": " + noFilters + " ");
      text.append(AnLocale.getString("active filters"));
    }
    filterLabel.setText(text.toString());
    AnUtility.setAccessibleContext(filterLabel.getAccessibleContext(), filterLabel.getText());
    filterLabel.setDisplayedMnemonic(AnLocale.getString('i', "FilterPanelActiveFiltersMN"));
    filterLabel.setLabelFor(manageButton);
  }

  private void updateStatus(final boolean showTooltip) {
    // The IPC call below should be done on a worker thread
    //        if (!anWindow.getAnalyzer().IPC_started) {
    //            return;
    //        }
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            final List<FilterClause> list = anWindow.getFilters().getFilters();

            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  @Override
                  public void run() {
                    undoButton.setEnabled(anWindow.getFilters().canUndoLastFilter());
                    redoButton.setEnabled(anWindow.getFilters().canRedoLastFilter());
                    if (list.isEmpty()) {
                      resetButton.setEnabled(false);
                      anWindow
                          .getFilterStatusHandle()
                          .update(AnLocale.getString("off"), StatusLabelValueHandle.Mode.DEFAULT);
                    } else {
                      resetButton.setEnabled(true);
                      anWindow
                          .getFilterStatusHandle()
                          .update(AnLocale.getString("on"), StatusLabelValueHandle.Mode.SET);
                    }

                    removeAllFilterTextFields();
                    if (list.isEmpty()) {
                      scrollPane.setVerticalScrollBarPolicy(
                          javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                      scrollPane.setViewportView(infoArea);
                    } else {
                      scrollPane.setVerticalScrollBarPolicy(
                          javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                      scrollPane.setViewportView(listPanel);
                      for (FilterClause clause : list) {
                        addFilterTextField(clause);
                      }
                      addFiller();
                    }
                    updateLabel(list.size());
                    validate();
                    repaint();
                    if (showTooltip && filterTextFields.size() > 0) {
                      filterTextFields.get(0).showTooltipPopupAdded();
                    }
                  }
                });
          }
        },
        "Filter_thread");
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    filterLabel = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    filterLabel.setText("NOI18N");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(3, 6, 0, 0);
    add(filterLabel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel filterLabel;
  // End of variables declaration//GEN-END:variables
}

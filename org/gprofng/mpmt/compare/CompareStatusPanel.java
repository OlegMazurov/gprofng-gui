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

package org.gprofng.mpmt.compare;

import static org.gprofng.mpmt.event.AnChangeEvent.Type.DEBUG;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.EXPERIMENTS_LOADED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.EXPERIMENTS_LOADED_FAILED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.EXPERIMENTS_LOADING_ADDED_OR_REMOVED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.EXPERIMENTS_LOADING_NEW;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.FILTER_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.FILTER_CHANGING;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.MOST_RECENT_EXPERIMENT_LIST_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.REMOTE_CONNECTION_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.REMOTE_CONNECTION_CHANGING;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SELECTED_OBJECT_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SELECTION_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SELECTION_CHANGING;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SELECTION_UPDATE;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGING;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SOURCE_FINDING_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SOURCE_FINDING_CHANGING;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnIconButton;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.settings.CompareModeSetting;
import org.gprofng.mpmt.settings.CompareModeSetting.CompareMode;
import org.gprofng.mpmt.util.gui.AnGradientPanel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CompareStatusPanel extends javax.swing.JPanel implements AnChangeListener {
  private javax.swing.JLabel compareLabel;
  private ToolbarToggleButton absoluteButton;
  private ToolbarToggleButton deltaButton;
  private ToolbarToggleButton ratioButton;
  private JButton reverseButton;
  private JButton configureButton;
  private CompareGroupsPanel compareGroupsPanel;
  private int initialHeight;

  /** Creates new form CompareNavigationPanel */
  public CompareStatusPanel() {
    initComponents();
    setOpaque(false);

    GridBagConstraints gridBagConstraints = new GridBagConstraints();

    // Label
    compareLabel = new JLabel();
    compareLabel.setText(AnLocale.getString("Compare"));
    AnUtility.setAccessibleContext(compareLabel.getAccessibleContext(), compareLabel.getText());
    compareLabel.setFont(compareLabel.getFont().deriveFont(Font.BOLD));
    compareLabel.setForeground(AnEnvironment.FILTER_HEADER_COLOR);
    compareLabel.setDisplayedMnemonic(AnLocale.getString('p', "CompareStatusPanelMN"));
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    add(compareLabel, gridBagConstraints);

    // Button panel
    absoluteButton = new ToolbarToggleButton(AnUtility.compareAbsoluteIcon);
    absoluteButton.setToolTipText(AnLocale.getString("Absolute: show absolute numbers in tables"));
    absoluteButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            CompareModeSetting compareModeSetting =
                AnWindow.getInstance().getSettings().getCompareModeSetting();
            if (compareModeSetting.get() != CompareModeSetting.CompareMode.CMP_ENABLE) {
              compareModeSetting.set(
                  CompareStatusPanel.this, CompareModeSetting.CompareMode.CMP_ENABLE);
            }
          }
        });
    deltaButton = new ToolbarToggleButton(AnUtility.compareDeltaIcon);
    deltaButton.setToolTipText(
        AnLocale.getString("Delta: show numbers in tables as +/- difference"));
    deltaButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            CompareModeSetting compareModeSetting =
                AnWindow.getInstance().getSettings().getCompareModeSetting();
            if (compareModeSetting.get() != CompareModeSetting.CompareMode.CMP_DELTA) {
              compareModeSetting.set(
                  CompareStatusPanel.this, CompareModeSetting.CompareMode.CMP_DELTA);
            }
          }
        });
    ratioButton = new ToolbarToggleButton(AnUtility.compareRatioIcon);
    ratioButton.setToolTipText(AnLocale.getString("Ratio: show numbers in tables as ratio"));
    ratioButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            CompareModeSetting compareModeSetting =
                AnWindow.getInstance().getSettings().getCompareModeSetting();
            if (compareModeSetting.get() != CompareModeSetting.CompareMode.CMP_RATIO) {
              compareModeSetting.set(
                  CompareStatusPanel.this, CompareModeSetting.CompareMode.CMP_RATIO);
            }
          }
        });
    reverseButton = new AnIconButton(AnUtility.compareReverseIcon);
    reverseButton.setToolTipText(AnLocale.getString("Reverse comparison order"));
    reverseButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow anWindow = AnWindow.getInstance();
            String groups[][] = anWindow.getExperimentGroups();
            int nGroups = groups.length;
            if (nGroups > 1) {
              String newGroups[][] = new String[nGroups][];
              for (int n = 0; n < nGroups; n++) {
                newGroups[n] = groups[nGroups - 1 - n];
              }
              anWindow.loadExperimentGroups(
                  newGroups, null, !anWindow.experimentsLoaded(), null, false);
            }
          }
        });
    configureButton = new AnIconButton(AnUtility.compareHamburgerIcon);
    compareLabel.setLabelFor(configureButton);
    configureButton.setToolTipText(AnLocale.getString("Configure comparison"));
    configureButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance().compareExperimentsAction();
          }
        });

    JPanel buttonPanel =
        new AnGradientPanel(
            AnEnvironment.FILTER_TOOLBAR_BACKGROUND1_COLOR,
            AnEnvironment.FILTER_TOOLBAR_BACKGROUND2_COLOR);
    buttonPanel.setLayout(new GridBagLayout());
    buttonPanel.setBorder(
        BorderFactory.createMatteBorder(1, 1, 1, 1, AnEnvironment.FILTER_STATUS_BORDER_COLOR));
    gridBagConstraints = new GridBagConstraints();

    int gridX = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = gridX++;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(2, 3, 2, 0);
    buttonPanel.add(absoluteButton, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = gridX++;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(2, 2, 2, 0);
    buttonPanel.add(deltaButton, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = gridX++;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(2, 2, 2, 0);
    buttonPanel.add(ratioButton, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.gridx = gridX++;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 2, 2, 0);
    buttonPanel.add(reverseButton, gridBagConstraints);

    //        gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.gridx = gridX++;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 0.0;
    gridBagConstraints.insets = new Insets(2, 0, 2, 3);
    buttonPanel.add(configureButton, gridBagConstraints);

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
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    buttonAndFiltersPanel.add(buttonPanel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(3, 0, 0, 0);
    add(buttonAndFiltersPanel, gridBagConstraints);

    CompareGroupsPanel compareGroupsPanel = new CompareGroupsPanel();
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    add(compareGroupsPanel, gridBagConstraints);

    updateCompareModeStatus();
    AnEventManager.getInstance().addListener(this);

    updateAllButtonStates(false);

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
                Analyzer.getInstance().showHelp(AnVariable.HELP_Compare);
              }
            });
    initialHeight = getPreferredSize().height;
  }

  public int getDiffFromInitialHeight() {
    int diff = getPreferredSize().height - initialHeight;
    return diff;
  }

  private void updateToggleButtons(ToolbarToggleButton toolbarToggleButton) {
    if (toolbarToggleButton != absoluteButton) {
      absoluteButton.toggle(false);
    }
    if (toolbarToggleButton != deltaButton) {
      deltaButton.toggle(false);
    }
    if (toolbarToggleButton != ratioButton) {
      ratioButton.toggle(false);
    }
  }

  private void updateCompareButtonStates() {
    String groups[][] = AnWindow.getInstance().getExperimentGroups();
    boolean on = groups != null && groups.length > 1;
    if (!on) {
      absoluteButton.toggle(false);
      deltaButton.toggle(false);
      ratioButton.toggle(false);
    }
    absoluteButton.setEnabled(on);
    deltaButton.setEnabled(on);
    ratioButton.setEnabled(on);
    reverseButton.setEnabled(on);
  }

  private void updateAllButtonStates(boolean on) {
    absoluteButton.setEnabled(on);
    deltaButton.setEnabled(on);
    ratioButton.setEnabled(on);
    reverseButton.setEnabled(on);
    configureButton.setEnabled(on);
  }

  class ToolbarToggleButton extends JButton {

    private boolean on;

    public ToolbarToggleButton(ImageIcon image) {
      this(image, true);
    }

    public ToolbarToggleButton(ImageIcon image, final boolean toggle) {
      super(image);
      setBorder(BorderFactory.createLineBorder(AnEnvironment.COMPARE_TOGGLE_ICON_BORDER_COLOR, 1));
      setBackground(AnEnvironment.COMPARE_ICON_TOGGLE_BACKGROUND_COLOR);
      setContentAreaFilled(true);
      toggle(false);
      if (toggle) {
        addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (!on) {
                  on = true;
                  toggle(on);
                  updateToggleButtons(ToolbarToggleButton.this);
                }
              }
            });
      }
      addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
              if (!isEnabled()) {
                return;
              }
              setBorder(
                  BorderFactory.createLineBorder(
                      AnEnvironment.COMPARE_TOGGLE_ICON_BORDER_FOCUSED_COLOR, 1));
            }

            @Override
            public void mouseExited(MouseEvent e) {
              if (!isEnabled()) {
                return;
              }
              setBorder(
                  BorderFactory.createLineBorder(
                      AnEnvironment.COMPARE_TOGGLE_ICON_BORDER_COLOR, 1));
            }

            @Override
            public void mousePressed(MouseEvent e) {
              if (!isEnabled()) {
                return;
              }
              if (!on) {
                setBackground(AnEnvironment.COMPARE_ICON_MOUSE_PRESSED_BACKGROUND_COLOR);
                setContentAreaFilled(true);
              }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
              if (!isEnabled()) {
                return;
              }
              if (!on) {
                setBackground(AnEnvironment.COMPARE_TOGGLE_ICON_SELECTED_COLOR);
                setContentAreaFilled(true);
              }
            }
          });

      addFocusListener(
          new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
              setBorder(
                  BorderFactory.createLineBorder(
                      AnEnvironment.COMPARE_TOGGLE_ICON_BORDER_FOCUSED_COLOR, 1));
              validate();
              repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
              setBorder(
                  BorderFactory.createLineBorder(
                      AnEnvironment.COMPARE_TOGGLE_ICON_BORDER_COLOR, 1));
              validate();
              repaint();
            }
          });
    }

    public void toggle(boolean on) {
      this.on = on;
      if (on) {
        setContentAreaFilled(true);
        setBackground(AnEnvironment.COMPARE_TOGGLE_ICON_SELECTED_COLOR);
      } else {
        setContentAreaFilled(true);
        setBackground(AnEnvironment.COMPARE_ICON_TOGGLE_BACKGROUND_COLOR);
      }
    }

    /**
     * @return the on
     */
    public boolean isOn() {
      return on;
    }
  }

  /** Called from settings */
  private void updateCompareModeStatus() {
    CompareMode compareMode = AnWindow.getInstance().getSettings().getCompareModeSetting().get();
    //        System.out.println(compareMode);
    if (compareMode == CompareModeSetting.CompareMode.CMP_ENABLE) {
      absoluteButton.toggle(true);
      updateToggleButtons(absoluteButton);
    } else if (compareMode == CompareModeSetting.CompareMode.CMP_DELTA) {
      deltaButton.toggle(true);
      updateToggleButtons(deltaButton);
    } else if (compareMode == CompareModeSetting.CompareMode.CMP_RATIO) {
      ratioButton.toggle(true);
      updateToggleButtons(ratioButton);
    } else {
      // nothing
    }
  }

  private void updateCompareGroupPanel() {
    // Compare groups panel
    if (compareGroupsPanel != null) {
      remove(compareGroupsPanel);
    }
    compareGroupsPanel = new CompareGroupsPanel();
    GridBagConstraints gridBagConstraints;
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    add(compareGroupsPanel, gridBagConstraints);

    setMinimumSize(getPreferredSize());
  }

  private void updateHeight(int oldHeight) {
    int newHeight = getPreferredSize().height;
    int diff = newHeight - oldHeight;
    if (oldHeight == 0 || (diff >= -1 && diff <= 1)) {
      return;
    }
    AnWindow.getInstance().getNavigationPanel().adjustCompareNavigationPanelHeightChanged(diff);
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
      case EXPERIMENTS_LOADED_FAILED:
        updateAllButtonStates(false);
        break;
      case EXPERIMENTS_LOADED:
        int oldHeight = getPreferredSize().height;
        updateCompareGroupPanel();
        updateAllButtonStates(true);
        updateCompareButtonStates();
        updateHeight(oldHeight);
        break;
      case FILTER_CHANGED:
      case FILTER_CHANGING:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case SOURCE_FINDING_CHANGING:
      case SOURCE_FINDING_CHANGED:
      case SETTING_CHANGING:
        // Nothing
        break;
      case SETTING_CHANGED:
        AnSettingChangeEvent anSettingChangeEvent = (AnSettingChangeEvent) e.getSource();
        if (anSettingChangeEvent.getOriginalSource() != this) {
          if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.COMPARE_MODE) {
            updateCompareModeStatus();
          }
        }
        break;
      case SELECTED_OBJECT_CHANGED:
      case SELECTION_CHANGING:
      case SELECTION_CHANGED:
      case SELECTION_UPDATE:
        // Nothing
        break;
      case DEBUG:
        break;
      default:
        System.err.println("AnChanged event " + e.getType() + " not handled: " + this);
        break;
    }
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

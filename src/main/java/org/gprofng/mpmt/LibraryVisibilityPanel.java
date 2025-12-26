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

package org.gprofng.mpmt;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.picklist.StringPickList;
import org.gprofng.mpmt.picklist.StringPickListElement;
import org.gprofng.mpmt.settings.LibraryVisibilitySetting;
import org.gprofng.mpmt.util.gui.AnCheckBox;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.gui.AnUtility.AnRadioButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class LibraryVisibilityPanel extends JPanel implements AnChangeListener {
  private static final int maxEntries = 1000;
  private static final int fill1 = 80;
  private static int fill2 = 0;

  private enum SortState {
    NONE,
    UP,
    DOWN
  };

  private SortState nameSortState = SortState.DOWN;
  private SortState pathSortState = SortState.NONE;
  private LibraryVisibilityDialog dialog;
  private JLabel pleaseWaitLabel = new JLabel(AnLocale.getString("Please wait..."));
  private List<GUIEntry> entryListOriginal;
  private List<GUIEntry> entryListSortedFiltered;
  private int noEntriesViewed = 0;
  private boolean initialized = false;
  private boolean anyJava;
  private AnCheckBox functionsCheckBox;
  private AnCheckBox apiCheckBox;
  private AnCheckBox CheckBox;

  public LibraryVisibilityPanel(LibraryVisibilityDialog dialog) {
    this.dialog = dialog;
    initComponents();
    AnUtility.setTextAndAccessibleContext(
        infoLabel,
        AnLocale.getString(
            "Specify the visibility level of shared libraries and classes in data views."));
    scrollPane.getVerticalScrollBar().setUnitIncrement(10);
    scrollPane.setBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, AnEnvironment.SCROLLBAR_BORDER_COLOR));
    nativeRadioButton.setText(AnLocale.getString("Native"));
    nativeRadioButton.setToolTipText(AnLocale.getString("Select native libraries"));
    nativeRadioButton.setMnemonic(AnLocale.getString('N', "MN_LIBRARYVISIBILITY_Native_PANEL"));
    AnUtility.setAccessibleContext(
        nativeRadioButton.getAccessibleContext(), nativeRadioButton.getText());
    javaRadioButton.setText(AnLocale.getString("Java"));
    javaRadioButton.setToolTipText(AnLocale.getString("Select Java classes"));
    javaRadioButton.setMnemonic(AnLocale.getString('J', "MN_LIBRARYVISIBILITY_Java_PANEL"));
    AnUtility.setAccessibleContext(
        javaRadioButton.getAccessibleContext(), javaRadioButton.getText());
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(nativeRadioButton);
    buttonGroup.add(javaRadioButton);
    String includeTT = AnLocale.getString("Include entries that contain any of these text strings");
    includeLabel.setText(AnLocale.getString("Include:"));
    includeLabel.setToolTipText(includeTT);
    includeLabel.setDisplayedMnemonic(AnLocale.getString('I', "HideFunctionsIncludeLabelMnemonic"));
    includeLabel.setLabelFor(includeComboBox);
    includeComboBox.setToolTipText(includeTT);
    String excludeTT = AnLocale.getString("Exclude entries that contain any of these text strings");
    excludeLabel.setToolTipText(excludeTT);
    excludeLabel.setText(AnLocale.getString("Exclude:"));
    excludeLabel.setDisplayedMnemonic(AnLocale.getString('X', "HideFunctionsExcludeLabelMnemonic"));
    excludeLabel.setLabelFor(excludeComboBox);
    excludeComboBox.setToolTipText(excludeTT);
    filterPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    AnUtility.setTextAndAccessibleContext(filterLabel, " " + AnLocale.getString("Filters"));
    filterLabel.setLabelFor(includeComboBox);
    filterLabel.setOpaque(true);
    //        filterLabel.setDisplayedMnemonic(AnLocale.getString('F', "HideFilterMnemonics"));
    refreshButton.setText(AnLocale.getString("Refresh"));
    refreshButton.setMnemonic(AnLocale.getString('R', "HideFunctionsRefreshButton"));
    refreshButton.setToolTipText(AnLocale.getString("Apply filters"));
    listLabelPanel.setOpaque(false);
    AnUtility.setTextAndAccessibleContext(listLabel, AnLocale.getString("List"));
    //        listLabel.setDisplayedMnemonic(AnLocale.getString('L', "HideFunctionsListMnemonics"));

    functionsCheckBox = new AnCheckBox();
    functionsCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            functionsCheckBoxActionPerformed(evt);
          }
        });
    GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    outerListPanel.add(functionsCheckBox, gridBagConstraints);

    apiCheckBox = new AnCheckBox();
    apiCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            apiCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    outerListPanel.add(apiCheckBox, gridBagConstraints);

    CheckBox = new AnCheckBox();
    CheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            libraryCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    outerListPanel.add(CheckBox, gridBagConstraints);

    listLabel.setLabelFor(functionsCheckBox);
    functionsLabel.setText(AnLocale.getString("Functions"));
    String showTT = AnLocale.getString("Show all library and class functions");
    functionsLabel.setToolTipText(showTT);
    functionsLabel.setDisplayedMnemonic(AnLocale.getString('F', "MN_LibraryVisibility_Functions"));
    functionsLabel.setLabelFor(functionsCheckBox);
    functionsCheckBox.setToolTipText(showTT);
    libraryLabel.setText(AnLocale.getString("Library"));
    libraryLabel.setLabelFor(CheckBox);
    String hideTT =
        AnLocale.getString("Show only library or class name (Aggregate by library or class)");
    libraryLabel.setToolTipText(hideTT);
    libraryLabel.setDisplayedMnemonic(AnLocale.getString('L', "MN_LibraryVisibility_Library"));
    CheckBox.setToolTipText(hideTT);
    apiLabel.setText(AnLocale.getString("API"));
    apiLabel.setLabelFor(apiCheckBox);
    apiLabel.setDisplayedMnemonic(AnLocale.getString('A', "MN_LibraryVisibility_API"));
    String apiTT = AnLocale.getString("Show only API entry points");
    apiLabel.setToolTipText(apiTT);
    apiCheckBox.setToolTipText(apiTT);
    AnUtility.setTextAndAccessibleContext(nameButton, AnLocale.getString("Name"));
    nameButton.setMnemonic(AnLocale.getString('M', "HideFunctionsNameButton"));
    AnUtility.setTextAndAccessibleContext(pathButton, AnLocale.getString("Path"));
    pathButton.setMnemonic(AnLocale.getString('P', "HideFunctionsPathButton"));
    defaultsButton.setText(AnLocale.getString("Defaults"));
    defaultsButton.setMnemonic(AnLocale.getString('D', "HideFunctionsDefaultsButton"));
    defaultsButton.setToolTipText(AnLocale.getString("Reset selections to defaults"));
    outerListPanel.setBorder(BorderFactory.createLineBorder(AnEnvironment.BAR_BORDER_COLOR, 1));
    includeComboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    excludeComboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    Dimension dim1 = new java.awt.Dimension(fill1, 0);
    filler1.setMinimumSize(dim1);
    filler1.setPreferredSize(dim1);
    filler1.setMaximumSize(dim1);
    filler2.setMinimumSize(dim1);
    filler2.setPreferredSize(dim1);
    filler2.setMaximumSize(dim1);
    filler3.setMinimumSize(dim1);
    filler3.setPreferredSize(dim1);
    filler3.setMaximumSize(dim1);
    Dimension dim2 = new java.awt.Dimension(fill2, 0);
    filler4.setMinimumSize(dim2);
    filler4.setPreferredSize(dim2);
    filler4.setMaximumSize(dim2);
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    listPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    buttonPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);

    nameButton.setBorder(null);
    nameButton.setBorderPainted(false);
    nameButton.setBackground(Color.red);
    nameButton.setOpaque(false);

    pathButton.setBorder(null);
    pathButton.setBorderPainted(false);
    pathButton.setBackground(Color.red);
    pathButton.setOpaque(false);
    // Accept ENTER in combobox
    Action enterAction =
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String typedInclude =
                ((JTextField) includeComboBox.getEditor().getEditorComponent()).getText();
            includeComboBox.setSelectedItem(typedInclude);
            String typedExclude =
                ((JTextField) excludeComboBox.getEditor().getEditorComponent()).getText();
            excludeComboBox.setSelectedItem(typedExclude);
            applyFiltersAndUpdateGUI();
          }
        };
    ((JTextField) includeComboBox.getEditor().getEditorComponent())
        .getInputMap()
        .put(KeyStroke.getKeyStroke("ENTER"), "enter");
    ((JTextField) includeComboBox.getEditor().getEditorComponent())
        .getActionMap()
        .put("enter", enterAction);
    ((JTextField) excludeComboBox.getEditor().getEditorComponent())
        .getInputMap()
        .put(KeyStroke.getKeyStroke("ENTER"), "enter");
    ((JTextField) excludeComboBox.getEditor().getEditorComponent())
        .getActionMap()
        .put("enter", enterAction);

    setPreferredSize(new Dimension(1000, 600));

    dialog.getOKButton().setEnabled(false);
    maxLabel.setForeground(Color.red);
    AnEventManager.getInstance().addListener(this);
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
    //        System.out.println("StatisticsView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
      case EXPERIMENTS_LOADED_FAILED:
        // Nothing
        break;
      case EXPERIMENTS_LOADED:
        initialized = false;
        break;
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
      case FILTER_CHANGING:
      case FILTER_CHANGED:
      case SOURCE_FINDING_CHANGING:
      case SOURCE_FINDING_CHANGED:
      case SETTING_CHANGING:
      case SELECTED_OBJECT_CHANGED:
      case SELECTION_CHANGING:
      case SELECTION_CHANGED:
      case SELECTION_UPDATE:
      case SETTING_CHANGED:
        break;
      case DEBUG:
        debug();
        break;
      default:
        System.err.println("AnChanged event " + e.getType() + " not handled: " + this);
        break;
    }
  }

  protected void debug() {
    System.out.println("HideFunctionsPanel: " + getClass().getName());
  }

  private void updateSortState() {
    if (nameSortState == SortState.UP) {
      nameButton.setIcon(AnUtility.smallArrowDownIcon);
    } else if (nameSortState == SortState.DOWN) {
      nameButton.setIcon(AnUtility.smallArrowUpIcon);
    } else {
      nameButton.setIcon(null);
    }
    if (pathSortState == SortState.UP) {
      pathButton.setIcon(AnUtility.smallArrowUpIcon);
    } else if (pathSortState == SortState.DOWN) {
      pathButton.setIcon(AnUtility.smallArrowDownIcon);
    } else {
      pathButton.setIcon(null);
    }
  }

  class GUIEntry implements Comparable<GUIEntry>, ActionListener {

    private LibraryVisibilitySetting.Entry entry;
    public JRadioButton showRadioButton;
    public JRadioButton hideRadioButton;
    public JRadioButton apiRadioButton;

    public GUIEntry(LibraryVisibilitySetting.Entry entry) {
      this.entry = entry;

      showRadioButton = new AnRadioButton();
      AnUtility.setAccessibleContext(
          showRadioButton.getAccessibleContext(), AnLocale.getString("Functions"));
      showRadioButton.setOpaque(false);
      showRadioButton.setActionCommand("SHOW");
      showRadioButton.addActionListener(this);

      hideRadioButton = new AnRadioButton();
      AnUtility.setAccessibleContext(
          hideRadioButton.getAccessibleContext(), AnLocale.getString("API"));
      hideRadioButton.setActionCommand("HIDE");
      hideRadioButton.addActionListener(this);
      hideRadioButton.setOpaque(false);

      apiRadioButton = new AnRadioButton();
      AnUtility.setAccessibleContext(
          apiRadioButton.getAccessibleContext(), AnLocale.getString("Library"));
      apiRadioButton.setActionCommand("API");
      apiRadioButton.addActionListener(this);
      apiRadioButton.setOpaque(false);

      ButtonGroup buttonGroup = new ButtonGroup();
      buttonGroup.add(hideRadioButton);
      buttonGroup.add(showRadioButton);
      buttonGroup.add(apiRadioButton);

      if (entry.getState() == 0) {
        showRadioButton.setSelected(true);
      } else if (entry.getState() == 1) {
        hideRadioButton.setSelected(true);
      } else if (entry.getState() == 2) {
        apiRadioButton.setSelected(true);
      }
    }

    /**
     * @return the name
     */
    public String getName() {
      return entry.getName();
    }

    /**
     * @return the path
     */
    public String getPath() {
      return entry.getPath();
    }

    /**
     * @return the state
     */
    public int getState() {
      return entry.getState();
    }

    /**
     * @return the index
     */
    public int getIndex() {
      return entry.getIndex();
    }

    /**
     * @return the name
     */
    public boolean isJava() {
      return entry.isJava();
    }

    @Override
    public int compareTo(GUIEntry o) {
      if (nameSortState == SortState.UP) {
        return getName().compareTo(o.getName());
      } else if (nameSortState == SortState.DOWN) {
        return o.getName().compareTo(getName());
      } else if (pathSortState == SortState.UP) {
        return getPath().compareTo(o.getPath());
      } else if (pathSortState == SortState.DOWN) {
        return o.getPath().compareTo(getPath());
      } else {
        // Sort by index
        if (getIndex() < o.getIndex()) {
          return -1;
        } else if (getIndex() > o.getIndex()) {
          return 1;
        } else {
          return 0;
        }
      }
    }

    /**
     * @param state the state to set
     */
    public void setState(int state) {
      entry.setState(state);
      if (state == 0) {
        showRadioButton.setSelected(true);
      } else if (state == 1) {
        hideRadioButton.setSelected(true);
      } else if (state == 2) {
        apiRadioButton.setSelected(true);
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String ac = e.getActionCommand();
      if (ac.equals("SHOW")) {
        setState(0);
      } else if (ac.equals("HIDE")) {
        setState(1);
      } else if (ac.equals("API")) {
        setState(2);
      }
      updateCheckBoxStates();
    }
  }

  private void updateCheckBoxStates() {
    functionsCheckBox.setSelected(false);
    CheckBox.setSelected(false);
    apiCheckBox.setSelected(false);
    if (entryListSortedFiltered.size() > 0) {
      boolean allTheSame = true;
      int state = entryListSortedFiltered.get(0).getState();
      for (GUIEntry entry : entryListSortedFiltered) {
        if (entry.getState() != state) {
          allTheSame = false;
          break;
        }
      }
      if (allTheSame) {
        if (state == 0) {
          functionsCheckBox.setSelected(true);
        } else if (state == 1) {
          CheckBox.setSelected(true);
        } else if (state == 2) {
          apiCheckBox.setSelected(true);
        }
      }
    }
  }

  public void reset() {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            resetInternal();
          }
        });
  }

  private void resetInternal() {
    if (!initialized) {
      listPanel.removeAll();
      listPanel.add(pleaseWaitLabel);
      includeComboBox.setSelectedItem(UserPref.getInstance().getLibraryVisibilityIncludeFilter());
      initilize();
    } else {
      includeComboBox.setSelectedItem(UserPref.getInstance().getLibraryVisibilityIncludeFilter());
      List<LibraryVisibilitySetting.Entry> list =
          AnWindow.getInstance().getSettings().getLibraryVisibilitySetting().get();
      for (int i = 0; i < list.size(); i++) {
        LibraryVisibilitySetting.Entry entry = list.get(i);
        GUIEntry guiEntry = entryListOriginal.get(i);
        guiEntry.setState(entry.getState());
        if (guiEntry.getState() == 0) {
          guiEntry.showRadioButton.setSelected(true);
        } else if (guiEntry.getState() == 1) {
          guiEntry.hideRadioButton.setSelected(true);
        } else if (guiEntry.getState() == 2) {
          guiEntry.apiRadioButton.setSelected(true);
        }
      }
      updateCheckBoxStates();
    }
  }

  public void save() {
    List<LibraryVisibilitySetting.Entry> list = new ArrayList<>();
    for (GUIEntry guiEntry : entryListOriginal) {
      list.add(guiEntry.entry.copy());
    }
    AnWindow.getInstance().getSettings().getLibraryVisibilitySetting().set(this, list);
  }

  // For persistance
  public void initStates(
      boolean java,
      StringPickList hideFuncsIncludePickList,
      String currentIncludeFilter,
      StringPickList hideFuncsExcludePickList,
      String currentExcludeFilter) {
    AnUtility.checkIfOnAWTThread(true);
    initialized = false;
    if (java) {
      javaRadioButton.setSelected(true);
    } else {
      nativeRadioButton.setSelected(true);
    }
    updateFilterComboBoxes(
        hideFuncsIncludePickList,
        currentIncludeFilter,
        hideFuncsExcludePickList,
        currentExcludeFilter);
  }

  private void enableControls(boolean val) {
    dialog.getOKButton().setEnabled(val);
    includeComboBox.setEnabled(val);
    excludeComboBox.setEnabled(val);
    refreshButton.setEnabled(val);
    defaultsButton.setEnabled(val);
    nativeRadioButton.setEnabled(val);
    javaRadioButton.setEnabled(val && anyJava);
  }

  private void initilize() {
    AnUtility.checkIfOnAWTThread(true);
    initialized = true;
    enableControls(false);
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  @Override
                  public void run() {
                    anyJava = false;
                    entryListOriginal = new ArrayList<>();
                    entryListSortedFiltered = new ArrayList<>();
                    List<LibraryVisibilitySetting.Entry> list =
                        AnWindow.getInstance().getSettings().getLibraryVisibilitySetting().get();
                    for (LibraryVisibilitySetting.Entry entry : list) {
                      GUIEntry guiEntry = new GUIEntry(entry.copy());
                      entryListOriginal.add(guiEntry);
                      entryListSortedFiltered.add(guiEntry);
                      if (entry.isJava()) {
                        anyJava = true;
                      }
                    }
                    if (!anyJava) {
                      nativeRadioButton.setSelected(true);
                    }
                    javaRadioButton.setEnabled(anyJava);
                    applyFiltersAndUpdateGUI();
                    enableControls(true);
                  }
                });
          }
        },
        "LibraryVisibility");
  }

  private void updateList() {
    AnUtility.checkIfOnAWTThread(true);
    enableControls(false);
    dialog.busyCursor(true);
    updateSortState();
    Collections.sort(entryListSortedFiltered);
    listPanel.removeAll();
    noEntriesViewed = 0;
    maxLabel.setText("");
    maxListLabel.setText("");
    for (GUIEntry entry : entryListSortedFiltered) {
      addEntry(noEntriesViewed++, entry);
      if (noEntriesViewed >= maxEntries) {
        addMaxEntry(noEntriesViewed);
        break;
      }
    }
    addLastEntry(noEntriesViewed);

    listPanel.validate();
    listPanel.repaint();
    dialog.busyCursor(false);
    enableControls(true);

    //        if (noEntriesViewed != entryListOriginal.size()) {
    String labelText =
        String.format(
            AnLocale.getString("List (%d/%d)"), noEntriesViewed, entryListOriginal.size());
    listLabel.setText(labelText);
    //        } else {
    //            listLabel.setText(AnLocale.getString("List:"));
    //        }

    AnUtility.setTextAndAccessibleContext(
        listedLabel,
        AnLocale.getString("Number of entries viewed: ")
            + noEntriesViewed
            + "/"
            + entryListOriginal.size());
  }

  private void addEntry(int no, GUIEntry entry) {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();

    if (entry.getState() == 0) {
      entry.showRadioButton.setSelected(true);
    } else if (entry.getState() == 1) {
      entry.hideRadioButton.setSelected(true);
    } else if (entry.getState() == 2) {
      entry.apiRadioButton.setSelected(true);
    }

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = no;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, fill1 / 2 - 10, 0, 0);
    listPanel.add(entry.showRadioButton, gridBagConstraints);

    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = no;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, fill1 / 2 + 19, 0, 0);
    listPanel.add(entry.apiRadioButton, gridBagConstraints);

    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = no;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, fill1 / 2 + 19, 0, 0);
    listPanel.add(entry.hideRadioButton, gridBagConstraints);

    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = no;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 45, 0, 0);
    JLabel label = new JLabel(entry.getName());
    label.setToolTipText(entry.getName());
    listPanel.add(label, gridBagConstraints);

    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = no;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 12, 0, 0);
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    label = new JLabel(entry.getPath());
    label.setToolTipText(entry.getPath());
    listPanel.add(label, gridBagConstraints);
  }

  private void addMaxEntry(int no) {
    String text =
        String.format(
            AnLocale.getString(
                "Maximum number (%d) of viewed entries reached. Use the filters to reduce the"
                    + " number of entries"),
            maxEntries);

    // Add entry in list
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    JLabel maxEntryLabel = new JLabel();
    AnUtility.setTextAndAccessibleContext(maxEntryLabel, text);
    maxEntryLabel.setForeground(Color.red);

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = no + 1;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(8, 12, 8, 0);
    listPanel.add(maxEntryLabel, gridBagConstraints);

    // Set the max list label
    AnUtility.setTextAndAccessibleContext(maxListLabel, " - " + text);
    maxListLabel.setForeground(Color.red);
  }

  private void addLastEntry(int no) {
    JLabel label = new JLabel();
    GridBagConstraints gridBagConstraints = new GridBagConstraints();

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = no;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    listPanel.add(label, gridBagConstraints);
  }

  private void updateFilterComboBoxes(String includeFilterString, String excludeFilterString) {
    String includeSelected = (String) includeComboBox.getSelectedItem();
    UserPref.getInstance().getLibraryVisibilityIncludePickList().addElement(includeFilterString);
    String excludeSelected = (String) excludeComboBox.getSelectedItem();
    UserPref.getInstance().getLibraryVisibilityExcludePickList().addElement(excludeFilterString);
    updateFilterComboBoxes(
        UserPref.getInstance().getLibraryVisibilityIncludePickList(),
        includeSelected,
        UserPref.getInstance().getLibraryVisibilityExcludePickList(),
        excludeSelected);
  }

  private void updateFilterComboBoxes(
      StringPickList includePicklist,
      String currentIncludeFilter,
      StringPickList excludePicklist,
      String currentExcludeFilter) {
    includeComboBox.removeAllItems();
    includeComboBox.addItem("");
    for (StringPickListElement elem : includePicklist.getStringElements()) {
      if (elem.getString().length() > 0) {
        includeComboBox.addItem(elem.getString());
      }
    }
    if (currentIncludeFilter != null) {
      includeComboBox.setSelectedItem(currentIncludeFilter);
    }

    excludeComboBox.removeAllItems();
    excludeComboBox.addItem("");
    for (StringPickListElement elem : excludePicklist.getStringElements()) {
      if (elem.getString().length() > 0) {
        excludeComboBox.addItem(elem.getString());
      }
    }
    if (currentExcludeFilter != null) {
      excludeComboBox.setSelectedItem(currentExcludeFilter);
    }
  }

  private void formatColumns() {
    int maxNameWidth = 0;
    for (GUIEntry entry : entryListSortedFiltered) {
      JLabel label = new JLabel(entry.getName());
      int w = label.getPreferredSize().width;
      if (maxNameWidth < w) {
        maxNameWidth = w;
      }
    }
    fill2 = maxNameWidth + 28; //
    Dimension dim2 = new java.awt.Dimension(fill2, 0);
    filler4.setMinimumSize(dim2);
    filler4.setPreferredSize(dim2);
    filler4.setMaximumSize(dim2);
  }

  private void applyFiltersAndUpdateGUI() {
    String includeFilterString = (String) includeComboBox.getSelectedItem();
    String excludeFilterString = (String) excludeComboBox.getSelectedItem();

    if (includeFilterString == null) {
      includeFilterString = "";
    }
    if (excludeFilterString == null) {
      excludeFilterString = "";
    }
    entryListSortedFiltered = filterEntries(entryListOriginal, javaRadioButton.isSelected());
    entryListSortedFiltered = filterEntries(entryListSortedFiltered, includeFilterString, false);
    entryListSortedFiltered = filterEntries(entryListSortedFiltered, excludeFilterString, true);
    includeComboBox.addItem(includeFilterString);
    excludeComboBox.addItem(excludeFilterString);
    formatColumns();
    updateList();
    updateCheckBoxStates();
    updateFilterComboBoxes(includeFilterString, excludeFilterString);
    UserPref.getInstance().setLibraryVisibilityIncludeFilter(includeFilterString);
    UserPref.getInstance().setCurrentLibraryVisibilityExcludeFilter(excludeFilterString);
  }

  public boolean isJava() {
    return javaRadioButton.isSelected();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    infoLabel = new javax.swing.JLabel();
    filterPanel = new javax.swing.JPanel();
    filterLabel = new javax.swing.JLabel();
    radioButtonPanel = new javax.swing.JPanel();
    nativeRadioButton = new javax.swing.JRadioButton();
    javaRadioButton = new javax.swing.JRadioButton();
    includeLabel = new javax.swing.JLabel();
    includeComboBox = new javax.swing.JComboBox();
    excludeLabel = new javax.swing.JLabel();
    excludeComboBox = new javax.swing.JComboBox();
    refreshButton = new javax.swing.JButton();
    listLabelPanel = new javax.swing.JPanel();
    listLabel = new javax.swing.JLabel();
    maxListLabel = new javax.swing.JLabel();
    outerListPanel = new javax.swing.JPanel();
    filler1 =
        new javax.swing.Box.Filler(
            new java.awt.Dimension(60, 0),
            new java.awt.Dimension(60, 0),
            new java.awt.Dimension(60, 0));
    filler2 =
        new javax.swing.Box.Filler(
            new java.awt.Dimension(60, 0),
            new java.awt.Dimension(60, 0),
            new java.awt.Dimension(60, 0));
    filler3 =
        new javax.swing.Box.Filler(
            new java.awt.Dimension(60, 0),
            new java.awt.Dimension(60, 0),
            new java.awt.Dimension(60, 0));
    filler4 =
        new javax.swing.Box.Filler(
            new java.awt.Dimension(120, 0),
            new java.awt.Dimension(120, 0),
            new java.awt.Dimension(120, 0));
    functionsLabel = new javax.swing.JLabel();
    apiLabel = new javax.swing.JLabel();
    libraryLabel = new javax.swing.JLabel();
    nameButton = new javax.swing.JButton();
    pathButton = new javax.swing.JButton();
    scrollPane = new javax.swing.JScrollPane();
    listPanel = new javax.swing.JPanel();
    buttonPanel = new javax.swing.JPanel();
    maxLabel = new javax.swing.JLabel();
    defaultsButton = new javax.swing.JButton();
    listedLabel = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    infoLabel.setText("info...");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    add(infoLabel, gridBagConstraints);

    filterPanel.setLayout(new java.awt.GridBagLayout());

    filterLabel.setText("Filter:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.ipady = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    filterPanel.add(filterLabel, gridBagConstraints);

    radioButtonPanel.setOpaque(false);
    radioButtonPanel.setLayout(new java.awt.GridBagLayout());

    nativeRadioButton.setText("Native");
    nativeRadioButton.setOpaque(false);
    nativeRadioButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            nativeRadioButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    radioButtonPanel.add(nativeRadioButton, gridBagConstraints);

    javaRadioButton.setText("Java");
    javaRadioButton.setOpaque(false);
    javaRadioButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            javaRadioButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    radioButtonPanel.add(javaRadioButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    filterPanel.add(radioButtonPanel, gridBagConstraints);

    includeLabel.setText("Include");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 0);
    filterPanel.add(includeLabel, gridBagConstraints);

    includeComboBox.setEditable(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 0);
    filterPanel.add(includeComboBox, gridBagConstraints);

    excludeLabel.setText("and exclude");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
    filterPanel.add(excludeLabel, gridBagConstraints);

    excludeComboBox.setEditable(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 0);
    filterPanel.add(excludeComboBox, gridBagConstraints);

    refreshButton.setText("Refresh");
    refreshButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            refreshButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
    filterPanel.add(refreshButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(14, 0, 0, 0);
    add(filterPanel, gridBagConstraints);

    listLabelPanel.setLayout(new java.awt.GridBagLayout());

    listLabel.setText("List:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    listLabelPanel.add(listLabel, gridBagConstraints);

    maxListLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    listLabelPanel.add(maxListLabel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    add(listLabelPanel, gridBagConstraints);

    outerListPanel.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    outerListPanel.add(filler1, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    outerListPanel.add(filler2, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    outerListPanel.add(filler3, gridBagConstraints);

    filler4.setAutoscrolls(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    outerListPanel.add(filler4, gridBagConstraints);

    functionsLabel.setText("Functions");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    outerListPanel.add(functionsLabel, gridBagConstraints);

    apiLabel.setText("API");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    outerListPanel.add(apiLabel, gridBagConstraints);

    libraryLabel.setText("Library");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    outerListPanel.add(libraryLabel, gridBagConstraints);

    nameButton.setText("Name");
    nameButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            nameButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 14, 0, 0);
    outerListPanel.add(nameButton, gridBagConstraints);

    pathButton.setText("Path");
    pathButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            pathButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    outerListPanel.add(pathButton, gridBagConstraints);

    scrollPane.setBorder(null);

    listPanel.setLayout(new java.awt.GridBagLayout());
    scrollPane.setViewportView(listPanel);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    outerListPanel.add(scrollPane, gridBagConstraints);

    buttonPanel.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(4, 12, 0, 12);
    buttonPanel.add(maxLabel, gridBagConstraints);

    defaultsButton.setText("Defaults");
    defaultsButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            defaultsButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 12, 4, 0);
    buttonPanel.add(defaultsButton, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
    buttonPanel.add(listedLabel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    outerListPanel.add(buttonPanel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(outerListPanel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void nameButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_nameButtonActionPerformed
    // TODO add your handling code here:
    if (nameSortState == SortState.NONE) {
      nameSortState = SortState.UP;
    } else if (nameSortState == SortState.UP) {
      nameSortState = SortState.DOWN;
    } else {
      nameSortState = SortState.UP;
    }
    pathSortState = SortState.NONE;
    updateList();
  } // GEN-LAST:event_nameButtonActionPerformed

  private void pathButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_pathButtonActionPerformed
    // TODO add your handling code here:
    if (pathSortState == SortState.NONE) {
      pathSortState = SortState.UP;
    } else if (pathSortState == SortState.UP) {
      pathSortState = SortState.DOWN;
    } else {
      pathSortState = SortState.UP;
    }
    nameSortState = SortState.NONE;
    updateList();
  } // GEN-LAST:event_pathButtonActionPerformed

  private void refreshButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_refreshButtonActionPerformed
    applyFiltersAndUpdateGUI();
  } // GEN-LAST:event_refreshButtonActionPerformed

  private List<GUIEntry> filterEntries(List<GUIEntry> origList, boolean java) {
    List<GUIEntry> list = new ArrayList<>();
    for (GUIEntry guiEntry : origList) {
      if (guiEntry.isJava() == java) {
        list.add(guiEntry);
      }
    }
    return list;
  }

  private List<GUIEntry> filterEntries(
      List<GUIEntry> origList, String rawIncludeFilterString, boolean exclude) {
    String[] substrings =
        rawIncludeFilterString.split(" "); // multiword match  //FIXUP bypass escaped spaces?
    //        String[] substrings = {raw_includeFilterString}; // exact text match
    List<String> clauses = new ArrayList<>();
    for (int ii = 0; ii < substrings.length; ii++) {
      if (substrings[ii].isEmpty()) {
        continue; // strip out empty clauses caused by consecutive spaces
      }
      clauses.add(substrings[ii]);
    }
    final List<GUIEntry> newList;
    if (clauses.isEmpty()) {
      newList = new ArrayList<>(origList);
    } else {
      newList = new ArrayList<>();
      for (GUIEntry entry : origList) {
        boolean pass = false;
        for (String clause : clauses) {
          if (entry.getName().contains(clause) || entry.getPath().contains(clause)) {
            pass = true;
            break;
          }
        }
        if (exclude) {
          if (!pass) {
            newList.add(entry);
          }
        } else {
          if (pass) {
            newList.add(entry);
          }
        }
      }
    }
    return newList;
  }

  private void functionsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
    for (GUIEntry entry : entryListSortedFiltered) {
      entry.setState(0);
    }
    updateCheckBoxStates();
  }

  private void libraryCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
    for (GUIEntry entry : entryListSortedFiltered) {
      entry.setState(1);
    }
    updateCheckBoxStates();
  }

  private void apiCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
    for (GUIEntry entry : entryListSortedFiltered) {
      entry.setState(2);
    }
    updateCheckBoxStates();
  }

  private void defaultsButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_defaultsButtonActionPerformed
    for (GUIEntry entry : entryListSortedFiltered) {
      entry.setState(0);
    }
    updateCheckBoxStates();
  } // GEN-LAST:event_defaultsButtonActionPerformed

  private void nativeRadioButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_nativeRadioButtonActionPerformed
    applyFiltersAndUpdateGUI();
  } // GEN-LAST:event_nativeRadioButtonActionPerformed

  private void javaRadioButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_javaRadioButtonActionPerformed
    applyFiltersAndUpdateGUI();
  } // GEN-LAST:event_javaRadioButtonActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel apiLabel;
  private javax.swing.JPanel buttonPanel;
  private javax.swing.JButton defaultsButton;
  private javax.swing.JComboBox excludeComboBox;
  private javax.swing.JLabel excludeLabel;
  private javax.swing.Box.Filler filler1;
  private javax.swing.Box.Filler filler2;
  private javax.swing.Box.Filler filler3;
  private javax.swing.Box.Filler filler4;
  private javax.swing.JLabel filterLabel;
  private javax.swing.JPanel filterPanel;
  private javax.swing.JLabel functionsLabel;
  private javax.swing.JComboBox includeComboBox;
  private javax.swing.JLabel includeLabel;
  private javax.swing.JLabel infoLabel;
  private javax.swing.JRadioButton javaRadioButton;
  private javax.swing.JLabel libraryLabel;
  private javax.swing.JLabel listLabel;
  private javax.swing.JPanel listLabelPanel;
  private javax.swing.JPanel listPanel;
  private javax.swing.JLabel listedLabel;
  private javax.swing.JLabel maxLabel;
  private javax.swing.JLabel maxListLabel;
  private javax.swing.JButton nameButton;
  private javax.swing.JRadioButton nativeRadioButton;
  private javax.swing.JPanel outerListPanel;
  private javax.swing.JButton pathButton;
  private javax.swing.JPanel radioButtonPanel;
  private javax.swing.JButton refreshButton;
  private javax.swing.JScrollPane scrollPane;
  // End of variables declaration//GEN-END:variables
}

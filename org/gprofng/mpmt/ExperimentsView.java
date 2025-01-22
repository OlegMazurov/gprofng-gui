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
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.util.gui.AnJPanel;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnSplitPane;
import org.gprofng.mpmt.util.gui.AnTabbedPane;
import org.gprofng.mpmt.util.gui.AnTextIcon;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public final class ExperimentsView extends AnDisplay implements ExportSupport, AnChangeListener {

  private boolean initialized = false;
  private AnTree tree;
  private AnUtility.AnTextArea log;
  private AnTextIcon[] exp_list;
  private AnMenuListener menuListener;

  // Menu actions
  //    final String STR_ACTION_SETFILTER_SEO = AnLocale.getString("Add Filter: Selected Experiments
  // Only");  11
  //    final String STR_ACTION_SETFILTER_ESE = AnLocale.getString("Add Filter: Exclude Selected
  // Experiments");  12
  //    final String STR_ACTION_EXPERIMENTS = AnLocale.getString("Filter Experiments...");
  //    final String STR_ACTION_SETFILTER_RDF = AnLocale.getString("Remove All Filters");
  //    private final String STR_ACTION_UNDOFILTER = AnLocale.getString("Undo Last Filter");
  //    private final String STR_ACTION_MANAGEFILTERS = AnLocale.getString("Manage Filters...");
  // Constructor
  public ExperimentsView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_Experiments, AnVariable.HELP_TabsExperiments);
    setAccessibility(AnLocale.getString("Experiments"));
    AnEventManager.getInstance().addListener(this);
  }

  public void initErrorsAndWarnings(String[] errors, String[] warnings) {
    cleanLog(); // Clean old messages
    if (errors != null) {
      appendLog("" + AnLocale.getString("Errors:") + "\n");
      for (String msg : errors) {
        if (msg != null) {
          appendLog(msg + "\n");
        }
      }
      appendLog("\n");
    }
    if (warnings != null) {
      appendLog("" + AnLocale.getString("Warnings:") + "\n");
      for (String msg : warnings) {
        if (msg != null) {
          appendLog(msg + "\n");
        }
      }
      appendLog("\n");
    }
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
    //        System.out.println("ExperimentsView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
      case EXPERIMENTS_LOADED_FAILED:
        // Nothing
        break;
      case EXPERIMENTS_LOADED:
        computed = false;
        if (selected) {
          computeOnAWorkerThread();
        }
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
        // Nothing
        break;
      case SETTING_CHANGED:
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

  @Override
  public void requestFocus() {
    initGUIComponents();
    if (tree != null) {
      tree.requestFocus();
    }
  }

  /**
   * Override and return true if text find is supported
   *
   * @return
   */
  @Override
  protected boolean supportsFindText() {
    return true;
  }

  // Find
  @Override
  public int find(final String str, final boolean next, final boolean caseSensitive) {
    int selectedIndex = findSelectedIndex();
    Object root = tree.getModel().getRoot();
    int childCount = tree.getModel().getChildCount(root);

    DefaultMutableTreeNode foundNode = null;
    if (next) {
      if (selectedIndex >= 0 && selectedIndex < childCount - 1) {
        foundNode = findNode(selectedIndex + 1, childCount - 1, str, caseSensitive);
        if (foundNode == null) {
          foundNode = findNode(0, selectedIndex, str, caseSensitive);
        }
      } else {
        foundNode = findNode(0, childCount - 1, str, caseSensitive);
      }
    } else if (selectedIndex > 0) {
      foundNode = findNode(selectedIndex - 1, 0, str, caseSensitive);
      if (foundNode == null) {
        foundNode = findNode(childCount - 1, selectedIndex, str, caseSensitive);
      }
    } else {
      foundNode = findNode(childCount - 1, 0, str, caseSensitive);
    }

    if (foundNode != null) {
      TreePath treePath = new TreePath(foundNode.getPath());
      tree.scrollPathToVisible(treePath);
      tree.setSelectionPath(treePath);
      //            tree.expandPath(treePath);
      return 0;
    } else {
      return -1;
    }
  }

  private DefaultMutableTreeNode findNode(int from, int to, String str, boolean caseSensitive) {
    //        System.out.println(from + " " + to);
    Object root = tree.getModel().getRoot();
    DefaultMutableTreeNode foundNode = null;
    if (from <= to) {
      for (int i = from; i <= to; i++) {
        Object child = tree.getModel().getChild(root, i);
        foundNode = match(child, str, caseSensitive);
        if (foundNode != null) {
          break;
        }
      }
    } else {
      for (int i = from; i >= to; i--) {
        Object child = tree.getModel().getChild(root, i);
        foundNode = match(child, str, caseSensitive);
        if (foundNode != null) {
          break;
        }
      }
    }
    return foundNode;
  }

  private DefaultMutableTreeNode match(Object child, String str, boolean caseSensitive) {
    if (child instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) child;
      Object userObject = node.getUserObject();
      if (userObject instanceof AnTextIcon) {
        AnTextIcon anTextIcon = (AnTextIcon) userObject;
        if (!caseSensitive && anTextIcon.getText().toLowerCase().contains(str.toLowerCase())) {
          return node;
        } else if (anTextIcon.getText().contains(str)) {
          return node;
        }
      }
    }
    return null;
  }

  private int findSelectedIndex() {
    DefaultMutableTreeNode selectedNode =
        (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

    Object root = tree.getModel().getRoot();
    int childCount = tree.getModel().getChildCount(root);
    for (int i = 0; i < childCount; i++) {
      Object child = tree.getModel().getChild(root, i);
      if (child instanceof DefaultMutableTreeNode) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) child;
        if (node == selectedNode) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  protected void initComponents() {}

  //    @Override
  private void initGUIComponents() {
    if (!initialized) {
      initGUIComponentsInternal();
      initialized = true;
    }
  }

  /** Initialize GUI components */
  //    @Override
  private void initGUIComponentsInternal() {
    JLabel treeLabel;
    JPanel logPanel;

    setLayout(new BorderLayout());

    // Experiment tree area
    String text = AnLocale.getString("Experiments");
    tree = new AnTree(text, 2);
    tree.getAccessibleContext().setAccessibleName(text);
    tree.getAccessibleContext().setAccessibleDescription(text);
    treeLabel = new JLabel(text);
    treeLabel.setLabelFor(tree);
    treeLabel.getAccessibleContext().setAccessibleName(text);
    treeLabel.getAccessibleContext().setAccessibleDescription(text);
    treeLabel.setVisible(false);
    JScrollPane treeScrollPane = new AnJScrollPane(tree);
    treeScrollPane.setBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    treeScrollPane.add(treeLabel);

    // Errors and warnings list area
    JPanel listPanel = new JPanel();
    listPanel.setBackground(Color.WHITE);
    JScrollPane listScrollPane = new AnJScrollPane(listPanel);
    //        listScrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
    // AnEnvironment.SPLIT_PANE_BORDER_COLOR));

    // Errors and warnings log area
    log = new AnUtility.AnTextArea(null, false);
    log.setBackground(AnEnvironment.DEFAULT_DIALOG_BACKGROUND);
    AnUtility.setAccessibleContext(log.getAccessibleContext(), AnLocale.getString("Logs"));
    logPanel = new JPanel(new BorderLayout());
    logPanel.add(log, BorderLayout.CENTER);
    JScrollPane logScrollPane = new AnJScrollPane(logPanel);
    //        logScrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
    // AnEnvironment.SPLIT_PANE_BORDER_COLOR));

    // Errors and warnings tabs
    JTabbedPane tabbedPane = new AnTabbedPane();
    tabbedPane.setBorder(
        BorderFactory.createMatteBorder(
            1, 0, 0, 0, new Color(219, 219, 219))); // AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    tabbedPane.add(AnLocale.getString("List"), listScrollPane);
    tabbedPane.add(AnLocale.getString("Log"), logScrollPane);
    tabbedPane.setSelectedIndex(1); // FIXUP: justfornow
    JPanel errorsAndWarningsPanel = new JPanel(new GridBagLayout());
    errorsAndWarningsPanel.setBackground(Color.WHITE);
    errorsAndWarningsPanel.setBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    GridBagConstraints gridBagConstraints;
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 0, 2, 0);
    JLabel eawLabel = new JLabel(AnLocale.getString("Errors & Warnings:"));
    eawLabel.getAccessibleContext().setAccessibleDescription("");
    eawLabel.setOpaque(false);
    errorsAndWarningsPanel.add(eawLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    errorsAndWarningsPanel.add(tabbedPane, gridBagConstraints);
    //
    JSplitPane splitPane =
        new AnSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, errorsAndWarningsPanel);
    splitPane.setOneTouchExpandable(true);
    splitPane.setResizeWeight(1.0);
    add(splitPane, BorderLayout.CENTER);

    // Popup menu
    menuListener = new AnMenuListener(tree);
    tree.addMouseListener(menuListener);
    // this.addMouseListener(menuListener); // Do we need i?

    //        KeyStroke ks = KeyboardShortcuts.contextMenuActionShortcut;
    //        tree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, ks);
    //        tree.getActionMap().put(ks, new AbstractAction() {
    //            public void actionPerformed(ActionEvent ev) {
    //                JPopupMenu popup = menuListener.init_popup();
    //                if (popup != null) {
    //                    JTree src = (JTree) ev.getSource();
    //                    Rectangle cellRect, visRect;
    //
    //                    visRect = src.getVisibleRect();
    //                    cellRect = visRect;
    //                    int selrow = src.getMaxSelectionRow();
    //                    if (selrow >= 0) {
    //                        cellRect = src.getRowBounds(selrow);
    //                    }
    //
    //                    // if current view doesn't include selected row, scroll
    //                    if (!visRect.contains(cellRect)) {
    //                        // calculate middle based on selected row
    //                        // being below or above current visible rows
    //                        if (visRect.y < cellRect.y) {
    //                            cellRect.y += visRect.height / 2;
    //                        } else {
    //                            cellRect.y -= visRect.height / 2;
    //                        }
    //                        src.scrollRectToVisible(cellRect);
    //                    }
    //                    popup.show(src, cellRect.x, cellRect.y + cellRect.height);
    //                }
    //            }
    //        });
  }

  /** Append added experiments */
  private void addExperiment() {
    exp_list = window.getExperimentList();

    if (exp_list == null) {
      return;
    }

    // Add the extra node "Load Objects" if needed & Get the last index
    int last = tree.addExtra(AnLocale.getString("Load Objects"));

    // Add experiments into tree
    tree.addNodes(exp_list, last);
  }

  private void cleanLog() {
    initGUIComponents();
    log.setText("");
  }

  public void appendLog(final String text) {
    initGUIComponents();
    log.append(AnUtility.trimNewLine(text) + "\n");

    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            final Dimension logSize;
            final Rectangle endRect;

            logSize = log.getPreferredSize();
            endRect = new Rectangle(0, logSize.height, 1, 1);
            log.scrollRectToVisible(endRect);
          }
        });
  }

  /** Compute & update Experiment display */
  @Override
  public void doCompute() {
    //        System.out.println("ExperimentsView:doCompute: " + selected + " " + computed);
    AnUtility.checkIfOnAWTThread(false);
    initGUIComponents();

    if (!computed) {
      addExperiment();
      String[] info = window.getExpInfo();

      if (info != null) {
        int nc = (info.length - 1) / 2 + 1;
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        panel.add(new AnUtility.AnTextArea(AnUtility.trimNewLine(info[0]), false)); // Load Objects
        panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        tree.setContent(0, panel);
        int k = 1;
        for (int i = 1; i < nc; i++) {
          panel = new AnTreePanel(info[k++], info[k++], i - 1);
          panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
          tree.setContent(i, panel);
        }
      }
      tree.setSelectionRow(0);
      computed = true;
    }
  }

  class AnTreePanel extends AnJPanel
      implements UndoableEditListener, FocusListener, KeyListener, ActionListener {

    private static final int SAVE_FILE = 0;
    private static final int DELETE_FILE = 1;
    public static final int UNDO_LIMIT = 1500; // Setting Undo Limit to 1500 edits
    private int cnt_edit_actions, cnt_undo_actions;
    private final int exp_id;
    private boolean undo_manager_exists = false;
    public String notesText;
    public final String info_text;
    public AnUtility.AnTextArea notes_text_area;
    private AnUndoManager undoManager;
    public AnUtility.ResponseAction saveAction, undoAction, redoAction, deleteAction;

    public AnTreePanel(final String notes_text, final String info_text, final int exp_id) {
      super();
      this.exp_id = exp_id;
      cnt_edit_actions = cnt_undo_actions = 0;
      this.notesText = notes_text;
      this.info_text = info_text;
      initComponents();
      notes_text_area.getDocument().addUndoableEditListener(this);
      notes_text_area.addFocusListener(this);
      notes_text_area.addKeyListener(this);
      //            final String[] initMessages;
      //            initMessages = getInitMessages();
      //            for (int i = 0; i < initMessages.length; i++) {
      //                appendLog(initMessages[i]);
      //            }

    }

    private void initComponents() {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      final JPanel notesPanel = new JPanel();
      notesPanel.setLayout(new javax.swing.BoxLayout(notesPanel, BoxLayout.X_AXIS));
      notesPanel.setBorder(new javax.swing.border.EtchedBorder());

      final JPanel buttPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 2, 2));
      saveAction =
          new AnUtility.ResponseAction(
              AnUtility.save_icon, AnLocale.getString("Save notes") + "  Ctrl+S", "__save", this);
      saveAction.setEnabled(false);
      undoAction =
          new AnUtility.ResponseAction(
              AnUtility.undo_icon, AnLocale.getString("Undo") + "  Ctrl+Z", "__undo", this);
      undoAction.setEnabled(false);
      redoAction =
          new AnUtility.ResponseAction(
              AnUtility.redo_icon, AnLocale.getString("Redo") + "  Ctrl+Y", "__redo", this);
      redoAction.setEnabled(false);
      deleteAction =
          new AnUtility.ResponseAction(
              AnUtility.del_icon,
              AnLocale.getString("Delete notes") + "  Ctrl+D",
              "__delete",
              this);
      deleteAction.setEnabled(notesText != null && !notesText.equals(""));

      buttPanel.add(saveAction);
      buttPanel.add(undoAction);
      buttPanel.add(redoAction);
      buttPanel.add(deleteAction);

      AnUtility.AnLabel notesLabel =
          (AnUtility.AnLabel) AnUtility.getHeader(AnLocale.getString("Notes:"));

      if (notesText == null || notesText.equals("")) {
        notesText = AnLocale.getString("<Empty>");
      }
      notes_text_area = new AnUtility.AnTextArea(AnUtility.trimNewLine(notesText), true);
      AnUtility.setAccessibleContext(
          notes_text_area.getAccessibleContext(), AnLocale.getString("Notes area"));

      notesLabel.setLabelFor(notes_text_area);
      notesLabel.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(1, 4, 1, 1)));
      notesLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
      notesPanel.add(notesLabel);
      notesPanel.add(buttPanel);
      add(notesPanel);
      add(notes_text_area);

      final JPanel infoPanel = new JPanel(new FlowLayout(java.awt.FlowLayout.LEFT, 2, 2));
      infoPanel.setBorder(new EtchedBorder());
      final AnUtility.AnLabel infoLabel =
          (AnUtility.AnLabel) AnUtility.getHeader(AnLocale.getString("Info:"));
      final AnUtility.AnTextArea infoTextArea =
          new AnUtility.AnTextArea(AnUtility.trimNewLine(info_text), false);
      AnUtility.setAccessibleContext(
          infoTextArea.getAccessibleContext(), AnLocale.getString("Info area"));
      infoLabel.setLabelFor(infoTextArea);
      infoLabel.setBorder(new EmptyBorder(new java.awt.Insets(1, 2, 1, 1)));
      infoPanel.add(infoLabel);
      add(infoPanel);
      add(infoTextArea);
    }

    // createUndoMananger creating undo manager
    public void createUndoMananger() {
      if (undo_manager_exists) {
        return;
      }
      undoManager = new AnUndoManager();
      undoManager.setLimit(UNDO_LIMIT);
      undo_manager_exists = true;
    }

    // removeUndoMananger removing undo manager
    public void removeUndoMananger() {
      undoManager.end();
      cnt_edit_actions = 0;
      cnt_undo_actions = 0;
      saveAction.setEnabled(false);
      undoAction.setEnabled(false);
      redoAction.setEnabled(false);
      undo_manager_exists = false;
    }

    @Override
    public void focusGained(final FocusEvent fe) {
      createUndoMananger();
    }

    @Override
    public void focusLost(final FocusEvent fe) {
      // removeUndoMananger();
    }

    // undoableEditHappened called when edit happened
    @Override
    public void undoableEditHappened(final UndoableEditEvent e) {
      // add the edits to the unod manager
      undoManager.addEdit(e.getEdit());
      cnt_edit_actions++;
      cnt_undo_actions = 0; // no redo acts after editing
      redoAction.setEnabled(false);
      saveAction.setEnabled(true);
      undoAction.setEnabled(true);
    }

    @Override
    public void keyPressed(final KeyEvent e) {
      createUndoMananger();
      if ((e.getKeyCode() == KeyEvent.VK_Z) && (e.isControlDown())) {
        if (!undoAction.isEnabled()) {
          return; // Undo changes
        }
        try {
          undoManager.undo();
        } catch (CannotUndoException cue) {
          Toolkit.getDefaultToolkit().beep();
        }
      }
      if ((e.getKeyCode() == KeyEvent.VK_Y) && (e.isControlDown())) {
        if (!redoAction.isEnabled()) {
          return; // Redo changes
        }
        try {
          undoManager.redo();
        } catch (CannotRedoException cue) {
          Toolkit.getDefaultToolkit().beep();
        }
      }
      if ((e.getKeyCode() == KeyEvent.VK_S) && (e.isControlDown())) { // Save changes
        if (!saveAction.isEnabled()) {
          return;
        }
        undoManager.save();
      }

      if ((e.getKeyCode() == KeyEvent.VK_D) && (e.isControlDown())) { // Delete notes file
        if (!deleteAction.isEnabled()) {
          return;
        }
        undoManager.delete(true);
      }
    }

    @Override
    public void keyReleased(final KeyEvent e) {}

    @Override
    public void keyTyped(final KeyEvent e) {}

    @Override
    public void actionPerformed(final ActionEvent e) {
      createUndoMananger();
      if (e.getActionCommand().equals("__save")) {
        undoManager.save();
      } else if (e.getActionCommand().equals("__undo")) {
        try {
          undoManager.undo();
        } catch (CannotUndoException cue) {
          Toolkit.getDefaultToolkit().beep();
        }
      } else if (e.getActionCommand().equals("__redo")) {
        try {
          undoManager.redo();
        } catch (CannotRedoException cue) {
          Toolkit.getDefaultToolkit().beep();
        }
      } else if (e.getActionCommand().equals("__delete")) {
        undoManager.delete(true);
      }
    }

    public void updateAllExps(final int type) {
      SwingUtilities.invokeLater(
          new Runnable() {
            @Override
            public void run() {
              DefaultMutableTreeNode root, node;
              AnTree the_tree;
              AnTreePanel panel;
              final String cur_exp;
              String exp;

              //                    final int win_size = window.get_win_size();
              cur_exp = new File(exp_list[exp_id].getText()).getAbsolutePath();

              //                    for (int i = 0; i < win_size; i++) {
              the_tree = window.getExperimentsView().tree;
              root = ((DefaultMutableTreeNode) the_tree.getModel().getRoot());

              final int cnt = root.getChildCount();
              for (int k = 1; k < cnt; k++) {
                node = (DefaultMutableTreeNode) root.getChildAt(k);
                exp = new File(((AnTextIcon) node.getUserObject()).getText()).getAbsolutePath();
                if (cur_exp.equals(exp)) {
                  final DefaultMutableTreeNode nd = (DefaultMutableTreeNode) node.getChildAt(0);
                  panel = (AnTreePanel) nd.getUserObject();
                  if (!panel.equals(AnTreePanel.this)) {
                    panel.createUndoMananger();
                    panel.notes_text_area.setText(notesText);
                    updateNotes(panel.exp_id, type, (type == SAVE_FILE) ? notesText : null, false);
                    panel.deleteAction.setEnabled(type == SAVE_FILE);
                    panel.removeUndoMananger();
                    ((DefaultTreeModel) the_tree.getModel()).nodeChanged(node);
                  }
                }
              }
            }
            //                }
          });
    }

    private final class AnUndoManager extends UndoManager {

      @Override
      public synchronized void undo() throws CannotUndoException {
        super.undo();
        cnt_edit_actions--;
        cnt_undo_actions++;
        if (cnt_edit_actions == 0) {
          saveAction.setEnabled(false);
          undoAction.setEnabled(false);
        } else {
          saveAction.setEnabled(true);
        }
        redoAction.setEnabled(true);
      } // undo

      @Override
      public synchronized void redo() throws CannotUndoException {
        super.redo();
        cnt_edit_actions++;
        cnt_undo_actions--;
        saveAction.setEnabled(true);
        undoAction.setEnabled(true);
        redoAction.setEnabled(cnt_undo_actions != 0);
      } // redo

      public synchronized void save() {
        notesText = notes_text_area.getText();

        // delete emty notes files
        if (notesText.equals("")) {
          if (delete(deleteAction.isEnabled()) != 0) { // delete if file exists
            undo_all(); // undo all changes if not confirmed
          }
          removeUndoMananger();
          return;
        }

        final int status = updateNotes(exp_id, SAVE_FILE, AnUtility.addNewLine(notesText), true);

        if (status != 0) {
          window.showError(AnLocale.getString("Cannot write notes file"));
          undo_all();
        } else {
          deleteAction.setEnabled(true);

          final DefaultMutableTreeNode root;
          final DefaultMutableTreeNode node;

          root = ((DefaultMutableTreeNode) tree.getModel().getRoot());
          node = (DefaultMutableTreeNode) root.getChildAt(exp_id + 1);
          ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

          updateAllExps(SAVE_FILE);
        }
        removeUndoMananger();
      } // save

      public synchronized int delete(final boolean confirm) {
        if (confirm) {
          final int resp =
              JOptionPane.showConfirmDialog(
                  window.getFrame(),
                  AnLocale.getString("Delete notes file ?"),
                  UIManager.getString("OptionPane.titleText"),
                  JOptionPane.YES_NO_OPTION);

          if (resp != JOptionPane.YES_OPTION) {
            return -1;
          }
        }

        final int status = updateNotes(exp_id, DELETE_FILE, null, true);
        notesText = AnLocale.getString("<Empty>");
        deleteAction.setEnabled(false);

        if (status != 0) {
          if (confirm) {
            window.showError(AnLocale.getString("Cannot delete notes file"));
          }
          return -1;
        }

        notes_text_area.setText(AnUtility.trimNewLine(notesText));
        updateAllExps(DELETE_FILE);
        removeUndoMananger();

        return 0;
      } // delete

      private void undo_all() {
        while (cnt_edit_actions > 0) {
          undo();
        }
      } // undo_all
    }
  }

  private int updateNotes(
      final int exp_id, final int type, final String text, final boolean handle_file) {
    synchronized (IPC.lock) {
      window.IPC().send("updateNotes");
      window.IPC().send(0);
      window.IPC().send(exp_id);
      window.IPC().send(type);
      window.IPC().send(text);
      window.IPC().send(handle_file);
      return window.IPC().recvInt();
    }
  }

  private class AnMenuListener extends MouseAdapter {

    private boolean debug;
    // NM private AnTable anTable;
    JTree tree;

    AnMenuListener(JTree tree) {
      this.tree = tree;
      debug = false;
    }

    public JPopupMenu initPopup(MouseEvent event) {
      AccessibleContext ac;
      JMenuItem mi;
      JPopupMenu popup = new JPopupMenu();
      String txt;

      boolean row_selected = false;
      int row = tree.getLeadSelectionRow();
      if (row >= 0) {
        row_selected = true;
      }

      return popup;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        JPopupMenu popup = initPopup(e);
        if (popup != null) {
          popup.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuWillBecomeInvisible(" + e + ")");
      }
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuWillBecomeVisible(" + e + ")");
      }
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuCanceled(" + e + ")");
      }
    }
  }

  @Override
  public java.util.List<ExportFormat> getSupportedExportFormats() {
    java.util.List<ExportFormat> formats = new ArrayList<ExportFormat>();
    formats.add(ExportFormat.JPG);
    return formats;
  }

  @Override
  public boolean exportLimitSupported() {
    return true;
  }
}

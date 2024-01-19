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

package org.gprofng.mpmt.settings;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnDialog;
import org.gprofng.mpmt.AnFile;
import org.gprofng.mpmt.AnList;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.navigation.View;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.settings.CompareModeSetting.CompareMode;
import org.gprofng.mpmt.settings.ViewModeSetting.ViewMode;
import org.gprofng.mpmt.util.gui.AnJPanel;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnTabbedPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.gui.AnUtility.AnRadioButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public final class Settings
    implements ActionListener, ItemListener, DocumentListener, ChangeListener {

  private Settings instance = null;
  private final Frame frame;
  private AnWindow anWindow;
  private SettingsDialog settingsDialog;
  private JTabbedPane tabbedPane;
  public int settingsviewsIndex = -1;
  public int settingsMetricsIndex = -1;
  public int settingsTimelineIndex = -1;
  public int settingsSourceDisassemblyIndex = -1;
  public int settingsCalltreeIndex = -1;

  // States
  private final ViewModeSetting viewModeSetting = new ViewModeSetting();
  private final ViewModeEnabledSetting viewModeEnabledSetting = new ViewModeEnabledSetting();
  private final FormatSetting formatSetting = new FormatSetting();
  private final TableSettings tableSettings = new TableSettings();
  private final CompareModeSetting compareModeSetting = new CompareModeSetting();
  private final TimelineSetting timelineSetting = new TimelineSetting();
  private final SourceDisassemblySetting sourceDisassemblySetting = new SourceDisassemblySetting();
  private final MetricsSetting metricSetting = new MetricsSetting();
  private final SearchPathSetting searchPathSetting = new SearchPathSetting();
  private final PathMapSetting pathMapSetting = new PathMapSetting();
  private final CallTreeSetting callTreeSetting = new CallTreeSetting();
  private final ViewsSetting viewsSetting = new ViewsSetting();
  private final LibraryVisibilitySetting libraryVisibilitySetting = new LibraryVisibilitySetting();

  // Metrics Tab
  private MetricsPanel metricsPanel;

  // Source/Disassembly Tab
  private int vis_src;
  private int vis_dis;
  private static final int[] ccmv_type;
  private static final String[] ccmv_desc;
  private static final String[] ccmv_comd;
  private static final char[] ccmv_mnem;
  private int threshold_src;
  private int threshold_dis;
  private boolean src_visible;
  private boolean src_metric;
  private boolean hex_visible;
  private boolean cmp_visible;
  private boolean func_visible;
  private boolean scope_only;
  private JComponent[] src_chks;
  private JComponent[] dis_chks;
  private JComponent[] threshold;
  private JCheckBox dis_src;
  private JCheckBox dis_asrc;
  private JCheckBox dis_hex;
  private JCheckBox cmp_line;
  private JCheckBox func_line;
  private JCheckBox dis_scope;

  // Formats Tab
  private AnList tab_name_format;
  // Formats Tab: Function Name Style
  private JRadioButton short_fmt;
  private JRadioButton mangled_fmt;
  private JRadioButton long_fmt;
  private JCheckBox soname_chk;
  // Formats Tab: View Mode
  private JComponent viewModeLabel;
  private JRadioButton view_fmt;
  private JRadioButton view_expert_fmt;
  private JRadioButton view_machine_fmt;
  // Formats Tab: Compare
  private JLabel cmpModeLabel;
  private JRadioButton cmp_abs_value;
  private JRadioButton cmp_delta;
  private JRadioButton cmp_ratio;
  // Wrap long metric names in table headers
  private JCheckBox wrapNameCheckBox;

  // Timeline Tab
  public static final int PROP_NONE = 0;
  private ArrayList<JCheckBox> tldata_checkBoxes; // checkboxes
  private ArrayList<String> tldata_check_unames; // names shown for checkboxes
  private int previous_tldata_names_version;
  private boolean tldata_show_states; // show per-DATA_* event states (mstates)
  private boolean tldata_show_counts; // show per-DATA_* event values
  private JCheckBox tldata_show_states_cb; // checkboxes
  private JCheckBox tldata_show_counts_cb; // checkboxes
  // TL options' components
  private JPanel tl_type_panel;
  private ArrayList<JRadioButton> tl_entity_button; // Entities == LWP, Thread, CPU, Experiments
  private JRadioButton[] tl_stack_align;
  private JSlider tl_slider;
  private int tldata_stack_depth;
  private boolean needsTimelineEvent = false;

  // Search Path
  private SearchPathPanel searchPathPanel;

  // Path Map
  private PathmapsPanel pathmapsPanel;

  // Views Tab
  private JPanel viewsPanel;
  private JPanel memchk_panel, indxchk_panel;
  private AnList memlist, indxlist;
  private CustomMemDialog tabs_custom_mem_dlg;
  private CustomIndxDialog tabs_custom_indx_dlg;
  private AnUtility.AnCheckBox[] staticViewCheckBoxes;
  private AnUtility.AnCheckBox[] standardObjectCheckBoxes;
  private AnUtility.AnCheckBox[] memoryObjectCheckBoxes;
  private AnUtility.AnCheckBox[] indexObjectCheckBoxes;

  // Call Tree
  private CallTreePanel callTreePanel;

  // Constructor
  public Settings(final AnWindow window, final Frame frame) {
    //        name_col = -1;

    // tldata
    tldata_checkBoxes = new ArrayList();
    tldata_check_unames = new ArrayList();
    previous_tldata_names_version = 0;
    tldata_show_states = false;
    tldata_show_counts = false;
    tldata_stack_depth = 0;

    anWindow = window;
    this.frame = frame;
    instance = this;
    initComponents();
    updateGUIViewModeVisible();
    settingsDialog = new SettingsDialog(); // FIXUP: Settings dialog keeps some states!!!!!!!
  }

  private Settings getDefault() {
    return instance;
  }

  public SourceDisassemblySetting getSourceDisassemblySetting() {
    return sourceDisassemblySetting;
  }

  public ViewModeSetting getViewModeSetting() {
    return viewModeSetting;
  }

  public ViewModeEnabledSetting getViewModeEnabledSetting() {
    return viewModeEnabledSetting;
  }

  public FormatSetting getFormatSetting() {
    return formatSetting;
  }

  public CompareModeSetting getCompareModeSetting() {
    return compareModeSetting;
  }

  public MetricsSetting getMetricsSetting() {
    return metricSetting;
  }

  public TimelineSetting getTimelineSetting() {
    return timelineSetting;
  }

  public SearchPathSetting getSearchPathSetting() {
    return searchPathSetting;
  }

  public PathMapSetting getPathMapSetting() {
    return pathMapSetting;
  }

  public CallTreeSetting getCallTreeSetting() {
    return callTreeSetting;
  }

  public ViewsSetting getViewsSetting() {
    return viewsSetting;
  }

  public TableSettings getTableSettings() {
    return tableSettings;
  }

  public LibraryVisibilitySetting getLibraryVisibilitySetting() {
    return libraryVisibilitySetting;
  }

  // Initialize GUI components
  private void initComponents() {
    JPanel panel;
    Dimension psize;
    int tabCount = 0;

    tabbedPane = new AnTabbedPane();
    String txt = AnLocale.getString("Presentation Tabs");
    AnUtility.setAccessibleContext(tabbedPane.getAccessibleContext(), txt);

    // Views
    viewsPanel = new AnJPanel(new GridBagLayout());
    txt = AnLocale.getString("Views");
    tabbedPane.addTab(txt, new AnJScrollPane(viewsPanel));
    tabCount++;
    txt = AnLocale.getString("Select Tabs");
    AnUtility.setAccessibleContext(viewsPanel.getAccessibleContext(), txt);
    settingsviewsIndex = tabCount - 1;

    // Metrics
    txt = AnLocale.getString("Metrics");
    metricsPanel = new MetricsPanel();
    tabbedPane.addTab(txt, metricsPanel);
    tabCount++;
    settingsMetricsIndex = tabCount - 1;

    // Timeline
    panel = new AnJPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    edt_initTimeline(panel); // IPC!
    txt = AnLocale.getString("Timeline");
    panel.setName(txt);
    panel.getAccessibleContext().setAccessibleName(txt);
    tabbedPane.addTab(txt, new AnJScrollPane(panel));
    tabCount++;
    settingsTimelineIndex = tabCount - 1;

    // Source and Disassembly
    panel = new AnJPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    initSourceDisassembly(panel);
    tabbedPane.addTab(AnLocale.getString("Source/Disassembly"), new AnJScrollPane(panel));
    tabCount++;
    settingsSourceDisassemblyIndex = tabCount - 1;

    // Call Tree
    callTreePanel = new CallTreePanel();
    txt = AnLocale.getString("Call Tree");
    tabbedPane.addTab(txt, new AnJScrollPane(callTreePanel));
    tabCount++;
    txt = AnLocale.getString("Call Tree");
    AnUtility.setAccessibleContext(callTreePanel.getAccessibleContext(), txt);
    settingsCalltreeIndex = tabCount - 1;

    // Formats
    tab_name_format = new AnList(false);
    initNameFormat(tab_name_format);
    AnUtility.setAccessibleContext(tab_name_format.getAccessibleContext(), FORMATS_STR);
    tabbedPane.addTab(FORMATS_STR, new AnJScrollPane(tab_name_format));
    tabCount++;

    // Search Path
    searchPathPanel = new SearchPathPanel(this);
    txt = AnLocale.getString("Search Path");
    tabbedPane.addTab(txt, searchPathPanel);
    tabCount++;

    // Path Mapping
    pathmapsPanel = new PathmapsPanel(this);
    txt = AnLocale.getString("Pathmaps");
    tabbedPane.addTab(txt, pathmapsPanel);
    tabCount++;

    psize = tabbedPane.getPreferredSize();
    if (psize.width < MIN_WIDTH) {
      psize.width = MIN_WIDTH;
    }
    tabbedPane.setPreferredSize(psize);
  }

  /**
   * Opens "Settings" dialog with the tab on top specified by parameter Available values:
   * presentation.timeline_index
   *
   * @param tabIndex
   */
  public void showDialog(int tabIndex) {
    if (tabIndex >= 0) {
      if (tabIndex < tabbedPane.getComponentCount()) {
        tabbedPane.setSelectedIndex(tabIndex);
      }
    }
    showDialog(true);
  }

  public void showDialog(boolean show) {
    if (settingsDialog == null) {
      settingsDialog = new SettingsDialog();
    }
    // Refresh the panel to get changes made in other views
    if (show) {
      settingsDialog.showDialog(true);
    } else {
      settingsDialog.showDialog(false);
    }
  }

  // Initialize components for the Source and Disassembly pane
  private void initSourceDisassembly(final JPanel annotate) {
    final JPanel panel;
    JPanel chk_panel, text_panel;
    final AnList list;
    JLabel label;
    int i;
    final String[] threshold_str;

    int[] defaultSettings = getSourceDisassemblySetting().getDefaultSourceDisassemblySetting();
    vis_src = defaultSettings[0];
    vis_dis = defaultSettings[1];
    threshold_src = defaultSettings[2];
    threshold_dis = defaultSettings[3];
    src_visible = defaultSettings[4] == 0 ? false : true;
    src_metric = defaultSettings[5] == 0 ? false : true;
    hex_visible = defaultSettings[6] == 0 ? false : true;
    cmp_visible = defaultSettings[7] == 0 ? false : true;
    scope_only = defaultSettings[8] == 0 ? false : true;
    func_visible = defaultSettings[9] == 0 ? false : true;

    panel = new AnJPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    // Compiler Commentary
    list = new AnList(false);
    chk_panel = new AnJPanel(new GridLayout(1, 2));
    chk_panel.add(AnUtility.getHeader(AnLocale.getString("Source")));
    chk_panel.add(AnUtility.getHeader(AnLocale.getString("Disassembly")));
    String txt = AnLocale.getString("Compiler Commentary");
    list.add(AnUtility.getItem(txt), chk_panel);
    txt = AnLocale.getString("Compiler Commentary");
    AnUtility.setAccessibleContext(list.getAccessibleContext(), txt);

    // Space for adding selecting choice
    src_chks = new JComponent[size_ccmv];
    dis_chks = new JComponent[size_ccmv];
    threshold = new JComponent[2];

    for (i = 0; i < size_ccmv; i++) {
      chk_panel = new AnJPanel(new GridLayout(1, 2));

      chk_panel.add(src_chks[i] = new AnUtility.AnCheckBox(" ", (vis_src & ccmv_type[i]) != 0));
      ((AnUtility.AnCheckBox) src_chks[i]).addItemListener(this);
      chk_panel.add(dis_chks[i] = new AnUtility.AnCheckBox(" ", (vis_dis & ccmv_type[i]) != 0));
      ((AnUtility.AnCheckBox) dis_chks[i]).addItemListener(this);

      label = (JLabel) AnUtility.getItem(ccmv_desc[i]);
      label.setDisplayedMnemonic(ccmv_mnem[i]);
      label.setLabelFor(src_chks[i]);
      AnUtility.setAccessibleContext(label.getAccessibleContext(), ccmv_desc[i]);
      list.add(label, chk_panel);
    }
    // Syncing states with GUI
    vis_src = 0;
    vis_dis = 0;
    for (int ii = 0; ii < size_ccmv; ii++) {
      if (((JCheckBox) src_chks[ii]).isSelected()) {
        vis_src |= ccmv_type[ii];
      }

      if (((JCheckBox) dis_chks[ii]).isSelected()) {
        vis_dis |= ccmv_type[ii];
      }
    }

    // Highlighting Threshold
    chk_panel = new AnJPanel(new GridLayout(1, 2));
    threshold_str = new String[2];
    threshold_str[0] = Integer.toString(threshold_src);
    threshold_str[1] = Integer.toString(threshold_dis);
    for (i = 0; i < 2; i++) {
      threshold[i] = AnUtility.getNumber(threshold_str[i], 3);
      ((JTextField) threshold[i]).setEditable(true);
      ((JTextField) threshold[i]).getDocument().addDocumentListener(this);
      chk_panel.add(text_panel = new AnJPanel());
      text_panel.add(threshold[i]);
    }

    txt = AnLocale.getString("Hot Lines highlighting Threshold (%):");
    label = (JLabel) AnUtility.getItem(txt);
    //        label.setDisplayedMnemonic(AnLocale.getString('T',
    // "MNEM_PRESENT_HIGHLIGHTING_THRESHOLD"));
    label.setLabelFor(threshold[0]);
    AnUtility.setAccessibleContext(label.getAccessibleContext(), txt);

    list.add(label, chk_panel);
    list.setAlignmentX();
    list.setAlignmentY();

    panel.add(list);

    // Disassembly Listing
    AnList list2 = new AnList(false);
    list2.add(AnUtility.getItem(AnLocale.getString("Show in Disassembly Listing:")), null);
    list2.setAlignmentX();
    list2.setAlignmentY();
    panel.add(list2);
    dis_src = new AnUtility.AnCheckBox(AnLocale.getString("Source Code"), src_visible);
    //        dis_src.setMnemonic(AnLocale.getString('r', "MNEM_PRESENT_SOURCE_CODE"));
    dis_src.addItemListener(this);

    dis_asrc = new AnUtility.AnCheckBox(AnLocale.getString("Metrics for Source Lines"), src_metric);
    //        dis_asrc.setMnemonic(AnLocale.getString('e',
    // "MNEM_PRESENT_METRICS_FOR_SOURCE_LINES"));
    dis_asrc.addItemListener(this);

    dis_hex = new AnUtility.AnCheckBox(AnLocale.getString("Hexadecimal Instructions"), hex_visible);
    //        dis_hex.setMnemonic(AnLocale.getString('x', "MNEM_PRESENT_HEXADECIMAL_INSTRUCTIONS"));
    dis_hex.addItemListener(this);

    dis_scope =
        new AnUtility.AnCheckBox(
            AnLocale.getString("Only Show Data of Current Function"), scope_only);
    //        dis_scope.setMnemonic(AnLocale.getString('u',
    // "MNEM_PRESENT_ONLY_SHOW_DATA_OF_CURRENT_FUNCTION"));
    dis_scope.addItemListener(this);

    chk_panel = new AnJPanel();
    chk_panel.setLayout(new BoxLayout(chk_panel, BoxLayout.Y_AXIS));
    chk_panel.add(dis_src);
    chk_panel.add(dis_asrc);
    chk_panel.add(dis_hex);
    chk_panel.add(dis_scope);
    panel.add(chk_panel);

    // Disassembly Listing
    AnJPanel chk_panel2 = new AnJPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
    panel.add(chk_panel2); // just a separator
    AnList list3 = new AnList(false);
    list3.add(
        AnUtility.getItem(AnLocale.getString("Show in Source and Disassembly Listing:")), null);
    list3.setAlignmentX();
    list3.setAlignmentY();
    panel.add(list3);

    cmp_line =
        new AnUtility.AnCheckBox(
            AnLocale.getString("Show compiler command-line flags"), cmp_visible);
    //        cmp_line.setMnemonic(AnLocale.getString('s', "MNEM_PRESENT_SHOW_COMMAND_LINE_FLAGS"));
    cmp_line.addItemListener(this);

    func_line =
        new AnUtility.AnCheckBox(AnLocale.getString("Show function beginning line"), func_visible);
    //        func_line.setMnemonic(AnLocale.getString('f',
    // "MNEM_PRESENT_SHOW_FUNCTION_SEPARATOR_LINE"));
    func_line.addItemListener(this);

    AnJPanel chk_panel3 = new AnJPanel();
    chk_panel3.setLayout(new BoxLayout(chk_panel3, BoxLayout.Y_AXIS));
    chk_panel3.add(cmp_line);
    chk_panel3.add(func_line);
    panel.add(chk_panel3);

    annotate.add(panel);
  }

  /** Sets display mode without enabling OK/Apply Called (indirectly) from outside Presentation */
  private void updateGUIViewMode() {
    view_fmt.removeItemListener(this);
    view_expert_fmt.removeItemListener(this);
    view_machine_fmt.removeItemListener(this);

    ViewMode mode = getViewModeSetting().get();
    view_fmt.setSelected(mode == ViewModeSetting.ViewMode.USER);
    view_expert_fmt.setSelected(mode == ViewModeSetting.ViewMode.EXPERT);
    view_machine_fmt.setSelected(mode == ViewModeSetting.ViewMode.MACHINE);

    view_fmt.addItemListener(this);
    view_expert_fmt.addItemListener(this);
    view_machine_fmt.addItemListener(this);
  }

  private void updateGUIViewModeVisible() {
    boolean set = getViewModeEnabledSetting().isViewModeEnabled();
    viewModeLabel.setEnabled(set);
    view_fmt.setEnabled(set);
    view_expert_fmt.setEnabled(set);
    view_machine_fmt.setEnabled(set);
  }

  /**
   * Updates Name Format mode without enabling OK/Apply Called from within Presentations (clicking
   * on radio buttons)
   */
  private void updateGUINameFormat() {
    short_fmt.removeItemListener(this);
    long_fmt.removeItemListener(this);
    mangled_fmt.removeItemListener(this);

    FormatSetting.Style style = getFormatSetting().getStyle();
    boolean soName = getFormatSetting().getAppendSoName();
    short_fmt.setSelected(style == FormatSetting.Style.SHORT);
    long_fmt.setSelected(style == FormatSetting.Style.LONG);
    mangled_fmt.setSelected(style == FormatSetting.Style.MANGLED);
    soname_chk.setSelected(soName);

    short_fmt.addItemListener(this);
    long_fmt.addItemListener(this);
    mangled_fmt.addItemListener(this);
  }

  /**
   * Sets display compare mode without enabling OK/Apply Called (indirectly) from outside
   * Presentation
   */
  private void updateGUIDisplayCompareMode() {
    cmp_abs_value.removeItemListener(this);
    cmp_delta.removeItemListener(this);
    cmp_ratio.removeItemListener(this);

    CompareMode mode = getCompareModeSetting().get();
    boolean enbl = (mode != CompareMode.CMP_DISABLE);
    cmpModeLabel.setEnabled(enbl);
    cmp_abs_value.setEnabled(enbl);
    cmp_delta.setEnabled(enbl);
    cmp_ratio.setEnabled(enbl);
    cmp_abs_value.setSelected(!enbl || (mode == CompareMode.CMP_ENABLE));
    cmp_delta.setSelected(mode == CompareMode.CMP_DELTA);
    cmp_ratio.setSelected(mode == CompareMode.CMP_RATIO);

    cmp_abs_value.addItemListener(this);
    cmp_delta.addItemListener(this);
    cmp_ratio.addItemListener(this);
  }

  /**
   * Updates Name Format mode without enabling OK/Apply Called from within Presentations (clicking
   * on radio buttons)
   */
  private void updateGUITableFormat() {
    wrapNameCheckBox.setSelected(getTableSettings().wrapMetricNames());
  }

  /**
   * Sets SourceDisassembly settings without enabling OK/Apply Called (indirectly) from outside
   * Presentation
   */
  private void updateGUISourceDisassembly() {
    final int[] anno_set = getSourceDisassemblySetting().get();

    vis_src = anno_set[0];
    vis_dis = anno_set[1];
    threshold_src = anno_set[2];
    threshold_dis = anno_set[3];
    src_visible = (anno_set[4] == 0) ? false : true;
    src_metric = (anno_set[5] == 0) ? false : true;
    hex_visible = (anno_set[6] == 0) ? false : true;
    cmp_visible = (anno_set[7] == 0) ? false : true;
    scope_only = (anno_set[8] == 0) ? false : true;
    func_visible = (anno_set[9] == 0) ? false : true;

    for (int i = 0; i < size_ccmv; i++) {
      ((JCheckBox) src_chks[i]).setSelected((vis_src & ccmv_type[i]) != 0);
      ((JCheckBox) dis_chks[i]).setSelected((vis_dis & ccmv_type[i]) != 0);
    }
    ((JTextField) threshold[0]).setText("" + threshold_src);
    ((JTextField) threshold[1]).setText("" + threshold_dis);
    dis_src.setSelected(src_visible);
    dis_asrc.setSelected(src_metric);
    dis_hex.setSelected(hex_visible);
    cmp_line.setSelected(cmp_visible);
    dis_scope.setSelected(scope_only);
    func_line.setSelected(func_visible);
  }

  /**
   * Sets Timeline settings without enabling OK/Apply Called (indirectly) from outside Presentation
   */
  private void updateGUITimeline() {
    boolean buttonsEnabled = settingsDialog.areButtonsEnabled();

    tl_entity_button.get(getTimelineSetting().getTl_entity_selected_btn()).setSelected(true);
    tl_slider.setValue(
        getTimelineSetting().getTLStackFramePixels() - TIMELINE_STACK_FRAME_MAGNIFY_OFFSET);
    //        tl_slider.setValue(getTimelineSetting().getTLStackDepth());
    tldata_stack_depth = getTimelineSetting().getTLStackDepth();
    tl_stack_align[0].setSelected(
        getTimelineSetting().getStack_align() == TLStack_align.TLSTACK_ALIGN_ROOT);
    tl_stack_align[1].setSelected(
        getTimelineSetting().getStack_align() == TLStack_align.TLSTACK_ALIGN_LEAF);

    tldata_show_states_cb.setSelected(getTimelineSetting().getShowEventStates());
    tldata_show_states = tldata_show_states_cb.isSelected();
    tldata_show_counts_cb.setSelected(getTimelineSetting().getShowEventDensity());
    tldata_show_counts = tldata_show_counts_cb.isSelected();

    settingsDialog.setButtonsEnabled(buttonsEnabled);
  }

  public static String getErRcTLDataCmdName(TLData_type tldata_type, String auxName) {
    if (tldata_type == TLData_type.TL_UNKNOWN) {
      return null;
    }
    String cmd = tldata_type.getTLDataBaseCmd();
    if (auxName == null) {
      return cmd;
    }
    String tlDataCmd = cmd + "(" + auxName + ")";
    return tlDataCmd;
  }

  public static String getErRcTLDataLongUName(String dataUName, String auxUName) {
    if (auxUName == null) {
      return dataUName;
    }
    String baseUName = "HW Counter"; // uname is _too_ long
    String longUName = baseUName + " (" + auxUName + ")";
    return longUName;
  }

  private void updateGUITimelineData() {
    // translate metric selections to timeline DATA checkbox mask
    MetricsSetting metricsSetting = getMetricsSetting();
    AnMetric[] anMetrics = metricsSetting.getAvailableAnMetrics();
    Set<String> allList = new HashSet();
    Set<String> enabledList = new HashSet();
    for (int ii = 0; ii < anMetrics.length; ii++) {
      String dataType = anMetrics[ii].getDataTypeName();
      if (dataType == null) {
        continue; // for example, derived metrics
      }
      String tlDataCmd = getErRcTLDataCmdName(TLData_type.find(dataType), anMetrics[ii].aux);
      if (tlDataCmd == null) {
        continue; // weird
      }
      allList.add(tlDataCmd);
      //            boolean selected = metricsSetting.isSelected(ii);
      boolean selected = metricsSetting.isSelected(anMetrics[ii].getComd());
      //            if (selected != selected2) {
      //                System.out.println("ERROR******************** updateGUITimelineData");
      //            }
      if (selected) {
        enabledList.add(tlDataCmd);
      }
    }
    Set<String> disabledList = new HashSet(allList);
    for (String enabled : enabledList) {
      disabledList.remove(enabled);
    }
    edt_TLData_disable_only(allList, disabledList);
  }

  private boolean edt_TLData_disable_only(Set<String> allData, Set<String> disabledData) {
    long tldata_check_hidden_bitmask = timelineSetting.getTLDataHiddenMask();
    long new_mask = timelineSetting.setTLDataVisibility_edt(allData, disabledData);
    boolean changed = new_mask != tldata_check_hidden_bitmask;
    if (changed) {
      for (int i = 0; i < tldata_checkBoxes.size(); i++) {
        long hidden = ((1L << i) & new_mask);
        tldata_checkBoxes.get(i).setSelected(hidden == 0);
      }
      tl_type_panel.repaint();
      needsTimelineEvent = true; // FIXUP, is this correct?
      checkTimelineChanged(this);
    }
    return changed;
  }

  private void updateGUISearchPath() {
    searchPathPanel.updateGUI();
  }

  private void updateGUIPathMap() {
    //        pathMapPanel.updateGUI();
    pathmapsPanel.updateGUI();
  }

  private void updateGUICallTree() {
    callTreePanel.updateGUI();
  }

  private void updateGUIMetrics() {
    metricsPanel.updateGUIElements();
  }

  private void updateGUIViews(ViewsSetting.Setting oldSetting, ViewsSetting.Setting newSetting) {
    AnUtility.checkIfOnAWTThread(true);
    if (ViewsSetting.viewsChanged(oldSetting, newSetting)) {
      setViewComponents();
    }
  }

  private JPanel get_space_panel() {
    // Create blank space for better grouping
    JPanel pnl = new JPanel();
    pnl.setOpaque(false);
    pnl.setSize(20, 40);
    return pnl;
  }

  private JLabel get_JLabel(final String txt) {
    final JLabel lbl = (JLabel) AnUtility.getItem(txt);
    AnUtility.setAccessibleContext(lbl.getAccessibleContext(), txt);
    return lbl;
  }

  // Initialize components for the Name Formats pane
  private void initNameFormat(final AnList format) {
    //        formatSetting.initialize(getNameFormat(), getSoName()); // IPC!!!!!
    ButtonGroup group = new ButtonGroup();
    long_fmt =
        new AnUtility.AnRadioButton(
            AnLocale.getString("Long"), formatSetting.getStyle() == FormatSetting.Style.LONG);
    long_fmt.setMnemonic(AnLocale.getString('L', "MNEM_PRESENT_LONG"));
    long_fmt.addItemListener(this);
    group.add(long_fmt);
    format.add(get_JLabel(AnLocale.getString("Function Name Style:")), long_fmt);

    short_fmt =
        new AnUtility.AnRadioButton(
            AnLocale.getString("Short"), formatSetting.getStyle() == FormatSetting.Style.SHORT);
    short_fmt.setMnemonic(AnLocale.getString('t', "MNEM_PRESENT_SHORT"));
    short_fmt.addItemListener(this);
    group.add(short_fmt);
    format.add((JComponent) null, short_fmt);

    mangled_fmt =
        new AnUtility.AnRadioButton(
            AnLocale.getString("Mangled"), formatSetting.getStyle() == FormatSetting.Style.MANGLED);
    mangled_fmt.setMnemonic(AnLocale.getString('M', "MNEM_PRESENT_MANGLED"));
    mangled_fmt.addItemListener(this);
    group.add(mangled_fmt);
    format.add((JComponent) null, mangled_fmt);
    format.add((JComponent) null, get_space_panel());

    soname_chk = new AnUtility.AnCheckBox(Blank);
    soname_chk.setSelected(formatSetting.getAppendSoName());
    soname_chk.addItemListener(this);

    final JLabel so_label = get_JLabel(AnLocale.getString("Append SO name to Function name:"));
    char ch_mnem = AnLocale.getString('p', "MNEM_PRESENT_SONAME");
    so_label.setDisplayedMnemonic(ch_mnem);
    so_label.setLabelFor(soname_chk);
    format.add(so_label, soname_chk);
    format.add((JComponent) null, get_space_panel());

    // Add View mode: User/Expert/Machine
    //        view_mode = JMODE_USER; // Don't ask IPC. Just use User mode for now, it will get set
    // correcly after loading experiments
    group = new ButtonGroup();
    view_fmt = new AnUtility.AnRadioButton(AnLocale.getString("User", "PRESENT_USER"), true);
    view_fmt.setMnemonic(AnLocale.getString('U', "MNEM_PRESENT_USER"));
    view_fmt.addItemListener(this);
    group.add(view_fmt);
    viewModeLabel = AnUtility.getItem(AnLocale.getString("View Mode:"));
    format.add(viewModeLabel, view_fmt);

    view_expert_fmt = new AnUtility.AnRadioButton(AnLocale.getString("Expert"), false);
    view_expert_fmt.setMnemonic(AnLocale.getString('x', "MNEM_PRESENT_EXPERT"));
    view_expert_fmt.addItemListener(this);
    group.add(view_expert_fmt);
    format.add((JComponent) null, view_expert_fmt);

    view_machine_fmt =
        new AnUtility.AnRadioButton(AnLocale.getString("Machine", "PRESENT_MACHINE"), false);
    view_machine_fmt.setMnemonic(AnLocale.getString('n', "MNEM_PRESENT_MACHINE"));
    group.add(view_machine_fmt);
    format.add((JComponent) null, view_machine_fmt);
    format.add((JComponent) null, get_space_panel());

    // Add compare mode. Don't ask IPC.
    // Just use CMP_DISABLE for now, it will get set correctly after loading experiments
    boolean enbl = false;
    cmpModeLabel = get_JLabel(AnLocale.getString("Comparison Style:"));
    cmpModeLabel.setEnabled(enbl);
    group = new ButtonGroup();
    cmp_abs_value = new AnUtility.AnRadioButton(AnLocale.getString("Absolute Values"), true);
    cmp_abs_value.setMnemonic(AnLocale.getString('B', "MNEM_PRESENT_ABS_VALUE"));
    cmp_abs_value.addItemListener(this);
    cmp_abs_value.setEnabled(enbl);
    group.add(cmp_abs_value);
    format.add(cmpModeLabel, cmp_abs_value);
    cmp_delta = new AnUtility.AnRadioButton(AnLocale.getString("Deltas"), false);
    cmp_delta.setMnemonic(AnLocale.getString('D', "MNEM_PRESENT_DELTAS"));
    cmp_delta.addItemListener(this);
    cmp_delta.setEnabled(enbl);
    group.add(cmp_delta);
    format.add((JComponent) null, cmp_delta);
    cmp_ratio = new AnUtility.AnRadioButton(AnLocale.getString("Ratios"), false);
    cmp_ratio.setMnemonic(AnLocale.getString('R', "MNEM_PRESENT_RATIOS"));
    cmp_ratio.addItemListener(this);
    cmp_ratio.setEnabled(enbl);
    group.add(cmp_ratio);
    format.add((JComponent) null, cmp_ratio);

    // Wrap long metric names in table headers
    format.add((JComponent) null, get_space_panel());
    JLabel wrapNameLabel =
        get_JLabel(AnLocale.getString("Wrap Long Metric Names in Table Headers:"));
    char mnChar = AnLocale.getString('W', "MNEM_PRESENT_WRAP_NAME");
    wrapNameLabel.setDisplayedMnemonic(mnChar);
    wrapNameCheckBox = new AnUtility.AnCheckBox(Blank);
    wrapNameLabel.setLabelFor(wrapNameCheckBox);
    wrapNameCheckBox.addItemListener(this);
    format.add(wrapNameLabel, wrapNameCheckBox);
  }

  // Initialize components for the Timeline pane
  private void edt_initTimeline(final JPanel timeline_panel) {
    final JPanel border_panel;
    final JPanel center_panel;
    final JPanel east_panel;
    JLabel label;
    ButtonGroup buttonGroup;
    String txt;

    // --- border_panel (has border) contains main_panel
    border_panel = new AnJPanel(new BorderLayout());
    border_panel.setBorder(AnVariable.boxBorder);
    timeline_panel.add(border_panel);

    // --- Center
    center_panel = new AnJPanel();
    center_panel.setLayout(new BoxLayout(center_panel, BoxLayout.Y_AXIS));

    // --- Center: Data Types Panels
    txt = AnLocale.getString("Data Types:");
    label = new JLabel(txt);
    AnUtility.setAccessibleContext(label.getAccessibleContext(), txt);
    label.setBorder(AnVariable.boxBorder);
    center_panel.add(label);

    tl_type_panel = new AnJPanel();
    tl_type_panel.setLayout(new BoxLayout(tl_type_panel, BoxLayout.Y_AXIS));
    tl_type_panel.setBorder(AnVariable.boxBorder);
    center_panel.add(tl_type_panel);

    // --- Center: Additional Graphs Panels
    txt = AnLocale.getString("For enabled Data Types, also show:  ");
    label = new JLabel(txt);
    AnUtility.setAccessibleContext(label.getAccessibleContext(), txt);
    label.setBorder(AnVariable.boxBorder);
    center_panel.add(label);

    final JPanel center_additional_graph_panel = new AnJPanel();
    center_additional_graph_panel.setLayout(
        new BoxLayout(center_additional_graph_panel, BoxLayout.Y_AXIS));
    center_additional_graph_panel.setBorder(AnVariable.boxBorder);
    {
      JCheckBox check;

      txt = AnLocale.getString("Event States");
      check = new AnUtility.AnCheckBox(txt, true); // YXXX add this to .er.rc
      check.addItemListener(this);
      center_additional_graph_panel.add(check);
      tldata_show_states_cb = check;
      tldata_show_states = tldata_show_states_cb.isSelected(); // Keep states in sync....

      txt = AnLocale.getString("Event Density");
      check = new AnUtility.AnCheckBox(txt, false); // YXXX add this to .er.rc
      check.addItemListener(this);
      center_additional_graph_panel.add(check);
      tldata_show_counts_cb = check;
      tldata_show_counts = tldata_show_counts_cb.isSelected(); // Keep states in sync....
    }
    center_panel.add(center_additional_graph_panel);

    // --- East
    east_panel = new AnJPanel();
    east_panel.setLayout(new BoxLayout(east_panel, BoxLayout.Y_AXIS));
    east_panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

    // --- East, 1st row: tl_entity
    final JPanel east_entity_panel = new AnJPanel(new BorderLayout(4, 4));
    east_entity_panel.setBorder(AnVariable.boxBorder);

    txt = AnLocale.getString("Group Data by:");
    label = new JLabel(txt);
    AnUtility.setAccessibleContext(label.getAccessibleContext(), txt);
    east_entity_panel.add(label, BorderLayout.NORTH);

    final JPanel entity_button_panel = new AnJPanel(new GridLayout(1, 3));

    buttonGroup = new ButtonGroup();
    tl_entity_button = new ArrayList<JRadioButton>();
    int tl_entity_selected_btn = getTimelineSetting().getTl_entity_selected_btn();
    TimelineSetting.EntityProp entProp;
    for (int i = 0; null != (entProp = getTimelineSetting().getTl_entity_prop(i)); i++) {
      String uname = entProp.prop_uname;
      JRadioButton thisButton = new AnRadioButton(uname, i == tl_entity_selected_btn);
      thisButton.setOpaque(false);
      thisButton.addItemListener(this);
      tl_entity_button.add(thisButton);
      buttonGroup.add(thisButton);
      entity_button_panel.add(thisButton);
    }

    east_entity_panel.add(entity_button_panel, BorderLayout.CENTER);
    east_panel.add(east_entity_panel);
    east_panel.add(new JSeparator(SwingConstants.HORIZONTAL));

    // --- East, 2nd row: Stack alignment
    final JPanel east_stack_align_panel = new AnJPanel(new BorderLayout(4, 4));
    east_stack_align_panel.setBorder(AnVariable.boxBorder);

    txt = AnLocale.getString("Stack Alignment:");
    label = new JLabel(txt);
    AnUtility.setAccessibleContext(label.getAccessibleContext(), txt);
    east_stack_align_panel.add(label, BorderLayout.NORTH);

    final JPanel alignment_button_panel = new AnJPanel(new GridLayout(1, 2));

    buttonGroup = new ButtonGroup();
    tl_stack_align = new AnRadioButton[2];

    tl_stack_align[0] =
        new AnRadioButton(
            TLStack_align.TLSTACK_ALIGN_ROOT.toString(),
            getTimelineSetting().getStack_align() == TLStack_align.TLSTACK_ALIGN_ROOT);
    tl_stack_align[0].setMnemonic(AnLocale.getString('R', "MNEM_PRESENT_ROOT"));
    tl_stack_align[0].addItemListener(this);
    tl_stack_align[0].setOpaque(false);
    buttonGroup.add(tl_stack_align[0]);
    alignment_button_panel.add(tl_stack_align[0]);

    tl_stack_align[1] =
        new AnRadioButton(
            TLStack_align.TLSTACK_ALIGN_LEAF.toString(),
            getTimelineSetting().getStack_align() == TLStack_align.TLSTACK_ALIGN_LEAF);
    tl_stack_align[1].setMnemonic(AnLocale.getString('f', "MNEM_PRESENT_LEAF"));
    tl_stack_align[1].addItemListener(this);
    tl_stack_align[1].setOpaque(false);
    buttonGroup.add(tl_stack_align[1]);
    alignment_button_panel.add(tl_stack_align[1]);

    east_stack_align_panel.add(alignment_button_panel, BorderLayout.CENTER);
    east_panel.add(east_stack_align_panel);

    east_panel.add(new JSeparator(SwingConstants.HORIZONTAL));

    // --- East, 3nd row: tl_stack_depth
    final JPanel east_stack_depth_panel = new AnJPanel(new BorderLayout(4, 4));
    east_stack_depth_panel.setBorder(AnVariable.boxBorder);

    txt = AnLocale.getString("Call Stack Magnification Level");
    label =
        new JLabel(
            AnLocale.getString("Call Stack Magnification: ")
                + getTimelineSetting().getTLStackFramePixels());
    AnUtility.setAccessibleContext(label.getAccessibleContext(), txt);
    label.setDisplayedMnemonic(AnLocale.getString('C', "MNEM_CallStackMagnification"));
    label.setAlignmentX(JDialog.LEFT_ALIGNMENT);
    east_stack_depth_panel.add(label, BorderLayout.NORTH);

    tl_slider = new JSlider();
    tl_slider.setOpaque(false);
    //        tl_slider.setMinimum(0);
    //        tl_slider.setMaximum(TIMELINE_MAX_STACK_DEPTH);
    tl_slider.setMinimum(TIMELINE_STACK_FRAME_MAGNIFY_MIN);
    tl_slider.setMaximum(TIMELINE_STACK_FRAME_MAGNIFY_MAX);
    //        tl_slider.setValue(getTimelineSetting().getTLStackDepth());
    tl_slider.setValue(getTimelineSetting().getTLStackFramePixels());
    tl_slider.setPaintLabels(true);
    tl_slider.setPaintTicks(true);
    tl_slider.setPaintTrack(true);
    tl_slider.setSnapToTicks(false);
    tl_slider.setMinorTickSpacing(1);
    //        tl_slider.setMajorTickSpacing(9);
    tl_slider.addChangeListener(new SliderLabelListener(label));
    east_stack_depth_panel.add(tl_slider, BorderLayout.CENTER);

    label.setLabelFor(tl_slider);

    east_panel.add(east_stack_depth_panel);

    // add components to top-level panel
    border_panel.add(center_panel, BorderLayout.CENTER);
    border_panel.add(east_panel, BorderLayout.EAST);
  }

  /** Called every time experiments are loaded */
  private void setViewComponents() {
    createTabSelectionPanel();
    settingsDialog.setButtonsEnabled(false);
  }

  // Set components in the Tab selection panel
  private void createTabSelectionPanel() {
    //        System.out.println("Settings.createTabSelectionPanel");
    viewsPanel.removeAll();

    // Left Panel
    AnList viewsList = new AnList(false);
    buildViewsList(viewsList);
    GridBagConstraints gridBagConstraints;
    JPanel standardPanel = new AnJPanel();
    standardPanel.setLayout(new GridBagLayout());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    JLabel standardLabel = new JLabel(AnLocale.getString("Standard Views"));
    standardLabel.getAccessibleContext().setAccessibleDescription(standardLabel.getText());
    standardLabel.getAccessibleContext().setAccessibleName(standardLabel.getText());
    standardLabel.setBackground(new Color(225, 220, 220));
    standardLabel.setOpaque(true);
    standardLabel.setHorizontalAlignment(JLabel.CENTER);
    standardLabel.setFont(standardLabel.getFont().deriveFont(Font.BOLD));
    standardLabel.setDisplayedMnemonic(AnLocale.getString('S', "SettingsViewsStandardLabelMN"));
    gridBagConstraints.insets = new Insets(3, 3, 0, 3);
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    standardPanel.add(standardLabel, gridBagConstraints);
    standardPanel.setBorder(
        BorderFactory.createMatteBorder(1, 1, 1, 1, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    AnList stdlist = new AnList(false);
    buildStdTabList(stdlist);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    standardPanel.add(stdlist, gridBagConstraints);
    if (standardObjectCheckBoxes.length > 0) {
      standardLabel.setLabelFor(standardObjectCheckBoxes[0]);
    }
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(6, 4, 6, 4);
    viewsPanel.add(standardPanel, gridBagConstraints);

    // Center Panel
    indxlist = new AnList(false);
    buildIndxTabList();
    JPanel indexObjectPanel = new AnJPanel();
    indexObjectPanel.setLayout(new GridBagLayout());
    JLabel indexObjectLabel = new JLabel(AnLocale.getString("Index Object Views"));
    indexObjectLabel.getAccessibleContext().setAccessibleDescription(standardLabel.getText());
    indexObjectLabel.getAccessibleContext().setAccessibleName(standardLabel.getText());
    indexObjectLabel.setBackground(new Color(225, 225, 225));
    indexObjectLabel.setOpaque(true);
    indexObjectLabel.setHorizontalAlignment(JLabel.CENTER);
    indexObjectLabel.setFont(indexObjectLabel.getFont().deriveFont(Font.BOLD));
    indexObjectLabel.setDisplayedMnemonic(AnLocale.getString('w', "SettingsViewsIndexLabelMN"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(3, 3, 0, 3);
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    indexObjectPanel.add(indexObjectLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    indexObjectPanel.add(indxlist, gridBagConstraints);
    indexObjectPanel.setBorder(
        BorderFactory.createMatteBorder(1, 1, 1, 1, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    if (indexObjectCheckBoxes.length > 0) {
      indexObjectLabel.setLabelFor(indexObjectCheckBoxes[0]);
    }
    // Custom button
    AnUtility.ResponseAction addCustomIViewButton =
        new AnUtility.ResponseAction(
            AnLocale.getString("Add Custom View"),
            null,
            AnLocale.getString('u', "MNEM_PRESENT_ADD_CUSTOM_INDXOBJECT"),
            this);
    addCustomIViewButton.setToolTipText(AnLocale.getString("Add Index Object Custom View"));
    //        if (GUITesting.getInstance().isRunningUnderGUITesting()) {
    //            addCustomIViewButton.setText("Add Custom Index Objects View");
    //        }
    addCustomIViewButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (tabs_custom_indx_dlg == null) {
              tabs_custom_indx_dlg = new CustomIndxDialog(0, frame);
            }
            tabs_custom_indx_dlg.setVisible(true);
          }
        });
    addCustomIViewButton.setMargin(new Insets(0, 4, 0, 4));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(4, 4, 4, 4);
    indexObjectPanel.add(addCustomIViewButton, gridBagConstraints);
    // Add the panel
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new Insets(6, 4, 6, 4);
    viewsPanel.add(indexObjectPanel, gridBagConstraints);

    // Right Panel
    memlist = new AnList(false);
    buildMemTabList();
    JPanel memoryObjectPanel = new AnJPanel();
    memoryObjectPanel.setBorder(
        BorderFactory.createMatteBorder(1, 1, 1, 1, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    memoryObjectPanel.setLayout(new GridBagLayout());
    JLabel memoryObjectLabel = new JLabel(AnLocale.getString("Memory Object Views"));
    memoryObjectLabel.getAccessibleContext().setAccessibleDescription(standardLabel.getText());
    memoryObjectLabel.getAccessibleContext().setAccessibleName(standardLabel.getText());
    memoryObjectLabel.setBackground(new Color(225, 225, 225));
    memoryObjectLabel.setOpaque(true);
    memoryObjectLabel.setHorizontalAlignment(JLabel.CENTER);
    memoryObjectLabel.setFont(memoryObjectLabel.getFont().deriveFont(Font.BOLD));
    memoryObjectLabel.setDisplayedMnemonic(AnLocale.getString('y', "SettingsViewsMemoryLabelMN"));
    if (memoryObjectCheckBoxes.length > 0) {
      memoryObjectLabel.setLabelFor(memoryObjectCheckBoxes[0]);
    }
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(3, 3, 0, 3);
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    memoryObjectPanel.add(memoryObjectLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    memoryObjectPanel.add(memlist, gridBagConstraints);
    // Custom button
    AnUtility.ResponseAction customMemoryObjectButton =
        new AnUtility.ResponseAction(
            AnLocale.getString("Add Custom View"),
            null,
            AnLocale.getString('m', "MNEM_PRESENT_ADD_CUSTOM_MEMOBJECT"),
            this);
    customMemoryObjectButton.setToolTipText(AnLocale.getString("Add Memory Object Custom View"));
    //        if (GUITesting.getInstance().isRunningUnderGUITesting()) {
    //            customMemoryObjectButton.setText("Add Custom Memory Objects View");
    //        }
    customMemoryObjectButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (tabs_custom_mem_dlg == null) {
              tabs_custom_mem_dlg = new CustomMemDialog(0, frame);
            }
            tabs_custom_mem_dlg.setVisible(true);
          }
        });
    customMemoryObjectButton.setMargin(new Insets(0, 4, 0, 4));
    customMemoryObjectButton.setAlignmentX(JButton.RIGHT_ALIGNMENT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    //        gridBagConstraints.anchor = GridBagConstraints.NORTH;
    gridBagConstraints.insets = new Insets(4, 4, 0, 4);
    memoryObjectPanel.add(customMemoryObjectButton, gridBagConstraints);
    JLabel machineModelLabel = new JLabel();
    AnUtility.setTextAndAccessibleContext(machineModelLabel, AnLocale.getString("Machine Model:"));
    machineModelLabel.setDisplayedMnemonic(
        AnLocale.getString('c', "MN_SETTINGS_VIEWS_Machine_Model"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.insets = new Insets(4, 4, 0, 4);
    memoryObjectPanel.add(machineModelLabel, gridBagConstraints);
    final JComboBox machineModelComboBox = new JComboBox();
    machineModelLabel.setLabelFor(machineModelComboBox);
    AnUtility.setAccessibleContext(
        machineModelComboBox.getAccessibleContext(), AnLocale.getString("Machine Models"));
    // add an emptry string
    machineModelComboBox.addItem(AnLocale.getString("(none)"));
    // loop over list from DBE
    String[] choices = getViewsSetting().getAvailableMachineMsodel();
    for (int j = 0; j < choices.length; j++) {
      machineModelComboBox.addItem(choices[j]);
    }
    machineModelComboBox.setSelectedIndex(0);
    if (!((getViewsSetting().getMachineModel() == null)
        || (getViewsSetting().getMachineModel().equals("")))) {
      for (int i = 0; i < machineModelComboBox.getItemCount(); i++) {
        String mn = (String) machineModelComboBox.getItemAt(i);
        if (mn.equalsIgnoreCase(getViewsSetting().getMachineModel())) {
          machineModelComboBox.setSelectedIndex(i);
          break;
        }
      }
    }
    machineModelComboBox.addPopupMenuListener(
        new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            String machineModelName = (String) machineModelComboBox.getSelectedItem();
            setMachineModel(machineModelComboBox, machineModelName);
          }

          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    memoryObjectPanel.add(machineModelComboBox, gridBagConstraints);

    // Align button widths
    int maxWidth = customMemoryObjectButton.getPreferredSize().width;
    if (machineModelLabel.getPreferredSize().width > maxWidth) {
      maxWidth = machineModelLabel.getPreferredSize().width;
    }
    if (machineModelComboBox.getPreferredSize().width > maxWidth) {
      maxWidth = machineModelComboBox.getPreferredSize().width;
    }
    Dimension dim = new Dimension(maxWidth, customMemoryObjectButton.getPreferredSize().height);
    customMemoryObjectButton.setPreferredSize(dim);
    machineModelLabel.setPreferredSize(dim);
    machineModelComboBox.setPreferredSize(dim);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new Insets(6, 4, 6, 4);
    viewsPanel.add(memoryObjectPanel, gridBagConstraints);

    // Align section widths
    maxWidth = standardLabel.getPreferredSize().width;
    if (indexObjectLabel.getPreferredSize().width > maxWidth) {
      maxWidth = indexObjectLabel.getPreferredSize().width;
    }
    if (memoryObjectLabel.getPreferredSize().width > maxWidth) {
      maxWidth = memoryObjectLabel.getPreferredSize().width;
    }
    dim = new Dimension(maxWidth + 30, standardLabel.getPreferredSize().height);
    standardLabel.setPreferredSize(dim);
    indexObjectLabel.setPreferredSize(dim);
    memoryObjectLabel.setPreferredSize(dim);

    // Sidepanel
    viewsPanel.validate(); // FIXUP ?
    viewsPanel.repaint(); // FIXUP ?
  }

  private void setMachineModel(final JComboBox comboBox, String machineModelName) {
    if ((machineModelName == null) || (machineModelName.equals(AnLocale.getString("(none)")))) {
      machineModelName = "";
    }
    if (machineModelName.equals(getViewsSetting().getMachineModel())) {
      return;
    }
    final String machineModelName2 = machineModelName;
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            final String errorMessage = ViewsSetting.loadMachineModelIPC(machineModelName2);
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  @Override
                  public void run() {
                    if (errorMessage == null) { // Successfully loaded machine model, update gui
                      addCustomIndxMemTab();
                      //                            anWindow.getViewPanels().removeAllViews(); // if
                      // loading new machine model, the old machine model views should be removed
                      settingsDialog.setButtonsEnabled(true);
                      getViewsSetting().setMachineModel(machineModelName2);
                    } else {
                      AnUtility.showMessage(viewsPanel, errorMessage, JOptionPane.ERROR_MESSAGE);
                      String machineModelName3 = getViewsSetting().getMachineModel();
                      if ((machineModelName3 == null) || (machineModelName3.equals(""))) {
                        machineModelName3 = AnLocale.getString("(none)");
                      }
                      for (int i = 0; i < comboBox.getItemCount(); i++) {
                        String mn = (String) comboBox.getItemAt(i);
                        if (mn.equalsIgnoreCase(machineModelName3)) {
                          comboBox.setSelectedIndex(i);
                          break;
                        }
                      }
                    }
                  }
                });
          }
        },
        "machinemodelcomboboxupdate");
  }

  // Build the list of checkboxes for the Views
  private void buildViewsList(AnList list) {
    //        System.out.println("Settings.buildViewsList");
    JPanel chk_panel;
    String[] tab_names;
    char[] tab_mnems;

    // Add column header
    chk_panel = new AnJPanel(new GridLayout(1, 1));
    list.add(AnUtility.getItem(AnLocale.getString("Views")), chk_panel);
    list.getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("List of views to choose from"));

    list.add((JComponent) null, new AnJPanel());
    final JPanel sep_panel = new AnJPanel(new GridLayout(1, 1));
    sep_panel.add(new JSeparator());
    list.add((JComponent) null, sep_panel);

    List<ViewsSetting.View> views_tablist = getViewsSetting().getStaticViews();
    tab_names = new String[views_tablist.size()];
    tab_mnems = new char[views_tablist.size()];
    for (int i = 0; i < views_tablist.size(); i++) {
      tab_names[i] = views_tablist.get(i).getAnDispTab().getTName();
      tab_mnems[i] = views_tablist.get(i).getAnDispTab().getTMnem();
    }

    // Compose the list of checkboxes
    staticViewCheckBoxes = new AnUtility.AnCheckBox[tab_names.length];

    // Compose standard tabs checklist
    for (int i = 0; i < staticViewCheckBoxes.length; i++) {
      chk_panel = new AnJPanel(new GridLayout(1, 2));
      chk_panel.add(staticViewCheckBoxes[i] = new AnUtility.AnCheckBox(" ", true)); // <=== FIXUP
      ((AnUtility.AnCheckBox) staticViewCheckBoxes[i]).addItemListener(this);
      JLabel itemlabel = (JLabel) AnUtility.getItem(tab_names[i]);
      //            itemlabel.setDisplayedMnemonic(tab_mnems[i]);
      itemlabel.setLabelFor(staticViewCheckBoxes[i]);
      AnUtility.setAccessibleContext(itemlabel.getAccessibleContext(), tab_names[i]);
      list.add(itemlabel, chk_panel);
    }

    // Sidepanel
    if (views_tablist != null) {
      for (ViewsSetting.View view : views_tablist) {
        anWindow.getViewsPanel().createView(anWindow, view.getAnDispTab(), View.Type.STATIC_VIEW);
      }
    }
  }

  // Build the list of checkboxes for the standard tabs
  private void buildStdTabList(AnList list) {
    JPanel chk_panel;
    String[] tab_names;
    char[] tab_mnems;
    JLabel itemlabel;

    // Add column header
    chk_panel = new AnJPanel();
    list.getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("List of Views to choose from"));
    List<ViewsSetting.View> standard_tablist = getViewsSetting().getStandardViews();

    tab_names = new String[standard_tablist.size()];
    tab_mnems = new char[standard_tablist.size()];
    for (int i = 0; i < standard_tablist.size(); i++) {
      tab_names[i] = standard_tablist.get(i).getAnDispTab().getTName();
      tab_mnems[i] = standard_tablist.get(i).getAnDispTab().getTMnem();
    }

    if (tab_names == null) {
      return;
    }

    // Compose the list of checkboxes
    standardObjectCheckBoxes = new AnUtility.AnCheckBox[tab_names.length];

    // Compose standard tabs checklist
    for (int i = 0; i < standardObjectCheckBoxes.length; i++) {
      ViewsSetting.View view = standard_tablist.get(i);
      chk_panel = new AnJPanel(new GridLayout(1, 2));
      chk_panel.add(standardObjectCheckBoxes[i] = new AnUtility.AnCheckBox(" ", view.isSelected()));
      ((AnUtility.AnCheckBox) standardObjectCheckBoxes[i]).addItemListener(this);
      itemlabel = (JLabel) AnUtility.getItem(tab_names[i]);
      AnUtility.setAccessibleContext(itemlabel.getAccessibleContext(), tab_names[i]);
      AnUtility.setAccessibleContext(
          standardObjectCheckBoxes[i].getAccessibleContext(), tab_names[i]);
      list.add(itemlabel, chk_panel);
    }

    // Sidepanel
    if (standard_tablist != null) {
      for (ViewsSetting.View view : standard_tablist) {
        anWindow.getViewsPanel().createView(anWindow, view.getAnDispTab(), View.Type.STANDARD);
      }
    }
  }

  // Build the list of checkboxes for the memory object tabs
  private void buildMemTabList() {
    JLabel memlabel;

    memlist.removeAll();

    // Add column header
    memchk_panel = new AnJPanel(new GridLayout(1, 1));
    memlist
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("List of memory object Views to choose from"));
    List<ViewsSetting.View> mem_tablist = getViewsSetting().getMemoryViews();

    // Compose memory object tabs checklist
    memoryObjectCheckBoxes = new AnUtility.AnCheckBox[mem_tablist.size()];

    for (int i = 0; i < memoryObjectCheckBoxes.length; i++) {
      ViewsSetting.View view = mem_tablist.get(i);
      memchk_panel = new AnJPanel(new GridLayout(1, 2));
      memchk_panel.add(
          memoryObjectCheckBoxes[i] = new AnUtility.AnCheckBox(" ", view.isSelected()));
      memoryObjectCheckBoxes[i].addItemListener(this);
      memlabel = (JLabel) AnUtility.getItem(view.getAnDispTab().getTName());
      AnUtility.setAccessibleContext(
          memlabel.getAccessibleContext(), view.getAnDispTab().getTName());
      AnUtility.setAccessibleContext(
          memoryObjectCheckBoxes[i].getAccessibleContext(), view.getAnDispTab().getTName());
      memlist.add(memlabel, memchk_panel);
    }
    memlist.revalidate();

    // Sidepanel
    if (mem_tablist != null) {
      for (ViewsSetting.View view : mem_tablist) {
        anWindow.getViewsPanel().createView(anWindow, view.getAnDispTab(), View.Type.MEMORY);
      }
    }
  }

  // Build the list of checkboxes for the index object tabs
  private void buildIndxTabList() {
    JLabel indxlabel;

    indxlist.removeAll();

    // Add column header
    indxchk_panel = new AnJPanel(new GridLayout(1, 1));
    indxlist
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("List of index object Vews to choose from"));
    List<ViewsSetting.View> indx_tablist = getViewsSetting().getIndexViews();

    // Compose index object tabs checklist
    indexObjectCheckBoxes = new AnUtility.AnCheckBox[indx_tablist.size()];

    for (int i = 0; i < indexObjectCheckBoxes.length; i++) {
      ViewsSetting.View view = indx_tablist.get(i);
      indxchk_panel = new AnJPanel(new GridLayout(1, 2));
      indxchk_panel.add(
          indexObjectCheckBoxes[i] = new AnUtility.AnCheckBox(" ", view.isSelected()));
      indexObjectCheckBoxes[i].addItemListener(this);
      indxlabel = (JLabel) AnUtility.getItem(view.getAnDispTab().getTName());
      AnUtility.setAccessibleContext(
          indxlabel.getAccessibleContext(), view.getAnDispTab().getTName());
      AnUtility.setAccessibleContext(
          indexObjectCheckBoxes[i].getAccessibleContext(), view.getAnDispTab().getTName());
      indxlist.add(indxlabel, indxchk_panel);
    }
    indxlist.revalidate();

    // Sidepanel
    if (indx_tablist != null) {
      for (ViewsSetting.View view : indx_tablist) {
        anWindow.getViewsPanel().createView(anWindow, view.getAnDispTab(), View.Type.INDEX);
      }
    }
  }

  private void updateTLDataCheckBoxes() {
    tl_type_panel.removeAll();
    tldata_checkBoxes = new ArrayList(); // Does listener result in leak?
    List<String> tldata_unames = timelineSetting.getTLDataUNames();
    long tldata_check_hidden_bitmask = timelineSetting.getTLDataHiddenMask();
    for (int i = 0; i < tldata_unames.size(); i++) {
      long hidden = ((1L << i) & tldata_check_hidden_bitmask);
      JCheckBox check = new AnUtility.AnCheckBox(tldata_unames.get(i), (hidden == 0));
      check.addItemListener(this);
      tl_type_panel.add(check);
      tldata_checkBoxes.add(check);
    }
    tl_type_panel.revalidate();
  }

  // Update show source with/without metrics in disassembly display
  public void itemStateChanged(final ItemEvent event) {
    final Object source = event.getItemSelectable();

    if (source == dis_src) {
      if (event.getStateChange() == ItemEvent.DESELECTED) {
        dis_asrc.setSelected(false);
      }
    } else if (source == dis_asrc) {
      if (event.getStateChange() == ItemEvent.SELECTED) {
        dis_src.setSelected(true);
      }
    }
    if (settingsDialog != null) {
      settingsDialog.setButtonsEnabled(true);
    }
  }

  // Action performed
  public void actionPerformed(final ActionEvent event) {
    final String cmd = event.getActionCommand();

    if (cmd.equals(AnLocale.getString("OK")) || cmd.equals(AnLocale.getString("Apply"))) {
      apply();
    } else if (cmd.equals(AnLocale.getString("Export..."))) {
      new ExportSettingsDialog(frame).setVisible(true);
    } else if (cmd.equals(AnLocale.getString("Import..."))) {
      new ImportSettingsDialog(frame).setVisible(true);
    }
  }

  private void updateMemTab() {
    Object[] memObjs = ViewsSetting.getMemObjectsIPC(); // IPC
    boolean[] memSelected = ViewsSetting.getMemTabSelectionStateIPC(); // IPC
    // Copy the check box states (may not yet be saved)
    for (int i = 0; i < memoryObjectCheckBoxes.length; i++) {
      JCheckBox checkBox = memoryObjectCheckBoxes[i];
      if (i < memSelected.length) {
        memSelected[i] = checkBox.isSelected();
      }
    }
    getViewsSetting().updateMemoryObjects(null, memObjs, memSelected);
  }

  private void addCustomMemTab() {
    //        System.out.println("Settings.addCustomMemTab");
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            updateMemTab();
          }
        },
        "custommemtab");
  }

  // Dialog to add custom memory object tab
  private class CustomMemDialog extends AnDialog {

    JTextField obj_name;
    AnUtility.AnTextArea formula_field;

    public CustomMemDialog(int win_id, final Frame frame) {
      super(
          anWindow,
          frame,
          AnLocale.getString("Add Memory Object"),
          false,
          null,
          null,
          AnVariable.HELP_CustomMemTab,
          true);
      initComponents();
    }

    // Initialize GUI components
    private void initComponents() {
      setAccessory(setCustomPanel());
      setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    }

    // Action performed
    @Override
    public void actionPerformed(final ActionEvent event) {
      final String cmd = event.getActionCommand();
      if (cmd.equals(AnLocale.getString("OK")) || cmd.equals(AnLocale.getString("Apply"))) {
        String formula = formula_field.getText();
        String formula_str = formula.replaceAll("\n", " ");
        AnUtility.checkIPCOnWrongThread(false);
        String sts =
            ViewsSetting.defineMemObjIPC(
                obj_name.getText(), formula_str, null, null); // IPC // CXXX add descriptions
        AnUtility.checkIPCOnWrongThread(true);
        if (sts == null) { // Successfully added new object, repaint mem checkboxes
          // Clear the text fields
          getViewsSetting()
              .addCustomMemoryObject(
                  obj_name.getText(), formula_str, null, null); // CXXX add descriptions
          formula_field.setText("");
          obj_name.setText("");
          addCustomMemTab();
          settingsDialog.setButtonsEnabled(true);
        } else {
          // Don't clear the text fields, so the user can correct an error
          AnUtility.showMessage(tabs_custom_mem_dlg, sts, JOptionPane.ERROR_MESSAGE);
        }
      }
    }

    private JPanel setCustomPanel() {
      JPanel basePanel, namePanel, formulaPanel;
      JLabel name_label, formula_label;

      basePanel = new AnJPanel(new GridBagLayout());
      //            basePanel.setBorder(AnVariable.fieldBorder);

      // Add name panel
      namePanel = new AnJPanel();
      namePanel.setLayout(new BorderLayout());
      namePanel.setBorder(AnVariable.fieldBorder);
      name_label = (JLabel) AnUtility.getTitle(AnLocale.getString("Object Name:"));
      name_label.setDisplayedMnemonic(AnLocale.getString('O', "MNEM_CUSTOMOBJ_NAME"));
      namePanel.add(name_label, BorderLayout.NORTH);
      obj_name = new JTextField(15);
      name_label.setLabelFor(obj_name);
      AnUtility.setAccessibleContext(obj_name.getAccessibleContext(), name_label.getText());
      namePanel.add(obj_name, BorderLayout.CENTER);

      // Add formula panel
      formulaPanel = new AnJPanel();
      formulaPanel.setLayout(new BorderLayout());
      formulaPanel.setBorder(AnVariable.fieldBorder);
      formula_label = (JLabel) AnUtility.getTitle(AnLocale.getString("Formula:"));
      formula_label.setDisplayedMnemonic(AnLocale.getString('F', "MNEM_CUSTOMOBJ_FORMULA"));
      formulaPanel.add(formula_label, BorderLayout.NORTH);
      formula_field = new AnUtility.AnTextArea("", true);
      AnUtility.setAccessibleContext(formula_field.getAccessibleContext(), formula_label.getText());
      formula_field.setLineWrap(true);
      formula_field.setWrapStyleWord(true);
      // formula_field.setFont(CollectUtility.text_font);
      JScrollPane form_scroll =
          new AnJScrollPane(
              formula_field,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      form_scroll.setPreferredSize(new Dimension(getPreferredSize().width, 120));
      form_scroll.setBorder(BorderFactory.createLineBorder(AnEnvironment.SPLIT_PANE_BORDER_COLOR));
      formula_label.setLabelFor(formula_field);
      formulaPanel.add(form_scroll, BorderLayout.CENTER);

      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      //            gridBagConstraints.insets = new Insets(8, 8, 0, 8);
      basePanel.add(namePanel, gridBagConstraints);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      //            gridBagConstraints.insets = new Insets(0, 8, 0, 8);
      basePanel.add(formulaPanel, gridBagConstraints);
      return basePanel;
    }
  } // class CustomMemDialog

  private void updateIndxTab() {
    Object[] indxObjs = ViewsSetting.getIndxObjDescriptionsIPC(); // IPC
    boolean[] indxSelected = ViewsSetting.getIndxTabSelectionStateIPC(); // IPC
    // Copy the check box states (may not yet be saved)
    for (int i = 0; i < indexObjectCheckBoxes.length; i++) {
      JCheckBox checkBox = indexObjectCheckBoxes[i];
      if (i < indxSelected.length) {
        indxSelected[i] = checkBox.isSelected();
      }
    }
    getViewsSetting().updateIndexObjects(null, indxObjs, indxSelected); // Will fire change event
  }

  private void addCustomIndxTab() {
    //        System.out.println("Settings.addCustomIndxTab");
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            updateIndxTab();
          }
        },
        "customindxtab");
  }

  private void updateIndxMemTab() {
    Object[] indxObjs = ViewsSetting.getIndxObjDescriptionsIPC(); // IPC
    boolean[] indxSelected = ViewsSetting.getIndxTabSelectionStateIPC(); // IPC
    // Copy the check box states (may not yet be saved)
    for (int i = 0; i < indexObjectCheckBoxes.length; i++) {
      JCheckBox checkBox = indexObjectCheckBoxes[i];
      if (i < indxSelected.length) {
        indxSelected[i] = checkBox.isSelected();
      }
    }

    Object[] memObjs = ViewsSetting.getMemObjectsIPC(); // IPC
    boolean[] memSelected = ViewsSetting.getMemTabSelectionStateIPC(); // IPC
    // Copy the check box states (may not yet be saved)
    for (int i = 0; i < memoryObjectCheckBoxes.length; i++) {
      JCheckBox checkBox = memoryObjectCheckBoxes[i];
      if (i < memSelected.length) {
        memSelected[i] = checkBox.isSelected();
      }
    }

    getViewsSetting().updateIndexMemoryObjects(null, indxObjs, indxSelected, memObjs, memSelected);
  }

  private void addCustomIndxMemTab() {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            updateIndxMemTab();
          }
        },
        "customindxmemtab");
  }

  // Dialog to add custom index object tab
  private class CustomIndxDialog extends AnDialog {

    JTextField obj_name;
    AnUtility.AnTextArea formula_field;

    public CustomIndxDialog(int win_id, final Frame frame) {
      super(
          anWindow,
          frame,
          AnLocale.getString("Add Index Object"),
          false,
          null,
          null,
          AnVariable.HELP_CustomIndxTab,
          true);
      initComponents();
    }

    // Initialize GUI components
    private void initComponents() {
      setAccessory(setCustomPanel());
      setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    }

    // Action performed
    @Override
    public void actionPerformed(final ActionEvent event) {
      final String cmd = event.getActionCommand();
      if (cmd.equals(AnLocale.getString("OK")) || cmd.equals(AnLocale.getString("Apply"))) {
        String formula = formula_field.getText();
        String formula_str = formula.replaceAll("\n", " ");
        AnUtility.checkIPCOnWrongThread(false);
        String sts =
            ViewsSetting.defineIndxObjIPC(
                obj_name.getText(), formula_str, null, null); // IPC // CXXX add descriptions
        AnUtility.checkIPCOnWrongThread(true);
        if (sts == null) { // Successfully added new object, repaint indx checkboxes
          getViewsSetting()
              .addCustomIndexObject(
                  obj_name.getText(), formula_str, null, null); // CXXX add descriptions
          addCustomIndxTab();
          // clear the fields for the next definition
          formula_field.setText("");
          obj_name.setText("");
          settingsDialog.setButtonsEnabled(true);
        } else {
          AnUtility.showMessage(tabs_custom_indx_dlg, sts, JOptionPane.ERROR_MESSAGE);
        }
      }
    }

    private JPanel setCustomPanel() {
      JPanel basePanel, namePanel, formulaPanel;
      JLabel name_label, formula_label;

      basePanel = new AnJPanel(new GridBagLayout());
      //            basePanel.setBorder(AnVariable.fieldBorder);

      // Add name panel
      namePanel = new AnJPanel();
      namePanel.setLayout(new BorderLayout());
      namePanel.setBorder(AnVariable.fieldBorder);
      name_label = (JLabel) AnUtility.getTitle(AnLocale.getString("Object Name:"));
      name_label.setDisplayedMnemonic(AnLocale.getString('O', "MNEM_CUSTOMOBJ_NAME"));
      namePanel.add(name_label, BorderLayout.NORTH);
      obj_name = new JTextField(15);
      name_label.setLabelFor(obj_name);
      AnUtility.setAccessibleContext(obj_name.getAccessibleContext(), name_label.getText());
      namePanel.add(obj_name, BorderLayout.CENTER);

      // Add formula panel
      formulaPanel = new AnJPanel();
      formulaPanel.setLayout(new BorderLayout());
      formulaPanel.setBorder(AnVariable.fieldBorder);
      formula_label = (JLabel) AnUtility.getTitle(AnLocale.getString("Formula:"));
      formula_label.setDisplayedMnemonic(AnLocale.getString('F', "MNEM_CUSTOMOBJ_FORMULA"));
      formulaPanel.add(formula_label, BorderLayout.NORTH);
      formula_field = new AnUtility.AnTextArea("", true);
      AnUtility.setAccessibleContext(formula_field.getAccessibleContext(), formula_label.getText());
      formula_field.setLineWrap(true);
      formula_field.setWrapStyleWord(true);
      // formula_field.setFont(CollectUtility.text_font);
      JScrollPane form_scroll =
          new AnJScrollPane(
              formula_field,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      form_scroll.setPreferredSize(new Dimension(getPreferredSize().width, 120));
      form_scroll.setBorder(BorderFactory.createLineBorder(AnEnvironment.SPLIT_PANE_BORDER_COLOR));
      formula_label.setLabelFor(formula_field);
      formulaPanel.add(form_scroll, BorderLayout.CENTER);

      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      basePanel.add(namePanel, gridBagConstraints);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      basePanel.add(formulaPanel, gridBagConstraints);
      return basePanel;
    }
  }

  private void checkTimelineChanged(Object originalSource) {
    Object newTm = getTimelineSettings(needsTimelineEvent);
    needsTimelineEvent = false;
    getTimelineSetting()
        .set(
            originalSource,
            (List) newTm,
            newTm != null); // Will fire AnChangeEvent if settings have changed
  }

  public Object edt_getTimelineOptions() {
    Object obj = getTimelineSettings(true);
    return obj;
  }

  private void checkSearchPathChanged(Object originalSource) {
    searchPathPanel.checkSearchPathChanged(originalSource);
  }

  private void checkPathMapChanged(Object originalSource) {
    //        pathMapPanel.checkPathMapChanged(originalSource);
    pathmapsPanel.checkPathMapChanged(originalSource);
  }

  private void checkCallTreeChanges(Object originalSource) {
    callTreePanel.checkChanges(originalSource);
  }

  private void checkViewsChanges(Object originalSource) {
    boolean[] values =
        new boolean
            [staticViewCheckBoxes.length
                + standardObjectCheckBoxes.length
                + indexObjectCheckBoxes.length
                + memoryObjectCheckBoxes.length];
    int index = 0;
    for (int i = 0; i < staticViewCheckBoxes.length; i++) {
      values[index++] = ((JCheckBox) staticViewCheckBoxes[i]).isSelected();
    }
    for (int i = 0; i < standardObjectCheckBoxes.length; i++) {
      values[index++] = ((JCheckBox) standardObjectCheckBoxes[i]).isSelected();
    }
    for (int i = 0; i < indexObjectCheckBoxes.length; i++) {
      values[index++] = ((JCheckBox) indexObjectCheckBoxes[i]).isSelected();
    }
    for (int i = 0; i < memoryObjectCheckBoxes.length; i++) {
      values[index++] = ((JCheckBox) memoryObjectCheckBoxes[i]).isSelected();
    }
    getViewsSetting().set(originalSource, values);
  }

  // Update display
  private void applyChanges() {
    // Keep this order is mandatory; setVisible -> setSort -> setOrder.
    checkMetricsChanged(this); // Will fire AnChangeEvent if changed
    checkSourceDisassemblyChanged(this); // Will fire AnChangeEvent if changed
    checkCompareModeChanged(this); // Will fire AnChangeEvent if changed
    checkViewModeChanged(this); // Will fire AnChangeEvent if changed
    checkFormatChanged(this); // Will fire AnChangeEvent if changed
    checkWrapNameChanged(this); // Will fire AnChangeEvent if changed
    checkTimelineChanged(this); // Will fire AnChangeEvent if changed
    checkSearchPathChanged(this); // Will fire AnChangeEvent if changed
    checkPathMapChanged(this); // Will fire AnChangeEvent if changed
    checkCallTreeChanges(this); // Will fire AnChangeEvent if changed
    checkViewsChanges(this); // Will fire AnChangeEvent if changed
  }

  // Set annotated visibility & check whether recompute is needed or not
  private void checkSourceDisassemblyChanged(Object originalsource) {
    final int[] anno_set;
    int new_vis_src;
    int new_vis_dis;
    final int new_threshold_src;
    final int new_threshold_dis;
    final boolean new_src_visible;
    final boolean new_src_metric;
    final boolean new_hex_visible;
    final boolean new_cmp_visible;
    final boolean new_scope_only;
    final boolean new_func_visible;

    // Get new Compiler Commentary visibility
    new_vis_src = new_vis_dis = 0;
    for (int i = 0; i < size_ccmv; i++) {
      if (((JCheckBox) src_chks[i]).isSelected()) {
        new_vis_src |= ccmv_type[i];
      }

      if (((JCheckBox) dis_chks[i]).isSelected()) {
        new_vis_dis |= ccmv_type[i];
      }
    }

    if (vis_src != new_vis_src) {
      vis_src = new_vis_src;
    }

    if (vis_dis != new_vis_dis) {
      vis_dis = new_vis_dis;
    }

    // Get new Highlighting Threshold
    new_threshold_src = Integer.parseInt(((JTextField) threshold[0]).getText());
    if (threshold_src != new_threshold_src) {
      threshold_src = new_threshold_src;
    }

    new_threshold_dis = Integer.parseInt(((JTextField) threshold[1]).getText());
    if (threshold_dis != new_threshold_dis) {
      threshold_dis = new_threshold_dis;
    }

    // Get new Disassembly Listing visibility
    new_src_visible = dis_src.isSelected();
    if (src_visible != new_src_visible) {
      src_visible = new_src_visible;
    }

    new_src_metric = dis_asrc.isSelected();
    if (src_metric != new_src_metric) {
      src_metric = new_src_metric;
    }

    new_hex_visible = dis_hex.isSelected();
    if (hex_visible != new_hex_visible) {
      hex_visible = new_hex_visible;
    }

    new_cmp_visible = cmp_line.isSelected();
    if (cmp_visible != new_cmp_visible) {
      cmp_visible = new_cmp_visible;
    }

    new_func_visible = func_line.isSelected();
    if (func_visible != new_func_visible) {
      func_visible = new_func_visible;
    }

    new_scope_only = dis_scope.isSelected();
    if (scope_only != new_scope_only) {
      scope_only = new_scope_only;
    }

    anno_set = new int[10];
    anno_set[0] = vis_src;
    anno_set[1] = vis_dis;
    anno_set[2] = threshold_src;
    anno_set[3] = threshold_dis;
    anno_set[4] = src_visible ? 1 : 0;
    anno_set[5] = src_metric ? 1 : 0;
    anno_set[6] = hex_visible ? 1 : 0;
    anno_set[7] = cmp_visible ? 1 : 0;
    anno_set[8] = scope_only ? 1 : 0;
    anno_set[9] = func_visible ? 1 : 0;
    getSourceDisassemblySetting()
        .set(originalsource, anno_set); // Will fire AnChangeevent if changes
  }

  private void checkCompareModeChanged(Object originalsource) {
    CompareMode newMode =
        !cmpModeLabel.isEnabled()
            ? CompareMode.CMP_DISABLE
            : cmp_abs_value.isSelected()
                ? CompareMode.CMP_ENABLE
                : cmp_delta.isSelected() ? CompareMode.CMP_DELTA : CompareMode.CMP_RATIO;
    getCompareModeSetting().set(originalsource, newMode); // Will fire AnChangeevent if changes
  }

  private void checkFormatChanged(Object originalsource) {
    FormatSetting.Style newStyle =
        mangled_fmt.isSelected()
            ? FormatSetting.Style.MANGLED
            : long_fmt.isSelected() ? FormatSetting.Style.LONG : FormatSetting.Style.SHORT;
    boolean newAppendSoName = soname_chk.isSelected();
    formatSetting.set(
        originalsource, newStyle, newAppendSoName); // Will fire AnChangeevent if changes
  }

  private void checkWrapNameChanged(Object originalsource) {
    getTableSettings()
        .setWrapMetricNames(
            originalsource, wrapNameCheckBox.isSelected()); // Will fire AnChangeevent if changes
  }

  private void checkViewModeChanged(Object originalsource) {
    ViewMode newViewMode;
    if (view_fmt.isSelected()) {
      newViewMode = ViewMode.USER;
    } else if (view_expert_fmt.isSelected()) {
      newViewMode = ViewMode.EXPERT;
    } else {
      newViewMode = ViewMode.MACHINE;
    }
    getViewModeSetting().set(originalsource, newViewMode); // Will fire AnChangeevent if changes
  }

  // Set timeline whether recompute is needed or not
  public Object getTimelineSettings(boolean forceReturnState) {
    AnUtility.checkIfOnAWTThread(true);
    boolean new_tm = false;

    // tl_entity (CPU,LWP, etc)
    String new_tl_entity_prop_name = UserPref.getInstance().getTimelineGroupDataByButtonName();
    int new_tl_entity_button_num = 0;
    for (int ii = 0; ii < tl_entity_button.size(); ii++) {
      if (tl_entity_button.get(ii).isSelected()) {
        TimelineSetting.EntityProp entityProp = getTimelineSetting().getTl_entity_prop(ii);
        if (entityProp != null) {
          new_tl_entity_prop_name = entityProp.prop_name;
          new_tl_entity_button_num = ii;
        } else {
          int xx = 1; // weird, could not find matching string.
        }
        break;
      }
    }
    if (getTimelineSetting().getTl_entity_selected_btn() != new_tl_entity_button_num) {
      new_tm = true;
    }

    boolean doShow = tldata_show_states_cb.isSelected();
    if (tldata_show_states != doShow) {
      tldata_show_states = doShow;
      new_tm = true;
    }

    doShow = tldata_show_counts_cb.isSelected();
    if (tldata_show_counts != doShow) {
      tldata_show_counts = doShow;
      new_tm = true;
    }

    // Stack mode
    final TLStack_align new_stack_align =
        (tl_stack_align[0].isSelected()
            ? TLStack_align.TLSTACK_ALIGN_ROOT
            : TLStack_align.TLSTACK_ALIGN_LEAF);
    if (getTimelineSetting().getStack_align() != new_stack_align) {
      new_tm = true;
    }

    // Stack depth
    //        final int new_stack_depth = tl_slider.getValue();
    //        if (getTimelineSetting().getTLStackDepth() != new_stack_depth) {
    final int new_stack_depth =
        getTimelineSetting().getTLStackDepth(); // YXXX not sure if this is right... THP?
    if (tldata_stack_depth != new_stack_depth) {
      new_tm = true;
    }

    // Stack magnify
    final int new_stack_frame_pixels = tl_slider.getValue() + TIMELINE_STACK_FRAME_MAGNIFY_OFFSET;
    if (getTimelineSetting().getTLStackFramePixels() != new_stack_frame_pixels) {
      new_tm = true;
    }

    //        synchronized (tldata_sync) {
    {
      // Data type
      int tldata_names_version = timelineSetting.getTLDataNamesVersion();
      if (previous_tldata_names_version != tldata_names_version) {
        previous_tldata_names_version = tldata_names_version;
        new_tm = true;
      }

      long new_disable_mask;
      long tldata_check_hidden_bitmask = timelineSetting.getTLDataHiddenMask();
      if (tldata_checkBoxes.isEmpty()) {
        new_disable_mask = tldata_check_hidden_bitmask;
      } else {
        new_disable_mask = 0;
        for (int i = 0; i < tldata_checkBoxes.size(); i++) {
          JCheckBox check = tldata_checkBoxes.get(i);
          if (!check.isSelected()) {
            long mask = (1L << i);
            new_disable_mask |= mask;
          }
        }
        if (tldata_check_hidden_bitmask != new_disable_mask) {
          timelineSetting.setTLDataHiddenMask(new_disable_mask);
          new_tm = true;
        }
      }
      // TBR?
      //            if (new_tm) {
      //                String tldata_cmd = timelineSetting.edt_update_tldata_cmd();
      ////            setTLValue(tldata_cmd, //YXXX does DBE really need to know this??
      ////                    new_tl_entity_prop_id, new_stack_align.getValue(),
      ////                    new_stack_depth);// IPC!!
      //            }

      if (new_tm) {
        needsTimelineEvent = true;
      }
      if (!forceReturnState && !new_tm) {
        return null;
      }

      // return values
      final ArrayList newtm_evt = new ArrayList(); // AnSettingChangeEvent.Type.TIMELINE event
      newtm_evt.add(new_tl_entity_prop_name); // 0
      newtm_evt.add(new_tl_entity_button_num); // 1
      newtm_evt.add(new_stack_align); // 2
      newtm_evt.add(Integer.valueOf(new_stack_depth)); // 3
      newtm_evt.add(Long.valueOf(new_disable_mask)); // 4
      newtm_evt.add(timelineSetting.getTLDataCNames()); // 5
      newtm_evt.add(timelineSetting.getTLDataUNames()); // 6
      newtm_evt.add(Integer.valueOf(previous_tldata_names_version)); // 7
      newtm_evt.add(Boolean.valueOf(tldata_show_states)); // 8
      newtm_evt.add(Boolean.valueOf(tldata_show_counts)); // 9
      newtm_evt.add(Integer.valueOf(new_stack_frame_pixels)); // 10
      newtm_evt.add(timelineSetting.getTLDataCmd()); // 11

      return newtm_evt;
    } // synchronized (tldata_sync) {
  }

  // Set visibility & check whether recompute is needed or not
  private void checkMetricsChanged(Object originalSource) {
    List<MetricNameSelection> list = metricsPanel.getMetricNameSelections();
    getMetricsSetting().set(originalSource, list); // will fire AnChangeEvent if changed
  }

  // Apply changes made in Preferences dialog
  private void apply() {
    applyChanges();
    settingsDialog.setButtonsEnabled(false);
  }

  // Show dialog for Save preferences
  public void saveAsDotErRc() {
    String homeDir;
    String workingDir = Analyzer.getInstance().getWorkingDirectory();

    if (Analyzer.getInstance().isRemote()) {
      homeDir = AnWindow.getInstance().getHomeDir(); // IPC
    } else {
      homeDir = Analyzer.home_dir;
    }
    final Object[] options = {currentDirStr + workingDir, homeDirStr + homeDir};

    Object result =
        JOptionPane.showInputDialog(
            tabbedPane,
            AnLocale.getString("Save Settings in:"),
            AnLocale.getString("Save Settings"),
            JOptionPane.OK_CANCEL_OPTION,
            null,
            options,
            options[0]);
    String resultString = (String) result;
    if (resultString == null) {
      // Nothing
    } else if (resultString.equals(options[0])) {
      save(workingDir);
    } else if (resultString.equals(options[1])) {
      save(homeDir);
    }
  }

  // Save preferences
  private void save(final String dir) {
    final String path = dir + "/.er.rc";

    // apply changes before saving (CR 6387624)
    apply();

    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            PrintWriter pw = null;
            // Check if file exists
            AnFile afile = anWindow.getAnFile(path);
            if (afile.exists()) {
              // Show confirmation dialog
              final String title = AnLocale.getString("Overwrite file?");
              final String message =
                  AnLocale.getString("File: ")
                      + path
                      + AnLocale.getString(" exists. Do you want to overwrite it?");
              int res =
                  JOptionPane.showConfirmDialog(
                      tabbedPane, message, title, JOptionPane.YES_NO_OPTION);
              if (res != JOptionPane.YES_OPTION) {
                return;
              }
            }
            String localPath = path;
            if (Analyzer.getInstance().isRemote()) {
              localPath = AnWindow.tempFile().getAbsolutePath();
            }
            try {
              pw = new PrintWriter(new BufferedWriter(new FileWriter(localPath)));
              write(pw);
            } catch (IOException e) {
              AnUtility.showMessage(tabbedPane, e.toString(), JOptionPane.ERROR_MESSAGE);
            } finally {
              if (pw != null) {
                pw.close();
              }
            }
            if (Analyzer.getInstance().isRemote()) {
              int res = AnWindow.copyToRemote(localPath, path);
              if (res < 0) {
                System.err.println("Settings:copyToRemote failed: " + res);
              }
            }
          }
        },
        "Presentation_thread");
  }

  // Write preferences
  private void write(final PrintWriter pw) {
    AnMetric sortMetric;
    String abbr, comd, vstr;
    char stype, sort_stype;
    boolean visible, tvisible, pvisible, sort_visible;
    final List<String> path;

    sortMetric = null;
    sort_stype = '\0';
    sort_visible = true;

    // Write dmetrics
    pw.print("dmetrics ");
    //        final AnMetric[] mlist = order_list.getMetrics();
    final AnMetric[] mlist = getMetricsSetting().getMetricListByMType(0);
    int last = mlist.length - 1;

    String prevcmd = "";
    int prevsubtype = -1;

    for (int i = 0; i < mlist.length; i++) {
      AnMetric mitem = mlist[i];

      // YXXX quick/dirty workaround for adjacent duplicate items caused by comparison
      String thiscmd = mitem.getComd();
      if (thiscmd.equalsIgnoreCase(prevcmd) && mitem.getSubType() == prevsubtype) {
        continue;
      }
      prevcmd = thiscmd;
      prevsubtype = mitem.getSubType();

      // Subtype
      abbr = mitem.getAbbr();
      // YXXX shouldn't we use getSubType() instead?
      if (mitem.getSubType() == AnMetric.EXCLUSIVE) {
        stype = 'e';
      } else if (mitem.getSubType() == AnMetric.INCLUSIVE) {
        stype = 'i';
      } else {
        stype = '\0';
      }
      // Sort
      visible = mitem.isVVisible();
      tvisible = mitem.isTVisible();
      pvisible = mitem.isPVisible();

      if (mitem.isSorted()) {
        sortMetric = mitem;
        sort_stype = stype;
        sort_visible = (visible || tvisible || pvisible);
      }

      // Visibility
      if (stype == '\0') {
        if (!visible) {
          pw.print('!');
        }
      } else {
        pw.print(stype);

        vstr = "";
        if (mitem.is_time_val()) {
          if (visible) {
            vstr += '+';
          }
          if (tvisible) {
            vstr += '.';
          }
        } else if (visible || tvisible) {
          vstr += '.';
        }

        if (pvisible) {
          vstr += '%';
        }
        if (vstr.equals("")) {
          pw.print('!');
        } else {
          pw.print(vstr);
        }
      }

      // Type
      comd = mitem.getComd();
      if (i == last) {
        pw.println(comd);
      } else {
        pw.print(comd + ":");
      }
    }

    // Write dsort
    if (sortMetric != null) {
      pw.print("dsort ");

      if (sort_stype != '\0') {
        pw.print(sort_stype);
        pw.print(sort_visible ? '.' : '!');
      } else if (!sort_visible) {
        pw.print('!');
      }

      pw.println(sortMetric.getComd());
    }

    // Write Compiler Commentary visibility
    pw.println("scc " + getCompComm(vis_src, false, false, false, false, false, false));
    pw.println(
        "dcc "
            + getCompComm(
                vis_dis,
                !src_visible,
                src_visible,
                src_metric,
                hex_visible,
                cmp_visible,
                scope_only));

    // Write the threshold
    pw.println("sthresh " + threshold_src);
    pw.println("dthresh " + threshold_dis);

    // Write name formats
    pw.println("name " + formatSetting.getStyle().name().toLowerCase());

    // Write view mode
    pw.println("view " + getViewModeSetting().get().name().toLowerCase());

    // TBR?
    // Write timeline mode
    //        int btn = getTimelineSetting().getTl_entity_selected_btn();
    //        TimelineSetting.EntityProp entityProp =
    //                        getTimelineSetting().getTl_entity_prop(btn);
    //        if(entityProp!=null){
    //            String modecmd = entityProp.prop_cmd;
    //            pw.println("tlmode " +
    //                modecmd
    //                + ":" + getTimelineSetting().getStack_align().toString().toLowerCase() +
    //                ":depth=" + getTimelineSetting().getTLStackDepth());
    //        }
    //        synchronized (tldata_sync) {
    {
      //            String tldata_cmd = timelineSetting.edt_update_tldata_cmd();
      //            pw.println("tldata all" + tldata_cmd);
      //            tldata_unused_cnames.clear(); // dump unused baggage
    }

    // Write search path
    path = getSearchPathSetting().get();
    last = path.size() - 1;

    if (last >= 0) {
      comd = "addpath ";
      for (int i = 0; i <= last; i++) {
        comd += path.get(i);

        if (i != last) {
          comd += ":";
        }
      }

      pw.println(comd);
    }

    String[] from = AnWindow.getInstance().getSettings().getPathMapSetting().getPathMapFrom();
    String[] to = AnWindow.getInstance().getSettings().getPathMapSetting().getPathMapTo();
    if (from.length == to.length) {
      for (int i = 0; i < from.length; i++) {
        pw.println("pathmap " + from[i] + " " + to[i]);
      }
    }

    // Save the custom index object defines
    Object[] custom_list = ViewsSetting.getCustomIndxObjectsIPC();
    if (custom_list != null) {
      String[] cnames = (String[]) custom_list[0]; // Names of custom index objects
      String[] expr = (String[]) custom_list[1]; // Formula for custom index object
      for (int i = 0; i < cnames.length; i++) {
        StringBuffer defineComd = new StringBuffer("indxobj_define ");
        defineComd.append(cnames[i] + " \"" + expr[i] + "\"");
        pw.println(defineComd);
      }
    }
  }

  // Get Compiler Commentary visibility command
  private static String getCompComm(
      final int vis_bits,
      final boolean no_src,
      final boolean show_src,
      final boolean show_asrc,
      final boolean show_hex,
      final boolean show_cmp,
      final boolean scope_only) {
    String str = "";
    boolean first = true;

    for (int i = 0; i < size_ccmv; i++) {
      if ((vis_bits & ccmv_type[i]) != 0) {
        if (first) {
          first = false;
        } else {
          str += ":";
        }
        str += ccmv_comd[i];
      }
    }

    // Got nothing
    if (first) {
      str = "none";
    }
    // Set source visibility for disasm
    if (no_src) {
      str += ":nosrc";
    }
    if (show_src) {
      str += ":src";
    }
    if (show_asrc) {
      str += ":asrc";
    }
    // Set hexadecimal visibility of instruction
    if (show_hex) {
      str += ":hex";
    }
    if (show_cmp) {
      str += ":cf";
    }
    if (scope_only) {
      str += ":scope";
    }
    return str;
  }

  // ----------------------------------------------------------------------------------------
  // ----------------------------------------------------------------------------------------
  public void toggleDisFuncScope() {
    scope_only = !scope_only;
    dis_scope.setSelected(scope_only);
    checkSourceDisassemblyChanged(this); // Will fire AnChangeEvent if changed
  }

  @Override
  public void stateChanged(final ChangeEvent e) {
    settingsDialog.setButtonsEnabled(true);
  }

  @Override
  public void insertUpdate(final DocumentEvent e) {
    settingsDialog.setButtonsEnabled(true);
  }

  @Override
  public void removeUpdate(final DocumentEvent e) {
    settingsDialog.setButtonsEnabled(true);
  }

  @Override
  public void changedUpdate(final DocumentEvent e) {
    settingsDialog.setButtonsEnabled(true);
  }

  // Native methods from liber_dbe.so
  public static AnMetric[] getMetricList(final Object[] ref_data) {
    final int msize = ((int[]) ref_data[0]).length;
    AnMetric[] mlist = new AnMetric[msize];
    final int[] type = (int[]) ref_data[0];
    final int[] subtype = (int[]) ref_data[1];
    final int[] clock = (int[]) ref_data[2];
    final int[] flavors = (int[]) ref_data[3];
    final int[] value_styles = (int[]) ref_data[4];
    final String[] user_name = (String[]) ref_data[5];
    final String[] expr_spec = (String[]) ref_data[6];
    final String[] aux = (String[]) ref_data[7];
    final String[] name = (String[]) ref_data[8];
    final String[] abbr = (String[]) ref_data[9];
    final String[] comd = (String[]) ref_data[10];
    final String[] unit = (String[]) ref_data[11];
    final int[] vis = (int[]) ref_data[12];
    final boolean[] sorted = (boolean[]) ref_data[13];
    final String[] legend = (String[]) ref_data[14];
    final int[] valtype = (int[]) ref_data[15];
    final String[] data_type_name = (String[]) ref_data[16];
    final String[] data_type_uname = (String[]) ref_data[17];
    final String[] short_desc = (String[]) ref_data[18];

    for (int i = 0; i < msize; i++) {
      AnMetric m =
          new AnMetric(
              type[i],
              subtype[i],
              clock[i],
              flavors[i],
              value_styles[i],
              user_name[i],
              expr_spec[i],
              aux[i],
              name[i],
              abbr[i],
              comd[i],
              unit[i],
              legend[i],
              vis[i],
              sorted[i],
              valtype[i],
              data_type_name[i],
              data_type_uname[i],
              short_desc[i]);
      mlist[i] = m;
    }
    return mlist;
  }

  /*
   * ------------------------------------- INNER CLASSES    ----------------------------------------------------
   */
  // Slider listener for updating the value label
  final class SliderLabelListener implements ChangeListener {

    final JLabel label;

    public SliderLabelListener(final JLabel label) {
      this.label = label;
    }

    public void stateChanged(final ChangeEvent e) {
      final JSlider slider = (JSlider) e.getSource();

      label.setText(AnLocale.getString("Call Stack Magnification: ") + slider.getValue());
      if (settingsDialog != null) {
        settingsDialog.setButtonsEnabled(true);
      }
    }
  }

  protected void setMessageAreaVisible(boolean visible) {
    settingsDialog.setMessageAreaVisible(visible);
  }

  public final class SettingsDialog extends AnDialog implements AnChangeListener {

    private JTextArea messageArea;

    public SettingsDialog() {
      super(anWindow, frame, title, false, aux, mnemonic, help_id);
      JPanel settingsPanel = new JPanel(new BorderLayout());
      settingsPanel.add(tabbedPane, BorderLayout.CENTER);

      messageArea = new JTextArea();
      messageArea.setForeground(Color.red);
      messageArea.setOpaque(false);
      messageArea.setBorder(null);
      messageArea.setWrapStyleWord(true);
      messageArea.setEditable(false);
      messageArea.setLineWrap(true);
      messageArea.setText(
          AnLocale.getString(
              "Some settings have been modified that may require the experiment(s) to be reloaded"
                  + " in order for the changes to take effect. An experiment can easily be reloaded"
                  + " from Welcome view."));
      messageArea.setVisible(false);
      messageArea.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
      settingsPanel.add(messageArea, BorderLayout.SOUTH);

      setAccessory(settingsPanel);
      setButtonsEnabled(false);
      // setModal(true);
      updateGUIViewMode();
      updateGUIDisplayCompareMode();
      updateGUINameFormat();
      updateGUITableFormat();
      AnEventManager.getInstance().addListener(this);
      setBackground(AnEnvironment.DEFAULT_DIALOG_BACKGROUND);
    }

    protected void setMessageAreaVisible(boolean visible) {
      if (messageArea != null) {
        messageArea.setVisible(visible);
      }
    }

    /**
     * AnChangeEvent handler. Events are always dispatched on AWT thread. Requirements to an event
     * handler like this one: o When it done handling the event and returns, all it's states need to
     * be *consistent* and *well-defined*. It cannot dispatch that or part of that work to a
     * different (worker) thread and not wait for it to finish. o It cannot spend significant time
     * handling the event. Significant work like doCompute needs to be dispatched to a worker
     * thread.
     *
     * @param e the event
     */
    @Override
    public void stateChanged(AnChangeEvent e) {
      //            System.out.println("Settings.SettingsDialog stateChanged:  " + e.getType());
      switch (e.getType()) {
        case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
        case EXPERIMENTS_LOADING_NEW:
        case EXPERIMENTS_LOADED_FAILED:
          break;
        case EXPERIMENTS_LOADED:
          updateTLDataCheckBoxes();
          if (messageArea != null) {
            messageArea.setVisible(false);
          }
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
        case SELECTED_OBJECT_CHANGED:
        case SELECTION_CHANGING:
        case SELECTION_CHANGED:
        case SELECTION_UPDATE:
          // Nothing
          break;
        case SETTING_CHANGED:
          AnSettingChangeEvent anSettingChangeEvent = (AnSettingChangeEvent) e.getSource();
          Object originalSource = anSettingChangeEvent.getOriginalSource();
          //                    System.out.println("Settings getOriginalSource: " + originalSource);
          if (originalSource == Settings.this) {
            return;
          }
          //                    System.out.println("Settings getOriginalSource:
          // doIt...............");
          if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.VIEW_MODE) {
            updateGUIViewMode(); // update GUI
          } else if (anSettingChangeEvent.getType()
              == AnSettingChangeEvent.Type.VIEW_MODE_ENABLED) {
            updateGUIViewModeVisible();
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.COMPARE_MODE) {
            updateGUIDisplayCompareMode(); // update GUI
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.FORMAT) {
            updateGUINameFormat(); // update GUI
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.TABLE_FORMATS) {
            updateGUITableFormat(); // update GUI
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.SRC_DIS) {
            updateGUISourceDisassembly(); // update GUI
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.TIMELINE) {
            updateGUITimeline(); // update GUI
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.SEARCH_PATH) {
            updateGUISearchPath(); // update GUI
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.PATH_MAP) {
            updateGUIPathMap(); // update GUI
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.CALL_TREE) {
            updateGUICallTree(); // update GUI
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.METRICS) {
            updateGUIMetrics(); // update GUI
            updateGUITimelineData();
            metricsPanel.updateGUIValues();
          } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.VIEWS) {
            ViewsSetting.Setting oldSetting =
                (ViewsSetting.Setting) anSettingChangeEvent.getOldValue();
            ViewsSetting.Setting newSetting =
                (ViewsSetting.Setting) anSettingChangeEvent.getNewValue();
            updateGUIViews(oldSetting, newSetting); // update GUI
          }
          break;
        case DEBUG:
          //                debug();
          break;
        default:
          System.err.println("AnChanged event " + e.getType() + " not handled: " + this);
          break;
      }
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      getDefault().actionPerformed(event);
    }

    public void showDialog(boolean show) {
      setVisible(show);
    }

    public void setButtonsEnabled(boolean set) {
      apply.setEnabled(set);
      ok.setEnabled(set);
      aux_actions[0].setEnabled(!set); // Export...
      aux_actions[1].setEnabled(!set); // Import...
    }

    public boolean areButtonsEnabled() {
      return apply.isEnabled();
    }
  }

  /*
   * ------------------------------------- CONSTANTS    --------------------------------------------------------
   */
  private static final String[] aux = {
    AnLocale.getString("Export..."), AnLocale.getString("Import...")
  };
  private static final char[] mnemonic = {
    AnLocale.getString('o', "MNEM_SETTINGS_EXPORT"), AnLocale.getString('i', "MNEM_SETTINGS_IMPORT")
  };
  private static final String help_id = AnVariable.HELP_Settings;
  private static final String title = AnLocale.getString("Settings");
  private static final String homeDirStr = AnLocale.getString("Home directory: ");
  private static final String currentDirStr = AnLocale.getString("Current directory: ");

  public enum TLData_type {
    // Note: the cmd strings are used for .er.rc

    TL_SAMPLE("PROFDATA_TYPE_SAMPLE", "sample", AnUtility.samp_icon),
    TL_GCEVENT(
        "PROFDATA_TYPE_GCEVENT", "gcevent", AnUtility.del_icon), // CXXX Bug 20801848 - change icon
    TL_HEAPSZ("PROFDATA_TYPE_HEAPSZ", "heapsize", AnUtility.heap_icon),
    TL_CLOCK("PROFDATA_TYPE_CLOCK", "clock", AnUtility.prof_icon),
    TL_HWC("PROFDATA_TYPE_HWC", "hwc", AnUtility.hwc_icon),
    TL_SYNC("PROFDATA_TYPE_SYNCH", "synctrace", AnUtility.sync_icon),
    TL_HEAP("PROFDATA_TYPE_HEAP", "heaptrace", AnUtility.heap_icon),
    TL_MPI("PROFDATA_TYPE_MPI", "mpitrace", AnUtility.mpi_icon),
    TL_RACES("PROFDATA_TYPE_RACE", "datarace", AnUtility.races_icon),
    TL_DLCK("PROFDATA_TYPE_DLCK", "deadlock", AnUtility.races_icon),
    TL_IOTRACE("PROFDATA_TYPE_IOTRACE", "iotrace", AnUtility.iotrace_icon),
    TL_UNKNOWN(null, "unknown", null),
    TL_ALL(null, "all", null);

    private TLData_type(String dataIdName, String cmd, ImageIcon icon) {
      data_id_name = dataIdName;
      this.cmd = cmd;
      this.icon = icon;
    }

    public final String getTLDataBaseCmd() {
      return cmd;
    }

    public final String getTLDataIdName() {
      return data_id_name;
    }
    ;

    public final ImageIcon getIcon() {
      return icon;
    }
    ;

    @Override
    public String toString() {
      return data_id_name;
    }

    private final String data_id_name; // DATA_ID name (not user name)
    private final String cmd; // .er.rc command name
    private final ImageIcon icon;
    private static final TLData_type values[] = {
      // Note: must match order above.
      // However, values do not have to match DBE DATA_* enums
      TL_SAMPLE,
      TL_GCEVENT,
      TL_HEAPSZ,
      TL_CLOCK,
      TL_HWC,
      TL_SYNC,
      TL_HEAP,
      TL_MPI,
      TL_RACES,
      TL_DLCK,
      TL_IOTRACE
    }; // CXXX Bug 20801848
    //      public static final List<TLData_type> VALUES
    //              = Collections.unmodifiableList(Arrays.asList(values));

    public static final TLData_type find(String _dataIdName) {
      String dataIdName = _dataIdName.toUpperCase();
      for (TLData_type tmp : values) {
        if (tmp.data_id_name.equals(dataIdName)) {
          return tmp;
        }
      }
      return TL_UNKNOWN;
    }
  }

  public enum TLStack_align {
    // Note: these strings are used for .er.rc

    TLSTACK_MODE_UNKNOWN(0, AnLocale.getString("Unknown")),
    TLSTACK_ALIGN_ROOT(1, AnLocale.getString("root")),
    TLSTACK_ALIGN_LEAF(2, AnLocale.getString("leaf"));

    private TLStack_align(int v, String n) {
      value = v;
      name = n;
    }

    public final int getValue() {
      return value;
    }
    ;

    @Override
    public String toString() {
      return name;
    }

    private final String name;
    private final int value;
    private static final TLStack_align values[] = {TLSTACK_ALIGN_ROOT, TLSTACK_ALIGN_LEAF};
    public static final List<TLStack_align> VALUES =
        Collections.unmodifiableList(Arrays.asList(values));

    public static final TLStack_align find(int intVal) {
      for (TLStack_align tmp : values) {
        if (tmp.value == intVal) {
          return tmp;
        }
      }
      return TLSTACK_MODE_UNKNOWN;
    }
  }

  private static final int size_ccmv = 11;
  private static final int CCMV_BASIC = 0x400; // for default messages
  private static final int CCMV_VER = 0x001; // Versioning messages
  private static final int CCMV_WARN = 0x002; // Warning messages
  private static final int CCMV_PAR = 0x004; // Parallelization messages
  private static final int CCMV_QUERY = 0x008; // Compiler queries
  private static final int CCMV_LOOP = 0x010; // Loop detail messages
  private static final int CCMV_PIPE = 0x020; // Pipelining messages
  private static final int CCMV_INLINE = 0x040; // Inlining information
  private static final int CCMV_MEMOPS = 0x080; // Messages concerning memory op
  private static final int CCMV_FE = 0x100; // Front-end messages
  private static final int CCMV_CG = 0x200; // Code-generator messages

  static {
    ccmv_type = new int[size_ccmv];
    ccmv_desc = new String[size_ccmv];
    ccmv_comd = new String[size_ccmv];
    ccmv_mnem = new char[size_ccmv];

    ccmv_type[0] = CCMV_BASIC;
    ccmv_desc[0] = AnLocale.getString("Default", "PRESENT_DEFAULT");
    ccmv_comd[0] = "basic";
    ccmv_mnem[0] = AnLocale.getString('D', "MNEM_PRESENT_DEFAULT");
    ccmv_type[1] = CCMV_VER;
    ccmv_desc[1] = AnLocale.getString("Versioning");
    ccmv_comd[1] = "version";
    //        ccmv_mnem[1] = AnLocale.getString('V', "MNEM_PRESENT_VERSIONING");
    ccmv_type[2] = CCMV_WARN;
    ccmv_desc[2] = AnLocale.getString("Warning", "PRESENT_WARNING");
    ccmv_comd[2] = "warn";
    //        ccmv_mnem[2] = AnLocale.getString('W', "MNEM_PRESENT_WARNING");
    ccmv_type[3] = CCMV_PAR;
    ccmv_desc[3] = AnLocale.getString("Parallelization");
    ccmv_comd[3] = "parallel";
    //        ccmv_mnem[3] = AnLocale.getString('P', "MNEM_PRESENT_PARALLELIZATION");
    ccmv_type[4] = CCMV_QUERY;
    ccmv_desc[4] = AnLocale.getString("Compiler queries");
    ccmv_comd[4] = "query";
    //        ccmv_mnem[4] = AnLocale.getString('q', "MNEM_PRESENT_COMPILER_QUERIES");
    ccmv_type[5] = CCMV_LOOP;
    ccmv_desc[5] = AnLocale.getString("Loop detail");
    ccmv_comd[5] = "loop";
    //        ccmv_mnem[5] = AnLocale.getString('L', "MNEM_PRESENT_LOOP_DETAIL");
    ccmv_type[6] = CCMV_PIPE;
    ccmv_desc[6] = AnLocale.getString("Pipelining");
    ccmv_comd[6] = "pipe";
    //        ccmv_mnem[6] = AnLocale.getString('n', "MNEM_PRESENT_PIPELINING");
    ccmv_type[7] = CCMV_INLINE;
    ccmv_desc[7] = AnLocale.getString("Inlining information");
    ccmv_comd[7] = "inline";
    //        ccmv_mnem[7] = AnLocale.getString('I', "MNEM_PRESENT_INLINE_INFORMATION");
    ccmv_type[8] = CCMV_MEMOPS;
    ccmv_desc[8] = AnLocale.getString("Memory operations");
    ccmv_comd[8] = "memops";
    //        ccmv_mnem[8] = AnLocale.getString('M', "MNEM_PRESENT_MEMORY_OPERATIONS");
    ccmv_type[9] = CCMV_FE;
    ccmv_desc[9] = AnLocale.getString("Front-end");
    ccmv_comd[9] = "fe";
    //        ccmv_mnem[9] = AnLocale.getString('F', "MNEM_PRESENT_FRONT_END");
    ccmv_type[10] = CCMV_CG;
    ccmv_desc[10] = AnLocale.getString("Code-generator");
    ccmv_comd[10] = "cg";
    //        ccmv_mnem[10] = AnLocale.getString('g', "MNEM_PRESENT_CODE_GENERATOR");
  }

  private static final int MIN_WIDTH = 800;
  private static final int TIMELINE_STACK_FRAME_MAGNIFY_MIN = 1;
  private static final int TIMELINE_STACK_FRAME_MAGNIFY_MAX = 7;
  private static final int TIMELINE_STACK_FRAME_MAGNIFY_OFFSET =
      1 - TIMELINE_STACK_FRAME_MAGNIFY_MIN;
  public static final int TIMELINE_MAX_STACK_FRAME_PIXELS =
      TIMELINE_STACK_FRAME_MAGNIFY_MAX - TIMELINE_STACK_FRAME_MAGNIFY_MIN + 1;
  public static final int TIMELINE_DEFAULT_STACK_FRAME_PIXELS = 4;
  //    private final static String excl_abbr = AnLocale.getString("Excl. ");
  //    private final static String incl_abbr = AnLocale.getString("Incl. ");
  //    private final static String attr_abbr = AnLocale.getString("Attr. ");
  //    private final static String data_abbr = AnLocale.getString("Data. ");
  //    private final static String indx_abbr = AnLocale.getString("Index. ");
  public static final String excl_name = AnLocale.getString("Exclusive ");
  public static final String incl_name = AnLocale.getString("Inclusive ");
  public static final String attr_name = AnLocale.getString("Attributed ");
  public static final String data_name = AnLocale.getString("Data-derived ");
  public static final String time_str = AnLocale.getString(" Time");
  private static final String FORMATS_STR = AnLocale.getString("Formats");
  private static final String SORT_STR = AnLocale.getString("Sort");
  private static final String Blank = " ";
}

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

package org.gprofng.mpmt.collect;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnChooser;
import org.gprofng.mpmt.AnDialog;
import org.gprofng.mpmt.AnFile;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.util.gui.AnDialog2;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;

public final class CollectPanel extends JPanel implements ActionListener {

  private static final int CEXP_NA = 0;
  private static final int CEXP_TARGET = 1;
  private static final int CEXP_NAME = 2;
  private static final int CEXP_DIR = 3;
  private static final int CEXP_GROUP = 4;
  private static final int CEXP_WORK = 5;
  private static final int CEXP_LNCH = 6;
  private static final int CEXP_ENV = 7;
  protected final String CSTR_TARGET, CSTR_DIR, CSTR_GROUP, CSTR_WORK, CSTR_TARGET_JDK;
  private TargetJDK target_jdk = null;
  public CollectExp target,
      args,
      exp_name,
      work_dir,
      env_vars,
      launcher,
      vm_args;
  public CollectData descendant,
      pause_sig,
      sample_sig,
      exp_limit,
      time_limit,
      arch_exp,
      clock_prof,
      sync_trace,
      hwc1_prof,
      hwc1_val,
      dspace_hwc1,
      hwc2_prof,
      hwc2_val,
      dspace_hwc2,
      io_trace,
      heap_trace,
      data_race,
      count_data,
      mpi_trace,
      jvm_prof,
      sample,
      dbxsample,
      ctr_attr,
      reg_alloc,
      deadlock_trace,
      prof_idle;
  private CollectPanel2 collectPanel2; // "Profile Running Process"
  private CollectPanel3 collectPanel3; // "System Profiling"
  public JTextField preview_cmd;
  // public JTextArea preview_cmd;
  //    public JScrollPane work_panel;
  public JPanel work_panel;
  public JTabbedPane tab_pane;
  public JRadioButton startPaused, startResumed;
  public JLabel start_state;

  public List<HWCEntry> selectedHWCList;
  public JList hwcList;
  private JScrollPane hwcListScrollPane;
  private HWCSelectDialog hwcSelectDialog = null;
  private AnDialog2 availableCountersDialog = null;

  public List<String> sel_attr;
  public JList attrList;
  private JScrollPane attrScrollPane;
  public JButton updateButton, cancelButton;
  public JPanel buttonPanel;
  public boolean attr_warn;
  private JButton propertyButton;
  private JButton addHWCButton;
  private JButton removeButton;
  //    private JButton showButton;
  private JButton remAttr;
  private JButton addAttr;
  private JButton modAttr;
  private JLabel counterLabel;
  private JLabel selectedAttrLabel;
  private JPanel hwcPanel;
  // Data to collect
  private final String[][] arch_data = {
    {
      AnLocale.getString("On"),
      AnLocale.getString("Used Sources"),
      AnLocale.getString("All Sources"),
      AnLocale.getString("Off")
    },
    {ipc_str_on, "usedsrc", "src", ipc_str_off}
  };
  private final String[][] desc_data = {
    {AnLocale.getString("On"), AnLocale.getString("Off"), AnLocale.getString("Custom")},
    {ipc_str_on, ipc_str_off, "=.*"}
  };
  private final String[][] sig_data = {
    {AnLocale.getString("Off"), "USR1", "USR2", "PROF", AnLocale.getString("Custom")},
    {ipc_str_off, "USR1", "USR2", "PROF", "USR1"}
  };
  private final String[][] clock_data = {
    {
      AnLocale.getString("Normal (100 Hz)"),
      AnLocale.getString("High (1000 Hz)"),
      AnLocale.getString("Low (10 Hz)"),
      AnLocale.getString("Custom (msec)")
    },
    {ipc_str_on, "high", "low", "10.0"}
  };
  private final String[][] hwc_data = {
    {
      AnLocale.getString("Auto"),
      AnLocale.getString("Low"),
      AnLocale.getString("Mid"),
      AnLocale.getString("High"),
      AnLocale.getString("Custom")
    },
    {"", "low", ipc_str_on, "high", "9999991"}
  };
  private final String[][] sync_data = {
    {AnLocale.getString("Calibrate"), AnLocale.getString("All"), AnLocale.getString("Custom")},
    {"calibrate", "all", "0"}
  };
  private final String[][] samp_data = {
    {AnLocale.getString("Normal"), AnLocale.getString("Custom")}, {ipc_str_on, "1"}
  };
  private final String[][] count_types = {
    {AnLocale.getString("On"), AnLocale.getString("Static")}, {ipc_str_on, "static"}
  };
  private final String[][] limit_data = {
    {AnLocale.getString("Unlimited"), AnLocale.getString("Custom")}, {ipc_str_empty, ipc_str_empty}
  };
  private final String[][] limit_time = {
    {AnLocale.getString("Unlimited"), AnLocale.getString("Custom")}, {ipc_str_empty, ipc_str_empty}
  };
  private final String[][] mpi_data = {
    {
      AnLocale.getString("OMPT"),
      AnLocale.getString("CT"),
      AnLocale.getString("OPENMPI"),
      AnLocale.getString("MPICH2"),
      AnLocale.getString("MVAPICH2"),
      AnLocale.getString("off")
    },
    {"OMPT", "CT", "OPENMPI", "MPICH2", "MVAPICH2", ipc_str_off}
  };
  private final String[][] hwc_dsp_data = {
    {AnLocale.getString("Off"), AnLocale.getString("On")}, {ipc_str_off, ipc_str_on}
  };
  private final String[][] prof_idle_data = {
    {AnLocale.getString("On"), AnLocale.getString("Off")}, {ipc_str_on, ipc_str_off}
  };
  private final String external_terminal = "/usr/bin/gnome-terminal";

  // backend info about system's hwcs
  private final int maxHWC;
  private final String[][] hwcSets;
  private final List<List<HWCEntry>> hwcLists;
  private final String[] hwc_attr_list; // hwc attribute list
  private final String[] hwc_help; // hwc help text
  private final Map<String, HWCEntry> hwcI18NMap; // Will be faced out
  private final Map<String, HWCEntry> hwcNameMap;
  private final List<HWCEntry> hwcFlatList;
  private JLabel errorLabel;

  // backend info saved into format needed for combo boxes
  private final String[][] hwc_name_data;
  private final String[][] hwc_att_data;
  private String[][] hwc_reg_data;

  private final ArrayList data_list, left_list;
  private final boolean profile_running_process;
  private final boolean system_profiling;

  private int target_type;
  private boolean toAdd;
  public JDialog hwcPropertyDialog;
  private CollectChooser dir_chooser;
  private CollectChooser target_chooser;
  private CollectChooser exp_group_chooser;
  private Collector collector;
  private JTextArea out_log;
  private JTextArea pioTextArea;
  private JButton bt_clear;
  private JPanel pioPanel;
  private AnWindow anWindow;

  private static Boolean mpi_support() {
    return false;
  }

  public JTextArea getOutLog() {
    return out_log;
  }

  public void setOutLog(JTextArea area) {
    out_log = area;
  }

  public JButton getBtClear() {
    return bt_clear;
  }

  private final JButton[] buttons;
  private JButton RunButton = null;
  private final AnDialog m_dialog;
  private final Provider m_prov;
  private Thread output_thread;
  private Thread read_file_thread = null;  // Read collect_output_file
  private int autoUpdaterState = 0;
  // Interface constants
  /* Interface strings GUI <-> CLI */
  static final String ipc_str_clkprof = "clkprof";
  static final String ipc_str_hwcprof = "hwcprof";
  static final String ipc_str_javaprof = "javaprof";
  static final String ipc_str_sample = "sample";
  static final String ipc_str_synctrace = "synctrace";
  static final String ipc_str_heaptrace = "heaptrace";
  static final String ipc_str_iotrace = "iotrace";
  static final String ipc_str_thatrace = "thatrace";
  static final String ipc_str_count = "count";
  static final String ipc_str_MPIexpt = "MPIexpt"; // -M option
  static final String ipc_str_MPItrace = "MPItrace"; // -m option
  static final String ipc_str_dbxsample = "dbxsample"; // not implemented yet
  static final String ipc_str_sample_sig = "sample_sig"; // not implemented yet
  static final String ipc_str_pause_resume_sig = "pause_resume_sig"; // not implemented yet
  static final String ipc_str_descendant = "descendant"; // not implemented yet
  static final String ipc_str_arch_exp = "arch_exp"; // not implemented yet
  static final String ipc_str_time_limit = "time_limit"; // not implemented yet
  static final String ipc_str_exp_limit = "exp_limit"; // not implemented yet
  static final String ipc_str_hwc2_prof = "hwc2_prof";
  static final String ipc_str_hwc_default = "default";
  static final String ipc_str_hwc1_val = "hwc1_val"; // not implemented yet
  static final String ipc_str_dspace_hwc1 = "dspace_hwc1"; // not implemented yet
  static final String ipc_str_hwc_reg_alloc = "hwc_reg_alloc"; // not implemented yet
  static final String ipc_str_hwc_ctr_attr = "hwc_ctr_attr"; // not implemented yet
  // Standard answers
  static final String ipc_str_empty = "";
  static final String ipc_str_comma = ",";
  static final String ipc_str_auto = "auto";
  static final String ipc_str_on = "on";
  static final String ipc_str_off = "off";
  static final String ipc_str_space = " ";
  static final String ipc_str_unknown_control = "Unknown control";
  static final String ipc_str_internal_error = "Internal error";
  static final String ipc_str_zero = "0";
  static final String ipc_str_all = "all";
  static final String ipc_str_None = "None";
  static final String ipc_str_race = "race";
  static final String ipc_str_deadlock = "deadlock";
  static final String ipc_str_prof_idle = "prof_idle";
  // Semaphores
  private final Object globalSem = new Object(); //
  private Boolean inItemStateChanged = false;

  private Boolean clkprof_on_by_default = true;
  private Boolean javaprof_on_by_default = true;
  private Boolean hwcprof_on_by_default = false;
  private Boolean sample_on_by_default = true;
  private Boolean clkprof_not_changed_by_user = true;
  private Boolean javaprof_not_changed_by_user = true;
  private Boolean hwcprof_not_changed_by_user = true;
  private Boolean sample_not_changed_by_user = true;

  private String collect_output_file = null; // Temporary file for collect's output
  private String temp_file_name = null; // Temporary file for generated profiling script

  public CollectPanel(
      Collector collector,
      AnDialog anDialog,
      Provider cprov,
      AnWindow anWindow,
      JButton[] buttons) {
    this.collector = collector;
    this.anWindow = anWindow;
    this.m_dialog = anDialog;
    this.m_prov = cprov;

    CSTR_TARGET = AnLocale.getString("Target Program");
    CSTR_DIR = AnLocale.getString("Experiment Directory");
    CSTR_GROUP = AnLocale.getString("Experiment Group");
    CSTR_WORK = AnLocale.getString("Working Directory");
    CSTR_TARGET_JDK = AnLocale.getString("Target JDK");

    data_list = new ArrayList();
    left_list = new ArrayList();

    this.buttons = buttons;

    //        int h = CollectUtility.text_font.getSize();
    //        float hf = h;
    //        titlefont = CollectUtility.text_font.deriveFont(1, hf);
    if (collector.getProfilingType() == Collector.PROFILE_RUNNING_APPLICATION) {
      profile_running_process = true;
    } else {
      profile_running_process = false;
    }

    if (collector.getProfilingType() == Collector.SYSTEM_PROFILING) {
      system_profiling = true;
    } else {
      system_profiling = false;
    }

    // HW Counters IPC
    // Note: could be done in parallel to reduce latency
    hwcSets = getHwcSets(system_profiling);
    maxHWC = getHwcMaxConcurrent(system_profiling);
    hwc_attr_list = getHwcAttrList(system_profiling);
    hwcLists = getHWCsAllProcess(getHwcsAll(system_profiling));
    hwc_help = getHwcHelp(system_profiling);

    // populate hwc_map and hwcFlatList
    hwcI18NMap = new HashMap();
    hwcNameMap = new HashMap();
    hwcFlatList = new ArrayList();
    // prepend default counters at top of list
    for (int ii = 0; hwcSets[0] != null && ii < hwcSets[0].length; ii++) {
      HWCEntry hwcEntry =
          new HWCEntry(
              true,
              hwcSets[1][ii],
              hwcSets[1][ii],
              hwcSets[0][ii],
              null,
              0,
              0,
              0,
              null,
              null,
              false,
              false,
              true);
      hwcI18NMap.put(hwcEntry.getI18n(), hwcEntry);
      hwcNameMap.put(hwcEntry.getName(), hwcEntry);
      hwcEntry.setNo(hwcFlatList.size());
      hwcFlatList.add(hwcEntry);
    }
    for (List<HWCEntry> list : hwcLists) {
      for (HWCEntry hwcEntry : list) {
        hwcI18NMap.put(hwcEntry.getI18n(), hwcEntry);
        hwcNameMap.put(hwcEntry.getName(), hwcEntry);
        hwcEntry.setNo(hwcFlatList.size());
        hwcFlatList.add(hwcEntry);
      }
    }

    // populate *_data fields used in dropdowns
    if (hwcFlatList.isEmpty()) {
      hwc_name_data = new String[2][0];
      // THOMAS: should probably disable HWCs in this case
    } else {
      hwc_name_data = new String[2][hwcFlatList.size()];
      int ii = 0;
      for (HWCEntry hwcentry : hwcFlatList) {
        hwc_name_data[0][ii] = hwcentry.getI18n();
        hwc_name_data[1][ii] = hwcentry.getName();
        ii++;
      }
      hwc_reg_data = setRegData(hwcFlatList.get(0).getI18n());
    }

    // populate hwc_att_data
    final String[] alist = hwc_attr_list;
    if ((alist == null) || (alist.length == 0)) {
      hwc_att_data = new String[2][1];
      hwc_att_data[0][0] = AnLocale.getString("None");
      hwc_att_data[1][0] = "0";
    } else {
      hwc_att_data = new String[2][alist.length];
      hwc_att_data[0] = alist;
      for (int i = 0; i < hwc_att_data[0].length; i++) {
        hwc_att_data[1][i] = "0";
      }
    }

    //        dumpHWCounters();
    initComponents();

    initButtons();

    if (profile_running_process) {
      collectPanel2.update();
    }
    if (system_profiling) {
      collectPanel3.update();
    }

    autoUpdate();

    collector.addCollectingStatusListener(this);
  }

  private void dumpHWCEntry(int no, HWCEntry hwcentry) {
    System.out.println("=============================== " + no);
    System.out.println("recommended: " + hwcentry.isRecommended());
    System.out.println("name       : " + hwcentry.getName());
    System.out.println("collector  : " + hwcentry.getCollectorString());
    System.out.println("i18n       : " + hwcentry.getI18n());
    System.out.println("int_name   : " + hwcentry.getIntName());
    System.out.println("short_desc : " + hwcentry.getShortDesc());
    System.out.println("metric     : " + hwcentry.getMetric());
    System.out.println(
        "val: "
            + hwcentry.getVal()
            + ", timecvt: "
            + hwcentry.getTimecvt()
            + ", supportsAttrs: "
            + hwcentry.supportsAttrs()
            + ", supportsMemspace: "
            + hwcentry.supportsMemspace(hwcNameMap)
            + ", memop: "
            + hwcentry.getMemop());
    System.out.print("regs: ");
    if (hwcentry.getRegList() != null) {
      for (int i = 0; i < hwcentry.getRegList().length; i++) {
        System.out.print(" " + hwcentry.getRegList()[i]);
      }
    }
    System.out.println("");
  }

  private void dumpHWCounters() {
    int no = 0;
    System.out.println("************************************************* hwc_max_reg");
    System.out.println(maxHWC);

    System.out.println("************************************************* hwc_sets");
    System.out.println("hwc_sets");
    for (int i = 0; i < hwcSets[0].length; i++) {
      System.out.println(i + ": " + hwcSets[0][i] + ", " + hwcSets[1][i]);
    }

    System.out.println("************************************************* hwcs (std)");
    no = 0;
    for (HWCEntry hwcentry : hwcLists.get(0)) {
      dumpHWCEntry(no++, hwcentry);
    }

    System.out.println("************************************************* hwcs (raw)");
    no = 0;
    for (HWCEntry hwcentry : hwcLists.get(1)) {
      dumpHWCEntry(no++, hwcentry);
    }

    System.out.println("************************************************* hwcFlatList");
    no = 0;
    for (HWCEntry hwcentry : hwcFlatList) {
      dumpHWCEntry(no++, hwcentry);
    }

    System.out.println("************************************************* hwc_attr_list");
    for (int i = 0; i < hwc_attr_list.length; i++) {
      System.out.println(i + ": " + hwc_attr_list[i]);
    }
  }

  /** Check fields and update buttons */
  private void autoUpdate() {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            String err = null;
            RunButton = buttons[0];
            while (null != target) {
              try {
                Thread.sleep(200); // wait 200 milliseconds
                if (!m_dialog.isVisible()) {
                  continue; // to sleep
                }
                synchronized (globalSem) {
                  if (autoUpdaterState == 1) {
                    String target_name = target.getText();
                    if (profile_running_process) {
                      if (null != target_name) {
                        target_name = collectPanel2.getProcessID();
                      }
                    }
                    if (system_profiling) {
                      if (null == target_name) {
                        target_name = collectPanel3.getProcessID();
                      }
                    }
                    if ((null != target_name) && (target_name.length() > 0)) {
                      RunButton.setEnabled(true); // XXX should use AWT thread
                      RunButton.setToolTipText(AnLocale.getString("Start Profiling"));
                    } else if (system_profiling) {
                      RunButton.setEnabled(true); // XXX should use AWT thread
                      RunButton.setToolTipText(AnLocale.getString("Start Kernel Profiling"));
                    } else {
                      RunButton.setEnabled(false); // XXX should use AWT thread
                      RunButton.setToolTipText(AnLocale.getString("Target must be specified"));
                    }
                    if (getErrorText().length() > 0) {
                      RunButton.setEnabled(false); // XXX should use AWT thread
                      RunButton.setToolTipText(getErrorText());
                    }
                    if (!profile_running_process && !system_profiling) {
                      // Dependency between data_race, deadlock_trace, and descendant
                      if ((data_race.check.isEnabled()) || (deadlock_trace.check.isEnabled())) {
                        if ((data_race.isChecked()) || (deadlock_trace.isChecked())) {
                          // disable descendant.combo TEMPORARY (DEBUG)
                          descendant.combo.setEnabled(false);
                          // descendant.combo.setSelectedIndex(1); // off
                        } else {
                          descendant.combo.setEnabled(true);
                          // descendant.combo.setSelectedIndex(0); // on
                        }
                      }
                      if ((descendant.field != null)
                          && (ipc_str_on.equals(descendant.field.getText()))) {
                        // do not disable data_race - on is a default
                        // data_race.check.setEnabled(false);
                        // deadlock_trace.check.setEnabled(false);
                      } else {
                        // Update descendant mode
                        if ((descendant.field != null)
                            && (ipc_str_off.equals(descendant.field.getText()))) {
                          Collector.setCollectorControlValue(
                              descendant.name, ipc_str_off); // IPC call
                        } else {
                          Collector.unsetCollectorControlValue(descendant.name); // IPC call
                        }
                        if ((!data_race.check.isEnabled())) {
                          // try to enable data_race
                          err =
                              Collector.setCollectorControlValue(
                                  data_race.name, ipc_str_on); // IPC call
                          if (null == err) {
                            data_race.check.setEnabled(true); // Only if other controls allow
                            if (!data_race.isChecked()) {
                              Collector.unsetCollectorControlValue(data_race.name); // IPC call
                            }
                          } else {
                            data_race.check.setEnabled(false);
                            data_race.check.setSelected(false);
                            // err = m_window.setCollectorControlValue(data_race.name, ipc_str_off);
                            // // IPC call
                          }
                        }
                        if ((!deadlock_trace.check.isEnabled())) {
                          // try to enable deadlock_trace
                          err =
                              Collector.setCollectorControlValue(
                                  deadlock_trace.name, ipc_str_on); // IPC call
                          if (null == err) {
                            deadlock_trace.check.setEnabled(true);
                            if (!deadlock_trace.isChecked()) {
                              Collector.unsetCollectorControlValue(deadlock_trace.name); // IPC call
                            }
                          } else {
                            deadlock_trace.check.setEnabled(false);
                            deadlock_trace.check.setSelected(false);
                            // err = m_window.setCollectorControlValue(deadlock_trace.name,
                            // ipc_str_off); // IPC call
                          }
                        }
                      }
                      // End of Dependency between data_race, deadlock_trace, and descendant
                    } // !in_dbx
                  }
                  if (autoUpdaterState == 2) {
                    break; // Done
                  }
                } // end of critical section
              } catch (Exception e) {
                // break;
              }
            }
          }
        },
        "autoUpdateCollectDialog");
  }

  /**
   * Set AutoUpdater State
   *
   * @param state
   */
  private void setAutoUpdaterState(int state) {
    autoUpdaterState = state;
    try {
      Thread.sleep(300); // wait 200 milliseconds
    } catch (Exception e) {
      // break;
    }
  }

  /**
   * Read lines from specified InputStream and print them to the Process Output (JTextArea
   * pioTextArea)
   *
   * @param from
   */
  private void output(InputStream from, List fromVector) {
    String line;
    Thread thisThread = Thread.currentThread();
    BufferedReader br = null;
    if (null == fromVector) {
      br = new BufferedReader(new InputStreamReader(from));
    }
    boolean closed = false;
    JTextArea to = pioTextArea;
    int line_number = 0;

    while ((output_thread == thisThread) && !closed) {
      try {
        line = null;
        if (null == br) {
          if (fromVector.size() > 0) {
            line = (String) fromVector.get(0);
            fromVector.remove(0);
          } else if (output_thread != thisThread) {
            closed = true;
            break;
          }
        } else {
          line = br.readLine();
        }
        if (line != null) {
          if (line.length() > 0) {
            line_number++;
            if (line_number < 100) {
              // XXX: TEMPORARY HACK!!!
              String pattern = "Creating experiment directory ";
              int idx = line.indexOf(pattern);
              if (idx >= 0) {
                idx += pattern.length();
                String substr = line.substring(idx);
                idx = substr.indexOf(ipc_str_space);
                if (idx > 1) {
                  String expname = substr.substring(0, idx); //  test.*.er
                  collector.setActualExpName(expname);
                }
                // XXX: ANOTHER TEMPORARY HACK!!!
                pattern = "(Process ID: ";
                int n = line.indexOf(pattern);
                if (n >= 0) {
                  n += pattern.length();
                  long p = 0;
                  int d = 0;
                  while ((d >= 0) && (d <= 9)) {
                    p = p * 10 + d;
                    d = line.charAt(n) - '0';
                    n++;
                  }
                  if (p > 0) {
                    collector.setProcessPID(p);
                  }
                }
              }
            }
            write /*ln*/(line, to);
            // We need a short delay to make sure the responsiveness
            // is good - otherwise AWT thread can be overloaded with
            // output requests and buttons Pause, Resume, Terminate
            // work with long delays. 10 milliseconds should be enough.
            // Thread.sleep(10);
            if (line_number % 100 == 0) {
              Thread.sleep(10);
            }
            continue;
          }
        }
        Thread.sleep(10); // empty line
      } catch (Exception e) {
        // don't print anything, this exception is expected
        // printException(e, AnLocale.getString("I/O exception 0"));
        if (output_thread != thisThread) {
          // closed = true;
          break;
        }
      } finally {
        // closed = true;
      }
      // NM if (output_thread == thisThread) {
      // NM     closed = false; // TEMPORARY
      // NM }
    }
  }

  /**
   * Read lines from specified InputStream and print them to the Process Output (JTextArea
   * pioTextArea)
   *
   * @param from
   */
  private void output(InputStream from, JTextArea out) {
    BufferedReader br = new BufferedReader(new InputStreamReader(from));
    boolean closed = false;
    JTextArea to = pioTextArea;
    if (out != null) {
      to = out;
    }
    int line_number = 0;

    while (!closed) {
      String line;
      try {
        line = br.readLine();
        if (line != null) {
          if (line.length() > 0) {
            line_number++;
            if (line_number < 100) {
              // XXX: TEMPORARY HACK!!!
              String pattern = "Creating experiment directory ";
              int idx = line.indexOf(pattern);
              if (idx >= 0) {
                idx += pattern.length();
                String substr = line.substring(idx);
                idx = substr.indexOf(ipc_str_space);
                if (idx > 1) {
                  String expname = substr.substring(0, idx); //  test.*.er
                  collector.setActualExpName(expname);
                }
                // XXX: ANOTHER TEMPORARY HACK!!!
                pattern = "(Process ID: ";
                int n = line.indexOf(pattern);
                if (n >= 0) {
                  n += pattern.length();
                  long p = 0;
                  int d = 0;
                  while ((d >= 0) && (d <= 9)) {
                    p = p * 10 + d;
                    d = line.charAt(n) - '0';
                    n++;
                  }
                  if (p > 0) {
                    collector.setProcessPID(p);
                  }
                }
              }
            }
            // write/*ln*/(line, to);
            writeln(line, to);
            // We need a short delay to make sure the responsiveness
            // is good - otherwise AWT thread can be overloaded with
            // output requests and buttons Pause, Resume, Terminate
            // work with long delays. 10 milliseconds should be enough.
            if (line_number % 100 == 0) {
              Thread.sleep(10);
            }
            continue;
          }
        }
        Thread.sleep(10); // empty line
      } catch (Exception e) {
        // don't print anything, this exception is expected
        // printException(e, AnLocale.getString("I/O exception 0"));
        closed = true;
        break;
      }
    }
  }

  private static void printException(Exception e, String annot_msg) {

    if (annot_msg != null) {
      System.err.println(annot_msg + ": " + e.getMessage()); // DEBUG
    } else {
      System.err.println(e.getMessage()); // DEBUG
    }
  }

  private void write(final String line, final JTextArea to) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            to.append(line);
            to.setCaretPosition(to.getDocument().getLength());
          }
        });
  }

  // Output with new line
  private void writeln(final String line, final JTextArea to) {
    write(line + "\n", to);
  }

  // Add one collect argument
  private static void addOne(final ArrayList cmd_list, final String flag, final String str) {
    if ((str != null) && (str.length() > 0)) {
      if (flag != null) {
        cmd_list.add(flag);
      }
      cmd_list.add(str);
    }
  }

  private ArrayList getCmdList() {
    final ArrayList cmd_list;
    final String hwc1, hwc2;

    cmd_list = new ArrayList();

    String cmd = Analyzer.getInstance().getPathToCollect();

    // experiment name/dir/group
    if (profile_running_process) {
      cmd_list.add(cmd);
      addOne(cmd_list, "-P", collectPanel2.getProcessID());
      addOne(cmd_list, "-o", collectPanel2.getExperimentName());
    } else if (system_profiling) {
      cmd = Analyzer.getInstance().getPathToCollectKernel();
      cmd_list.add(cmd);
      String pid = collectPanel3.getProcessID();
      if ((null != pid) && (pid.length() > 0)) {
        addOne(cmd_list, "-F", "=" + pid);
      } else { // er_kernel supports only one -F option
        String proc = collectPanel3.getProcessName();
        if ((null != proc) && (proc.length() > 0)) {
          addOne(cmd_list, "-F", "=" + proc);
        } else {
          addOne(cmd_list, "-F", descendant.getValue());
        }
      }
      addOne(cmd_list, "-o", collectPanel3.getExperimentName());
      addOne(cmd_list, "-x", prof_idle.getValue());
    } else { // profile application
      cmd_list.add(cmd);
      addOne(cmd_list, "-o", exp_name.getText());
    }

    // don't add default limit value (now it's 2000 MB)
    addOne(cmd_list, "-L", exp_limit.getValue());

    addOne(cmd_list, "-t", time_limit.getValue());

    if (!profile_running_process && !system_profiling) {
      if (!descendant.getValue().equals(ipc_str_on)) { // Now "on" is the default value
        addOne(cmd_list, "-F", descendant.getValue());
      }
    } else {
      // dbx cannot profile descendants
      // already done for er_kernel
    }
    // Archiving
    if (!arch_exp.getValue().equals(ipc_str_on)) { // Now "on" is the default value
      addOne(cmd_list, "-A", arch_exp.getValue());
    }

    // set signals
    if (pause_sig.getValue() != null) {
      if (!pause_sig.getValue().equals(ipc_str_off)) {
        if (startResumed.isSelected()) {
          addOne(cmd_list, "-y", pause_sig.getValue() + ",r");
        } else {
          addOne(cmd_list, "-y", pause_sig.getValue());
        }
      }
    }
    addOne(cmd_list, "-l", sample_sig.getValue());

    // data to collect
    addOne(cmd_list, "-p", clock_prof.getValue());
    addOne(cmd_list, "-S", sample.getValue());

    if (((hwc1 = hwc1_prof.getValue()) != null) && (selectedHWCList.size() > 0)) {
      String ctrlist = ipc_str_empty;
      String separator = ipc_str_empty;
      for (int i = 0; i < selectedHWCList.size(); i++) {
        String ctrname = selectedHWCList.get(i).getCollectorString();
        addOne(cmd_list, "-h", ctrname);
      }
    }
    if (!profile_running_process && !system_profiling) {
      // Tracing options
      addOne(cmd_list, "-s", sync_trace.getValue());
      if ((null != io_trace.getValue()) && (!io_trace.getValue().equals(ipc_str_off))) {
        addOne(cmd_list, "-i", io_trace.getValue());
      }

      addOne(cmd_list, "-H", heap_trace.getValue());

      // thread analyzer parameters
      if ((data_race.isChecked()) || (deadlock_trace.isChecked())) {

        boolean comma_is_required = false;
        String tha_params = ipc_str_empty;

        if (data_race.isChecked()) {
          comma_is_required = true;
          tha_params += "race";
        }

        if (deadlock_trace.isChecked()) {
          tha_params += comma_is_required ? ipc_str_comma : ipc_str_empty;
          tha_params += "deadlock";
        }

        addOne(cmd_list, "-r", tha_params);
      }

      addOne(cmd_list, "-c", count_data.getValue());
      String mpi_trace_value = mpi_trace.getValue();
      if (mpi_trace_value != null) {
        if (!mpi_trace_value.equals(ipc_str_off)) {
          addOne(cmd_list, "-M", mpi_trace_value);
        }
      }
      String java_prof = jvm_prof.getValue();
      if (java_prof == null) { // pass "-j off"
        java_prof = ipc_str_off;
        addOne(cmd_list, "-j", java_prof);
      } // no need to pass "-j on" because it is the default
    } else // tracing is not supported by 'collect -P'
    {
      if (!system_profiling) {
        // Now 'collect -P' can support java profiling
        addOne(cmd_list, "-j", jvm_prof.getValue());
      }
    }

    if (!profile_running_process /* && !system_profiling */) {
      target_type = getTargetType(target.getText());

      // target command & arguments
      cmd_list.add(target.getText());
      addOne(cmd_list, null, args.getText());
    } else {
      // 'collect -P' does not need target
    }

    return cmd_list;
  }

  // Initialize the HWC configuration dialog
  public void showHWCPropertyDialog(HWCEntry entry) {
    String title = AnLocale.getString("Hardware Counter Properties");
    AnDialog2 hwcPropertyDialog2 = new AnDialog2(m_dialog, m_dialog, title);
    hwcPropertyDialog2.getAccessibleContext().setAccessibleDescription(title);
    hwcPropertyDialog2.setCustomPanel(getHwcPanel(entry));
    hwcPropertyDialog2.getOKButton().setText(AnLocale.getString("Update"));
    hwcPropertyDialog2.setVisible(true);
    if (hwcPropertyDialog2.getStatus() == AnDialog2.Status.OK) {
      int selectedIndex = hwcList.getSelectedIndex();
      updateCtrList(entry);
      hwcList.setSelectedIndex(selectedIndex);
    }
  }

  // Initialize the HWC configuration dialog
  public void showHWCSelectDialog() {
    if (hwcSelectDialog == null) {
      hwcSelectDialog = new HWCSelectDialog(m_dialog, this, hwcFlatList, hwcNameMap);
    }
    hwcSelectDialog.setVisible(true);
  }

  public void closeHWCDialogs() {
    if (hwcSelectDialog != null) {
      hwcSelectDialog.setVisible(false);
    }
    if (availableCountersDialog != null) {
      availableCountersDialog.setVisible(false);
    }
  }

  protected void addHWCEntry(List<HWCEntry> selectedList) {
    int oldSize = selectedHWCList.size();
    for (HWCEntry entry : selectedList) {
      if (entry.isSet()) {
        List<String> names = entry.getSetNames();
        for (String name : names) {
          HWCEntry nameEntry = hwcNameMap.get(name);
          if (nameEntry != null) {
            selectedHWCList.add(nameEntry);
          } else {
            // FIXUP....
          }
        }
      } else {
        selectedHWCList.add(entry);
      }
    }
    updateButtons();
    int newSelectedIndex = oldSize++;
    //        if (newSelectedIndex >= 0) { //FIXUP always >0
    hwcList.setSelectedIndex(newSelectedIndex);
    //        }
    hwcList.requestFocus();
  }

  // Update list of selected counters
  void updateCtrList(HWCEntry hwcEntry) {
    String hwc1;
    String regval = ipc_str_empty;
    String attr = ipc_str_empty;

    // Warn user if add attribute wasn't hit after a change
    if (attr_warn == true) {
      final String msg =
          AnLocale.getString(
              "Attribute selection changed and Add Attribute was not clicked, do you want to"
                  + " discard changes");
      final int user_choice =
          JOptionPane.showConfirmDialog(
              this,
              msg,
              AnLocale.getString("Question"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
      if (user_choice == JOptionPane.NO_OPTION) // do not discard changes
      {
        return;
      }
    }

    if (toAdd == true) {
      if ((hwc1 = hwc2_prof.getValue()) != null) {
        final String aggr_prof1 =
            dspace_hwc1.getValue() != null && dspace_hwc1.getValue().equals(ipc_str_on)
                ? "+"
                : ipc_str_empty;
        String[] hwcs = hwc1.split(ipc_str_comma);
        for (int i = 0; i < sel_attr.size(); i++) {
          attr = attr + (String) sel_attr.get(i);
        }
        if (!reg_alloc.getValue().equals("None")) {
          regval = "/" + reg_alloc.getValue();
        }
        String suffix = ipc_str_comma + hwc1_val.getValue();
        if (suffix != null && suffix.equals(ipc_str_comma)) {
          suffix = ipc_str_empty;
        }
        //                if (!hwcs[0].equals(ipc_str_hwc_default) &&
        // ipc_str_on.equals(hwc1_val.getValue())) {
        //                    suffix = ipc_str_empty; // default
        //                }
        String ctrstr = aggr_prof1 + hwcs[0] + attr + regval + suffix;
        //                HWCEntry hwcEntry = null;
        //                for (HWCEntry entry : selectedHWCList) {
        //                    if (entry.getName().equals(hwcs[0])) {
        //                        hwcEntry = entry;
        //                        break;
        //                    }
        //                }
        //                if (hwcEntry == null) {
        //                    hwcEntry = hwcNameMap.get(hwcs[0]);
        //                }
        if (hwcEntry != null) {
          hwcEntry.setCollectorString(ctrstr);
        }

        //                // If this register was already added - remove it
        //                for (int i = 0; i < selectedHWCList.size(); i++) {
        //                    String s = selectedHWCList.get(i);
        //                    int k = s.indexOf(ipc_str_comma);
        //                    if (k > 0) {
        //                        s = s.substring(0, k);
        //                    }
        //                    int m = hwc1.indexOf(ipc_str_comma);
        //                    String newhwc = hwc1;
        //                    if (m > 0) {
        //                        newhwc = hwc1.substring(0, m);
        //                    }
        //                    if (s.equals(newhwc)) {
        //                        selectedHWCList.remove(i); // remove duplicate
        //                    }
        //                }
        //                selectedHWCList.add(ctrstr); // FIXUP
        updateButtons();
      }
    } else // Modify existing entry
    {
      System.out.println(AnLocale.getString("Existing counter to be modified"));
    }
    attr_warn = false;
  }

  // Delete counter from list
  void deleteCtrFromList() {
    int idx = hwcList.getSelectedIndex();
    selectedHWCList.remove(idx);
    updateButtons();
  }

  // Add/Update attribute list
  void addAttribute() {
    attr_warn = false;
    String attrname = (String) ctr_attr.combo.getSelectedItem();
    String attrStr = "~" + attrname + "=";
    String attrEntry = attrStr + ctr_attr.getValue();

    Iterator en;
    boolean found = false;
    int i;
    for (i = 0, en = sel_attr.iterator(); en.hasNext(); i++) {
      String cur_elem = (String) en.next();
      if (cur_elem.indexOf(attrStr) != -1) {
        sel_attr.set(i, attrEntry); // replace element
        found = true;
      }
    }
    if (!found) {
      sel_attr.add(attrEntry);
    }
    setAttrButtons();
  }

  // Delete attribute from list
  void removeAttribute() {
    int idx = attrList.getSelectedIndex();
    sel_attr.remove(idx);
    setAttrButtons();
  }

  private void showChooser(String cmd) {
    if (cmd.equals(CSTR_TARGET)) {
      if (target_chooser == null) {
        target_chooser = new CollectChooser(AnChooser.TARGET_CHOOSER);
      }
      target_chooser.setDialogTitle(cmd);
      target_chooser.getFile(cmd);
    } else if (cmd.equals(CSTR_GROUP)) {
      if (exp_group_chooser == null) {
        exp_group_chooser = new CollectChooser(AnChooser.EXP_GROUP_CHOOSER);
      }
      exp_group_chooser.setDialogTitle(cmd);
      exp_group_chooser.getFile(cmd);
    } else if (cmd.equals(CSTR_DIR) || cmd.equals(CSTR_WORK) || cmd.equals(CSTR_TARGET_JDK)) {
      if (dir_chooser == null) {
        dir_chooser = new CollectChooser(AnChooser.DIR_CHOOSER);
      }
      dir_chooser.setDialogTitle(cmd);
      dir_chooser.getFile(cmd);
    }
  }

  private boolean checkSignals() {
    if (pause_sig.getValue() != null) {
      if (!pause_sig.getValue().equals(ipc_str_off)) {
        if (AnUtility.getSignalValue(pause_sig.getValue()) == -1) {
          AnUtility.showMessage(
              work_panel,
              AnLocale.getString("Illegal pause signal: ") + pause_sig.getValue(),
              JOptionPane.ERROR_MESSAGE);
          return false;
        }
      }
    }
    if (sample_sig.getValue() != null) {
      if (!sample_sig.getValue().equals(ipc_str_off)) {
        if (AnUtility.getSignalValue(sample_sig.getValue()) == -1) {
          AnUtility.showMessage(
              work_panel,
              AnLocale.getString("Illegal sample signal: ") + sample_sig.getValue(),
              JOptionPane.ERROR_MESSAGE);
          return false;
        }
      }
    }
    return true;
  }

  /**
   * checks java version used and warn user if its earlier then 1.4.2_02 TODO: Don't use AWT thread!
   * Create a special thread to call this method
   */
  // NM    public void checkJavaVersion() {
  //        boolean check = false;
  //        String line;
  //        String version_str = "unknown";
  //        int ind;
  //
  //        try {
  //            final String[] javaversion = {getJavaStr(), "-version"};
  //            final Process p = Runtime.getRuntime().exec(javaversion);
  //            p.waitFor();
  //            final InputStream data = p.getErrorStream();
  //            final BufferedReader br = new BufferedReader(new InputStreamReader(data));
  //
  //            while ((line = br.readLine()) != null) {
  //                ind = line.indexOf("java version");
  //                if (ind != -1) {
  //                    version_str = (line.substring(ind + 13, line.length())).trim();
  //                    break;
  //                }
  //            }
  //            writeln(AnLocale.getString("Using java version ") + version_str + " ...",
  // getOutLog());
  //            version_str = version_str.substring(1, version_str.length() - 1); // skip quotes
  //            ind = version_str.lastIndexOf('-');
  //            if (ind != -1) // cut -rc, -beta etc. from the end of the version string
  //            {
  //                version_str = version_str.substring(0, ind);
  //            }
  //
  //            ind = version_str.indexOf('.');
  //            if (ind != -1) {
  //                String ver = version_str.substring(0, ind); // major version
  //                int v = Integer.parseInt(ver);
  //                if (v == 1) { // 1.?.?
  //                    int ind_next = version_str.indexOf('.', ind + 1);
  //                    ver = version_str.substring(ind + 1, ind_next);
  //                    ind = ind_next;
  //                    v = Integer.parseInt(ver);
  //                    if (v == 4) { // 1.4.?
  //                        ind_next = version_str.indexOf('_', ind + 1);
  //                        if (ind_next != -1) {
  //                            ver = version_str.substring(ind + 1, ind_next);
  //                            ind = ind_next;
  //                            v = Integer.parseInt(ver);
  //                            if (v == 2) { // 1.4.2_??
  //                                ver = version_str.substring(ind + 1, version_str.length());
  //                                v = Integer.parseInt(ver);
  //                                if (v >= 2) // 1.4.2_02 or later
  //                                {
  //                                    check = true;
  //                                } else // 1.4.2_01 or earlier
  //                                {
  //                                    check = false;
  //                                }
  //                            } else if (v > 2) // 1.4.3_??
  //                            {
  //                                check = true;
  //                            } else {
  //                                check = false;
  //                            }
  //                        } else {
  //                            ver = version_str.substring(ind + 1, version_str.length());
  //                            v = Integer.parseInt(ver);
  //                            if (v > 2) // 1.4.3 or later
  //                            {
  //                                check = true;
  //                            } else // 1.4.2 or earlier
  //                            {
  //                                check = false;
  //                            }
  //                        }
  //                    } else if (v < 4) // 1.3.*
  //                    {
  //                        check = false;
  //                    } else // 1.5.0 or later
  //                    {
  //                        check = true;
  //                    }
  //                } else if (v > 1) // 2.*.*
  //                {
  //                    check = true;
  //                } else // major version less then 1
  //                {
  //                    check = false;
  //                }
  //            }
  //            data.close(); // close Input Stream
  //        } catch (NumberFormatException e) {
  //            check = false;
  //        } catch (IOException e) {
  //            writeln(e.getMessage(), getOutLog());
  //            check = false;
  //        } catch (InterruptedException e) {
  //            writeln(e.getMessage(), getOutLog());
  //            check = false;
  //        }
  //        if (!check) {
  //            writeln(AnLocale.getString("Warning: Java profiling requires JVM version 1.4.2_02 or
  // later"),
  //                    getOutLog());
  //        }
  //    }
  // NM    public String getJavaStr() {
  //        String java_str = "java";
  //        final String header = target_jdk.getHeader();
  //        String jdk_path;
  //        if (header.equals(AnLocale.getString("Path:")))
  //        {
  //            jdk_path = target_jdk.getText().trim();
  //        } else if (header.equals(AnLocale.getString("Default (PATH based)")))
  //        {
  //            jdk_path = "java";
  //        } else {
  //            jdk_path = target_jdk.getText();
  //        }
  //
  //        if (!jdk_path.equals(ipc_str_empty)) {
  //
  //
  //            if (!jdk_path.equals(java_str)) { // Not Default (PATH based)
  //                if (!jdk_path.endsWith(File.separator)) {
  //                    jdk_path += File.separatorChar;
  //                }
  //                java_str = jdk_path + "bin" + File.separatorChar + java_str;
  //            }
  //        }
  //        return java_str;
  //    }
  private String dbeGetExpParams(final String exp_name) {
    synchronized (IPC.lock) {
      anWindow.IPC().send("dbeGetExpParams");
      anWindow.IPC().send(exp_name);
      return anWindow.IPC().recvString();
    }
  }

  public void loadExpDefaults() {
    if (collector.isProcRunning()) {
      return;
    }

    if (null != work_dir) {
      // Set working directory
      work_dir.setText(Analyzer.getInstance().getWorkingDirectory()); // Still experimental.....
    }
    AnUtility.checkIPCOnWrongThread(false);
    final String[] exp_names = anWindow.getExpName(); // IPC
    AnUtility.checkIPCOnWrongThread(true);

    if (exp_names != null && exp_names.length != 0) {
      // find first non filtered experiment
      AnUtility.checkIPCOnWrongThread(false);
      final boolean[] expstats = anWindow.getExpEnable(); // IPC
      AnUtility.checkIPCOnWrongThread(true);
      int i = 0;
      String str_args = null;

      if (expstats != null) {
        while (i < expstats.length) {
          if (expstats[i]) {
            AnUtility.checkIPCOnWrongThread(false);
            str_args = dbeGetExpParams(exp_names[i]); // IPC
            AnUtility.checkIPCOnWrongThread(true);
            break;
          }
          i++;
        }
      }

      if (str_args != null) {
        final String[] arguments = str_args.split(ipc_str_space);
        final StringBuffer vargs = new StringBuffer();
        final StringBuffer params = new StringBuffer();
        final String trg;
        String trg_jdk = null;

        i = 0;
        if (arguments.length > 1) { // Check for java experiment
          if (new File(arguments[i]).getName().equals("java")) {
            final File jdk_dir = new File(arguments[i]).getParentFile();
            if (jdk_dir != null) {
              trg_jdk = jdk_dir.getParent();
            } else // Default java from $PATH
            {
              trg_jdk = ipc_str_empty;
            }
            i++;
            // skip -Xruncollector (it will be set later in collect)
            if (arguments[i].equals("-Xruncollector")) {
              i++;
            }
            while (arguments[i].startsWith("-") && i < arguments.length) {
              vargs.append(arguments[i] + ipc_str_space);
              if (arguments[i].equals("-cp") || arguments[i].equals("-classpath")) {
                vargs.append(arguments[++i] + ipc_str_space);
              }
              i++;
            }
          }
        }
        trg = arguments[i++];
        while (i < arguments.length) {
          params.append(arguments[i++] + ipc_str_space);
        }
        setComponents(trg, params.toString().trim(), trg_jdk, vargs.toString().trim());
      }
    }
  }

  // Show error mesg
  private void showError(final int err_type) {
    String msg = null;
    switch (err_type) {
      case TARG_UNKNOWN_TYPE:
        msg = AnLocale.getString("Target has unknown file type");
        break;
      case TARG_NOT_EXISTS:
        msg = AnLocale.getString("Target doesnt't exist");
        break;
      case TARG_IS_NOT_FILE:
        msg = AnLocale.getString("Target is not a file");
        break;
      case TARG_IS_NOT_READABLE:
        msg = AnLocale.getString("Target is not readable");
        break;
    }
    AnUtility.showMessage(work_panel, msg, JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    String col_cmd;
    final Iterator iter;
    int i;
    final String cmd = event.getActionCommand();
    final String empty = ipc_str_empty;

    Date date = new Date();
    String sdate = date.toString();
    if (cmd.equals(empty)) {
      int theState = event.getID(); // state change event

      if (theState == Collector.COLLECTING_RUNNING) {
        if (read_file_thread == null && collect_output_file != null) {
          // Create thread to read collect output file
          read_file_thread = new Thread("Read Collector Output Thread") {
            @Override
            public void run() {
              try {
                File f = new File(collect_output_file);
                while (collector.getCollectorState() == Collector.COLLECTING_RUNNING
                    || collector.getCollectorState() == Collector.COLLECTING_PAUSED) {
                  sleep(10);
                  if (f.exists()) {
                    break;
                  }
                }
                if (!f.exists()) {
                  return;
                }
                long len = 0;
                while (f.exists()) {
                  if (f.length() > len) {
                    BufferedReader in = new BufferedReader(new FileReader(f));
                    in.skip(len);
                    String line;
                    while ((line = in.readLine()) != null) {
                      writeln(line, getOutLog());
                    }
                    len = f.length();
                    in.close();
                    collector.read_collect_output_file(collect_output_file);
                  }
                  sleep(10);
                }
              } catch (Exception e) {
                printException(e, AnLocale.getString("Collector: Exeption in Read Collector Output Thread:"));
              }
            }
          };
          read_file_thread.start();
        }
        
        if (buttons[0].isEnabled()) { // the collection has just started
          String current_target_name = target.getText();
          if (!profile_running_process && !system_profiling) {
            current_target_name = new File(current_target_name).getName();
          }
          writeln(
              sdate + ": " + AnLocale.getString("Running: ") + current_target_name, getOutLog());
          set_runtime_buttons();
        } else { // the collection has been resumed
          String p = " (" + AnLocale.getString("process id ") + collector.getProcessPID() + ")";
          writeln(sdate + ": " + AnLocale.getString("Data collection resumed" + p), getOutLog());
          buttons[2].setText(AnLocale.getString("Pause"));
          buttons[2].setActionCommand(AnLocale.getString("Pause"));
        }
      } else if (theState == Collector.COLLECTING_PAUSED) {
        String p = " (" + AnLocale.getString("process id ") + collector.getProcessPID() + ")";
        writeln(sdate + ": " + AnLocale.getString("Data collection paused" + p), getOutLog());
        buttons[2].setText(AnLocale.getString("Resume"));
        buttons[2].setActionCommand(AnLocale.getString("Resume"));

      } else if (theState == Collector.COLLECTING_TERMINATING) {
        writeln(sdate + ": " + AnLocale.getString("Data collection terminating"), getOutLog());

      } else if ((theState == Collector.COLLECTING_TERMINATED)
          || (theState == Collector.COLLECTING_COMPLETED)) {
        read_file_thread = null;
        if (theState == Collector.COLLECTING_TERMINATED) {
          writeln(sdate + ": " + AnLocale.getString("Data collection terminated"), getOutLog());
        }

        if (output_thread != null) {
          // There is a potential problem - we have to make sure that
          // the output thread had enough time to copy all messages
          // from the output Vector to the Text area.
          // Wait till output_thread is still working
          int c = 0;
          while (output_thread != null) {
            if (c++ > 100) {
              break; // timeout 1 second
            }
            try {
              Thread.sleep(10);
            } catch (Exception e) {
              // continue
            }
          }
          output_thread = null;
        }

        collector.removeTempFile(collect_output_file);
        collector.removeTempFile(temp_file_name);

        if (collector.getProcessPID() > 0) {
          writeln(
              sdate
                  + ": "
                  + AnLocale.getString("Execution completed, exit status is ")
                  + collector.getProcessExitValue(),
              getOutLog());
        } else {
          writeln(sdate + ": " + AnLocale.getString("Execution failed"), getOutLog());
        }

        writeln(AnLocale.getString("Process ID: ") + collector.getProcessPID(), getOutLog());
        String actualExperimentName = collector.getActualExpName();
        getBtClear().setEnabled(true);

        if (collector.getProcessPID() > 0) {
          if (actualExperimentName != null) {
            String wd = work_dir.getText();
            final String experimentPath = getExperimentPath(actualExperimentName);
            AnDialog2 openDialog =
                new AnDialog2(m_dialog, m_dialog, AnLocale.getString("Open Experiment"));
            final CollectorOpenPanel collectorOpenPanel =
                new CollectorOpenPanel(experimentPath, wd);
            final String workingDirectory = wd;
            openDialog.setCustomPanel(collectorOpenPanel);
            openDialog.setHelpTag(null);
            openDialog.setVisible(true);
            if (openDialog.getStatus() == AnDialog2.Status.OK) { // Not Cancel
              try {
                // Wait till the Analyzer workspace has been initialized
                SwingUtilities.invokeLater(
                    new Runnable() {
                      @Override
                      public void run() {
                        final List<String> exp_list =
                            AnUtility.getExpList(new String[] {experimentPath});
                        if (anWindow != null) {
                          String configurationPath = collectorOpenPanel.getConfiguration();
                          boolean alwaysUseThisConfiguration =
                              collectorOpenPanel.alwaysUseThisConfiguration();
                          anWindow.loadExperimentList(
                              exp_list,
                              false,
                              workingDirectory,
                              true,
                              configurationPath,
                              alwaysUseThisConfiguration);
                        }
                        // Close Collector dialog
                        resetButtons();
                        m_dialog.setVisible(false);
                      }
                    });
                return; // dialog is closed
              } catch (Exception e) {
                // e.printStackTrace();
                showError(AnLocale.getString("Internal Error: open experiment failed."));
              }
            }
          } else {
            // Report error
            showError(AnLocale.getString("Internal Error: no experiment."));
          }
        } else {
          // Report profiling error
          String err = collector.getErrorMessage();
          if (err == null) {
            err = "\nPlease, look at the Process output window for details";
          }
          showError(AnLocale.getString("Error: Data collection failed: ") + err);
          // Clear old error message
          collector.setErrorMessage(ipc_str_empty);
        }
        resetButtons();
        if (null != exp_name) {
          exp_name.setValue(collector.getNextExpName());
        }
      }

    } else if (cmd.equals(AnLocale.getString("Run"))) {
      if (profile_running_process || system_profiling) {
        // Have to implement: check target, work_dir, exp_dir
      } else { // check target, work_dir, exp_dir
        if (target.getText().length() == 0) {
          tab_pane.setSelectedIndex(0);
          AnUtility.showMessage(
              work_panel,
              AnLocale.getString("The target name cannot be blank"),
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        if ((work_dir.getText() == null) || (work_dir.getText().length() < 1)) {
          work_dir.setValue(Analyzer.getInstance().getWorkingDirectory());
        }
        if (!collector.checkWorkDir(work_dir.getText())) {
          String errorMessage = AnLocale.getString("The working directory does not exist");
          if (null != anWindow.getAnalyzer().remoteConnection) {
            errorMessage =
                AnLocale.getString("The working directory does not exist on remote system");
          }
          AnUtility.showMessage(work_panel, errorMessage, JOptionPane.ERROR_MESSAGE);
          return;
        }
        if (!checkSignals()) {
          return;
        }

        // THIS CHECK IS NOT CORRECT - commented out (NM)
        // if (jvm_prof.isChecked()) {
        //    checkJavaVersion();
        // }
      }
      /* ^profile_running_process */

      // Create commands list and invoke collect
      final ArrayList cmdList = getCmdList();
      final String[] cmds = new String[ /*getCmdList()*/cmdList.size()];
      col_cmd = empty;

      // Define a temporary file for collect's output
      String output_file_name = null;
      if (!profile_running_process) { // 'collect -P' cannot redirect dbx's output (bug 17885089)
        if (!system_profiling) { // 'er_kernel -P' is not implemented yet
          if (!Analyzer.getInstance().isRemote()) { // not remote
            long t = System.currentTimeMillis();
            output_file_name = "/tmp/collect_" + t + "_pid.txt";
          }
        }
        /* ^system_profiling */

      }
      /* ^profile_running_process */

      for (i = 0, iter = /*getCmdList()*/ cmdList.iterator(); iter.hasNext(); i++) {
        cmds[i] = (String) iter.next();
        col_cmd += cmds[i] + ipc_str_space;
        if ((i == 0) && (null != output_file_name)) {
          col_cmd += "--outfile" + ipc_str_space + output_file_name + ipc_str_space;
        }
      }
      // Temporary correction of default HWC set
      if (col_cmd.contains("-h default,")) {
        String pattern = "-h default,";
        int k = col_cmd.indexOf(pattern);
        String s = col_cmd.substring(0, k);
        s += "-h ";
        k += pattern.length();
        s += col_cmd.substring(k, col_cmd.length());
        col_cmd = s;
      }

      preview_cmd.setText(col_cmd);
      tab_pane.setSelectedIndex(2);

      // Create a pipe and read stdout
      try {
        if (profile_running_process
            || system_profiling) { // 'collect -P' cannot redirect dbx's output (bug 17885089)
          // don't use vector, use pipe
          final PipedOutputStream pos = new PipedOutputStream();
          final PipedInputStream pis = new PipedInputStream(pos);
          collector.setLog(pos);
          output_thread =
              new Thread("Collector Panel Output Thread") {
                @Override
                public void run() {
                  output(pis, getOutLog());
                }
              };
        } else {
          final PipedInputStream pis = null;
          final List logVector = new ArrayList();
          collector.setLog(logVector);
          output_thread =
              new Thread("Collector Panel Output Thread") {
                @Override
                public void run() {
                  output(pis, logVector);
                }
              };
        }
        output_thread.start();
      } catch (Exception e) {
        // pis = null;
      }
      String run_cmd = col_cmd;
      String envs = env_vars.getText().trim();
      if (null != envs && envs.length() > 0) {
        run_cmd = envs + " " + col_cmd;
      }
      if (!Analyzer.getInstance().isRemote()) {
        // Create temporary script
        if (temp_file_name == null) {
          try {
            File tempFile = File.createTempFile("collect", ".tmp");
            temp_file_name = tempFile.getCanonicalPath();
          } catch (Exception e) {
            temp_file_name = "/tmp/collect.sh.tmp"; // Use default name
          }
        }
        String contents = "#!/bin/sh\n"
            + "echo \"% cat -n " + temp_file_name + "\"\n"
            + "cat -n " + temp_file_name + "\n"
            + "cd " + work_dir.getText() + "\n"
            + "echo % pwd\npwd\n\n"
            + "echo \"% " + run_cmd + "\"\n"
            + run_cmd + "\n";
        AnUtility.checkIPCOnWrongThread(false);
        anWindow.writeFile(temp_file_name, contents);
        AnUtility.checkIPCOnWrongThread(true);
        run_cmd = temp_file_name;
      }
      // Clean output window
      if (null != pioTextArea) {
        pioTextArea.setText(empty);
      }
      if (null != getOutLog()) {
        getOutLog().setText(empty);
      }
      String experimentGroup = "";
      if (profile_running_process) {
        experimentGroup = collectPanel2.getExperimentGroup();
      } else if (system_profiling) {
        experimentGroup = collectPanel3.getExperimentGroup();
      }
      if (profile_running_process) { // fill fake fields
        target.text.setText(collectPanel2.getProcessID());
        target.setText(run_cmd); // This is used to print what is running
        work_dir.text.setText(collectPanel2.getExperimentDirectory());
        work_dir.setText(collectPanel2.getExperimentDirectory());
        exp_name.text.setText(collectPanel2.getExperimentName());
        env_vars.text.setText(empty);
      }
      if (system_profiling) { // fill fields
        exp_name.text.setText(collectPanel3.getExperimentName());
      }
      collect_output_file = output_file_name;
      collector.collect(
          target.getText(),
          run_cmd,
          work_dir.getText(),
          exp_name.getText(),
          "",
          env_vars.getText().trim(),
          output_file_name,
          experimentGroup);

    } else if (cmd.equals(AnLocale.getString("Pause"))) {
      if (collector.pause(AnUtility.getSignalValue(pause_sig.getValue()))) {}
      tab_pane.setSelectedIndex(2);
    } else if (cmd.equals(AnLocale.getString("Resume"))) {
      if (collector.resume(AnUtility.getSignalValue(pause_sig.getValue()))) {}
      tab_pane.setSelectedIndex(2);
    } else if (cmd.equals(AnLocale.getString("Sample"))) {
      if (collector.sample(AnUtility.getSignalValue(sample_sig.getValue()))) {
        writeln(AnLocale.getString("Manual sample"), getOutLog());
      }
      tab_pane.setSelectedIndex(2);
    } else if (cmd.equals(AnLocale.getString("Terminate"))) {
      final String msg = AnLocale.getString("Do you want to terminate the running experiment?");
      final int user_choice =
          JOptionPane.showConfirmDialog(
              this, msg, AnLocale.getString("Question"), JOptionPane.YES_NO_OPTION);
      if (user_choice == JOptionPane.YES_OPTION) {
        collector.terminate_expt(profile_running_process);
      }
    } else if (cmd.equals(AnLocale.getString("Preview Command:"))) {
      col_cmd = empty;
      for (i = 0, iter = getCmdList().iterator(); iter.hasNext(); i++) {
        col_cmd += ((String) iter.next()) + ipc_str_space;
      }
      // Temporary correction of default HWC set
      String pattern = "-h " + ipc_str_hwc_default + ipc_str_comma;
      if (col_cmd.contains(pattern)) {
        int k = col_cmd.indexOf(pattern);
        String s = col_cmd.substring(0, k);
        s += "-h ";
        k += pattern.length();
        s += col_cmd.substring(k, col_cmd.length());
        col_cmd = s;
      }
      // Temporary correction of default HWC set
      pattern = "-h " + ipc_str_hwc_default + ipc_str_space;
      if (col_cmd.contains(pattern)) {
        int k = col_cmd.indexOf(pattern);
        String s = col_cmd.substring(0, k);
        s += "-h on ";
        k += pattern.length();
        s += col_cmd.substring(k, col_cmd.length());
        col_cmd = s;
      }
      // Remove "-j on"
      pattern = "-j " + ipc_str_on + ipc_str_space;
      if (col_cmd.contains(pattern)) {
        int k = col_cmd.indexOf(pattern);
        String s = col_cmd.substring(0, k);
        k += pattern.length();
        s += col_cmd.substring(k, col_cmd.length());
        col_cmd = s;
      }
      preview_cmd.setText(col_cmd);
    } else if (cmd.equals(AnLocale.getString("Properties"))) {
      toAdd = true;
      HWCEntry entry = (HWCEntry) hwcList.getSelectedValue();
      showHWCPropertyDialog(entry);
    } else if (cmd.equals(AnLocale.getString("Add"))) {
      showHWCSelectDialog();
    } else if (cmd.equals(AnLocale.getString("Remove"))) {
      int selectedIndex = hwcList.getSelectedIndex();
      deleteCtrFromList();
      if (selectedIndex >= (selectedHWCList.size() - 1)) {
        selectedIndex = selectedHWCList.size() - 1;
      }
      if (selectedIndex >= 0) {
        hwcList.setSelectedIndex(selectedIndex);
      }
    } else if (cmd.equals(AnLocale.getString("Add Attribute"))) {
      addAttribute();
    } else if (cmd.equals(AnLocale.getString("Remove Attribute"))) {
      removeAttribute();
      //        } else if (cmd.equals(AnLocale.getString("Update"))) {
      //            int selectedIndex = hwcList.getSelectedIndex();
      //            HWCEntry entry = (HWCEntry) hwcList.getSelectedValue();
      //            updateCtrList(entry);
      //            hwcList.setSelectedIndex(selectedIndex);
      //        } else if (cmd.equals(AnLocale.getString("Cancel"))) {
      //            closeHWCPropertyDialog();
      //        } else if (cmd.equals(AnLocale.getString("Available Counters"))) {
      //            String[] availableCounterText = hwc_help;
      //            int noLines = availableCounterText.length;
      ////            tab_pane.setSelectedIndex(2);
      ////            for (i = 0; i < noLines; i++) {
      ////                write(availableCounterText[i], getOutLog()); // Show available list
      ////            }
      //
      //            AnDialog2 availableCountersDialog = new AnDialog2(this, "Available Counters");
      //            JPanel panel = new JPanel(new BorderLayout());
      //            JScrollPane scrollPane = new JScrollPane();
      //            JTextArea textArea = new JTextArea();
      //            for (i = 0; i < noLines; i++) {
      //                textArea.append(availableCounterText[i]);
      //            }
      //            textArea.setCaretPosition(0);
      //            textArea.setFont(new Font("monospaced", Font.PLAIN,
      // textArea.getFont().getSize()));
      //            scrollPane.setViewportView(textArea);
      //            panel.add(scrollPane, BorderLayout.CENTER);
      //            availableCountersDialog.setCustomPanel(panel);
      //            availableCountersDialog.setModal(false);
      //            availableCountersDialog.getCancelButton().setVisible(false);
      //            availableCountersDialog.setPreferredSize(new
      // Dimension(availableCountersDialog.getPreferredSize().width - 300, 700));
      //            availableCountersDialog.pack();
      //            availableCountersDialog.setVisible(true);
    } else if (cmd.equals(AnLocale.getString("Clear"))) {
      // pioTerm.clearHistory();
      pioTextArea.setText(empty);
      getOutLog().setText(empty);
      getBtClear().setEnabled(false);
    } else {
      showChooser(cmd);
    }
  }

  protected void showAvailableCountersDialog() {
    if (availableCountersDialog == null) {
      String[] availableCounterText = hwc_help;
      int noLines = availableCounterText.length;

      availableCountersDialog = new AnDialog2(m_dialog, this, "Hardware Counter Help");
      JPanel panel = new JPanel(new BorderLayout());
      JScrollPane scrollPane = new JScrollPane();
      JTextArea textArea = new JTextArea();
      for (int i = 0; i < noLines; i++) {
        textArea.append(availableCounterText[i]);
      }
      textArea.setCaretPosition(0);
      textArea.setEditable(false);
      textArea.setFont(new Font("monospaced", Font.PLAIN, textArea.getFont().getSize()));
      scrollPane.setViewportView(textArea);
      panel.add(scrollPane, BorderLayout.CENTER);
      availableCountersDialog.setCustomPanel(panel);
      availableCountersDialog.setModal(false);
      availableCountersDialog.getCancelButton().setVisible(false);
      availableCountersDialog.setPreferredSize(new Dimension(800, 850));
      availableCountersDialog.pack();
    }
    availableCountersDialog.setVisible(true);
  }

  private String getExperimentPath(String actual_expname) {
    String experimentPath = actual_expname;
    if (!actual_expname.startsWith("/")) {
      String path = path = work_dir.getText();
        if (null == path) {
          path = Analyzer.getInstance().getWorkingDirectory();
        }
        path = path.trim();
        if (path.length() > 0) {
          experimentPath = path + "/" + actual_expname;
        } else {
          // BUG: CGetCurDir() may not work properly in remote mode
          path = Analyzer.getInstance().getWorkingDirectory();
          work_dir.setValue(path);
          experimentPath = path + "/" + actual_expname;
        }
    }
    return experimentPath;
  }

  private void setComponents(
      final String target, final String params, final String target_jdk, final String vm_args) {
    //        System.out.println("CollectPanel:setComponents:" + target);
    this.target.setText(target);
    this.args.setText(params);

    if (target_jdk != null) {
      this.jvm_prof.setChecked(true);
      // default java
      if (target_jdk.equals(ipc_str_empty)) {
        this.target_jdk.setSelectedIndex(this.target_jdk.getItemCount() - 1);
      } else {
        final String java_bin =
            target_jdk + File.separatorChar + "bin" + File.separatorChar + "java";
        if (new File(java_bin).exists()) {
          this.target_jdk.setValue(target_jdk);
        } else {
          this.target_jdk.setSelectedIndex(1);
        }
      }
      if (!vm_args.equals(ipc_str_empty)) {
        this.vm_args.setValue(vm_args);
      }
    } else {
      this.jvm_prof.setChecked(false);
    }
    this.updateTitle(target);
  }

  private void showError(String msg) {
    AnUtility.showMessage(work_panel, msg, JOptionPane.ERROR_MESSAGE);
  }

  private String CGetExpName(String str) {
    return Collector.getExpName1(str); // IPC
  }

  public void updateTitle(String target) {
    m_prov.setTitleStr(title + " [" + new File(target).getName() + "]");
  }

  public String getenv(String name) {
    return AnUtility.getenv(name);
  }

  public int getTargetType(String target_name) {
    // TODO: Check java class files to ensure that it has a main public method
    if (target_name == null) {
      return TARG_NOT_EXISTS;
    }
    if (!target_name.startsWith("/")
        && !target_name.startsWith("./")
        && !target_name.startsWith("../")) {
      return TARG_UNKNOWN_TYPE;
    }
    AnFile targ_file = new AnFile(target_name);
    /* NM
    if (!targ_file.exists()) {
    final String target_class_name = toJavaClassFileName(target_name);
    targ_file = new File(target_class_name);
    if (!targ_file.exists()) {
    final String work_str = work_dir.getText();
    targ_file = new File(work_str, target_name);
    if (!targ_file.exists()) {
    targ_file = new File(work_str, target_class_name);
    if (!targ_file.exists() || !targ_file.isFile()) {
    return TARG_NOT_EXISTS;
    }
    }
    }
    }
    */
    if (!targ_file.exists()) {
      return TARG_NOT_EXISTS;
    }
    if (!targ_file.isFile()) {
      return TARG_IS_NOT_FILE;
    }
    /* NM
    switch (AnUtility.getMimeFormat(targ_file)) {
    case AnUtility.MIME_ELF_EXECUTABLE:

    return TARG_ELF_EXECUTABLE;
    case AnUtility.MIME_JAVA_CLASS_FILE:

    return TARG_JAVA_CLASS_FILE;
    case AnUtility.MIME_JAR_FILE:

    return TARG_JAR_FILE;
    case AnUtility.MIME_CANNOT_READ_FILE:

    return TARG_IS_NOT_READABLE;
    default:
    */
    return TARG_UNKNOWN_TYPE;
    /* NM
    }
    */
  }

  // Log panel for process and collector output
  public Component getOutputPane() {

    if (out_log != null) {
      return null;
    }

    out_log = new JTextArea(8, 40);
    out_log.setEditable(false);
    out_log.getAccessibleContext().setAccessibleName(AnLocale.getString("Collector output"));
    out_log.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Collector output"));
    JScrollPane out_scroll =
        new AnJScrollPane(
            out_log,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    out_scroll.setBorder(BorderFactory.createLineBorder(AnEnvironment.SCROLLBAR_BORDER_COLOR));
    out_scroll.getAccessibleContext().setAccessibleName(AnLocale.getString("Collector output"));
    out_scroll
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Collector output"));

    final JPanel out_panel = new JPanel(new BorderLayout());
    JLabel label = new JLabel();
    AnUtility.setTextAndAccessibleContext(label, AnLocale.getString("Collector output"));
    out_panel.add(label, BorderLayout.NORTH);
    out_panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
    out_panel.add(out_scroll, BorderLayout.CENTER);

    final JPanel bt_panel = new JPanel(new BorderLayout());

    bt_clear = new JButton(AnLocale.getString("Clear"));
    bt_clear.setMnemonic(AnLocale.getString('l', "MNEM_COLLECTOR_OUTPUT_CLEAR"));
    bt_clear.setToolTipText(AnLocale.getString("Clear output"));
    bt_clear.setHorizontalTextPosition(SwingConstants.RIGHT);
    bt_clear.setActionCommand(AnLocale.getString("Clear"));
    bt_clear.getAccessibleContext().setAccessibleName(AnLocale.getString("Clear"));
    bt_clear.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Clear output"));

    bt_clear.addActionListener(this);
    bt_clear.setEnabled(false);
    JPanel clearButtonPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(4, 0, 0, 0);
    clearButtonPanel.add(bt_clear, gridBagConstraints);
    bt_panel.add(clearButtonPanel, BorderLayout.EAST);
    out_panel.add(bt_panel, BorderLayout.SOUTH);
    pioTextArea = new JTextArea(8, 40);
    if (!system_profiling) {
      pioTextArea.setEditable(false);
      pioTextArea.getAccessibleContext().setAccessibleName(AnLocale.getString("Process output"));
      pioTextArea
          .getAccessibleContext()
          .setAccessibleDescription(AnLocale.getString("Process output"));
      JScrollPane pio_scroll =
          new AnJScrollPane(
              pioTextArea,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      pio_scroll.setBorder(BorderFactory.createLineBorder(AnEnvironment.SCROLLBAR_BORDER_COLOR));
      pioPanel = new JPanel(new BorderLayout());
      label = new JLabel();
      AnUtility.setTextAndAccessibleContext(label, AnLocale.getString("Process output"));
      pioPanel.add(label, BorderLayout.NORTH);
      pioPanel.getAccessibleContext().setAccessibleName(AnLocale.getString("Process input/output"));
      pioPanel
          .getAccessibleContext()
          .setAccessibleDescription(AnLocale.getString("Process input/output"));
      pioPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
      pioPanel.add(pio_scroll, BorderLayout.CENTER);
    } else {
      return out_panel;
    }

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, out_panel, pioPanel);
    splitPane.setDividerSize(3);
    return (splitPane);
  }

  private void initButtons() {
    buttons[0].setText(AnLocale.getString("Run"));
    buttons[0].setActionCommand(AnLocale.getString("Run"));
    //        buttons[0].setMnemonic(AnLocale.getString('R', "MNEM_COLLECTOR_RUN"));
    buttons[0].getAccessibleContext().setAccessibleDescription(AnLocale.getString("Run"));

    buttons[1].setText(AnLocale.getString("Terminate"));
    buttons[1].setActionCommand(AnLocale.getString("Terminate"));
    buttons[1].setMnemonic(AnLocale.getString('e', "MNEM_COLLECTOR_TERMINATE"));
    buttons[1].getAccessibleContext().setAccessibleDescription(AnLocale.getString("Terminate"));

    // Set size of pause/resume button
    final Dimension pausePrefSize = buttons[2].getPreferredSize();
    final JButton resumebutton = new JButton(AnLocale.getString("Resume"));
    final Dimension resumePrefSize = resumebutton.getPreferredSize();
    final Dimension prPrefSize =
        new Dimension(
            Math.max(pausePrefSize.width, resumePrefSize.width),
            Math.max(pausePrefSize.height, resumePrefSize.height));
    buttons[2].setPreferredSize(prPrefSize);
    buttons[2].setText(aux[0]); // Pause
    buttons[2].setMnemonic(mnemonic[0]);
    buttons[2].getAccessibleContext().setAccessibleDescription(aux[0]);

    buttons[3].setText(aux[1]); // Sample
    buttons[3].setMnemonic(mnemonic[1]);
    buttons[3].getAccessibleContext().setAccessibleDescription(aux[1]);

    buttons[4].setText(AnLocale.getString("Close", "DIALOG_CLOSE"));
    buttons[4].setMnemonic(AnLocale.getString('C', "MNEM_DIALOG_CLOSE"));
    buttons[4]
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Close", "DIALOG_CLOSE"));

    resetButtons();
  }

  // Reset button states
  private void resetButtons() {
    buttons[0].setEnabled(true);
    buttons[1].setEnabled(false);
    buttons[2].setEnabled(false);
    buttons[2].setText(AnLocale.getString("Pause"));
    buttons[2].setActionCommand(AnLocale.getString("Pause"));
    buttons[3].setEnabled(false);
    setAutoUpdaterState(1); // resume
  }

  public void set_runtime_buttons() {
    setAutoUpdaterState(0); // pause
    buttons[0].setEnabled(false); // run button disabled
    buttons[1].setEnabled(true); // terminate button enabled
    // pause button
    if ((null == pause_sig.getValue()) || pause_sig.getValue().equals(ipc_str_off)) {
      buttons[2].setEnabled(false);
    } else {
      buttons[2].setEnabled(true);
      if (startPaused.isSelected()) {
        buttons[2].setText(AnLocale.getString("Resume"));
        buttons[2].setActionCommand(AnLocale.getString("Resume"));
      }
    }
    // sample button
    if ((null == sample_sig.getValue()) || sample_sig.getValue().equals(ipc_str_off)) {
      buttons[3].setEnabled(false);
    } else {
      buttons[3].setEnabled(true);
    }
  }

  private String[][] getHwcSets(boolean forKernel) {
    synchronized (IPC.lock) {
      anWindow.IPC().send("getHwcSets");
      anWindow.IPC().send(forKernel);
      return (String[][]) anWindow.IPC().recvObject();
    }
  }

  private String[] getHwcAttrList(boolean forKernel) {
    synchronized (IPC.lock) {
      anWindow.IPC().send("getHwcAttrList");
      anWindow.IPC().send(forKernel);
      return (String[]) anWindow.IPC().recvObject();
    }
  }

  private String[] getHwcHelp(boolean forKernel) {
    synchronized (IPC.lock) {
      anWindow.IPC().send("getHwcHelp");
      anWindow.IPC().send(forKernel);
      return (String[]) anWindow.IPC().recvObject();
    }
  }

  private Object[] getHwcsAll(boolean forKernel) {
    synchronized (IPC.lock) {
      anWindow.IPC().send("getHwcsAll");
      anWindow.IPC().send(forKernel);
      return (Object[]) anWindow.IPC().recvObject();
    }
  }

  private int getHwcMaxConcurrent(boolean forKernel) {
    synchronized (IPC.lock) {
      anWindow.IPC().send("getHwcMaxConcurrent");
      anWindow.IPC().send(forKernel);
      return anWindow.IPC().recvInt();
    }
  }

  private List<List<HWCEntry>> getHWCsAllProcess(Object[] obj) {
    List<List<HWCEntry>> types = new ArrayList();
    List<HWCEntry> std = new ArrayList();
    List<HWCEntry> raw = new ArrayList();
    types.add(std);
    types.add(raw);
    int ii = 0;
    boolean recommended = true;
    for (List<HWCEntry> current : types) {
      Object fields[] = (Object[]) obj[ii];
      int xx = 0;
      final String[] i18n = (String[]) fields[xx++];
      final String[] name = (String[]) fields[xx++];
      final String[] int_name = (String[]) fields[xx++];
      final String[] metric = (String[]) fields[xx++];
      final long[] val = (long[]) fields[xx++];
      final int[] timecvt = (int[]) fields[xx++];
      final int[] memop = (int[]) fields[xx++];
      final String[] short_desc = (String[]) fields[xx++];
      final int[][] reg_list = (int[][]) fields[xx++];
      final boolean[] supportsAttrs = (boolean[]) fields[xx++];
      final boolean[] supportsMemspace = (boolean[]) fields[xx++];
      for (int kk = 0; kk < i18n.length; kk++) {
        HWCEntry hwcEntry =
            new HWCEntry(
                false,
                i18n[kk],
                name[kk],
                int_name[kk],
                metric[kk],
                val[kk],
                timecvt[kk],
                memop[kk],
                short_desc[kk],
                reg_list[kk],
                supportsAttrs[kk],
                supportsMemspace[kk],
                recommended);
        current.add(hwcEntry);
      }
      ii++;
      recommended = false;
    }
    return types;
  }

  // Initialize register data for selected counter
  private String[][] setRegData(String hwname) {
    final HWCEntry ctr = hwcI18NMap.get(hwname);
    final int ct;
    if (ctr != null && ctr.getRegList() != null) {
      ct = ctr.getRegList().length;
    } else {
      ct = 0;
    }
    int count = ct + 1;
    String[][] rdata = new String[2][count];
    rdata[0][0] = AnLocale.getString("None");
    rdata[1][0] = rdata[0][0];

    for (int i = 1; i < count; i++) {
      rdata[0][i] = Integer.toString(ctr.getRegList()[i - 1]);
      rdata[1][i] = rdata[0][i];
    }

    // Set combo box with the new set of registers
    if ((reg_alloc != null) && (reg_alloc.combo != null)) {
      reg_alloc.combo.setSelectedItem(AnLocale.getString("None"));
      int n = 1;
      int max = reg_alloc.combo.getItemCount();
      while (n < max) {
        reg_alloc.combo.removeItemAt(1);
        n++;
      }
      for (int k = 1; k < count; k++) {
        reg_alloc.combo.addItem(rdata[0][k]);
      }
    }
    return rdata;
  }

  public JPanel getHwcPanel(HWCEntry entry) {
    JPanel basePanel = new JPanel();
    basePanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    basePanel.setLayout(new BoxLayout(basePanel, BoxLayout.Y_AXIS));
    // Create top panel
    JPanel hwcPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    hwcPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    hwcPanel.setLayout(new BoxLayout(hwcPanel, BoxLayout.Y_AXIS));
    hwcPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    hwcPanel
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Counter Selection"));

    JPanel labelPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    labelPanel.setOpaque(false);
    gridBagConstraints.gridx = 0;
    gridBagConstraints.insets = new Insets(8, 8, 0, 8);
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    JLabel counterLabel = new JLabel(entry.getMetricText());
    AnUtility.setAccessibleContext(counterLabel);
    counterLabel.setFont(counterLabel.getFont().deriveFont(Font.BOLD));
    counterLabel.setOpaque(true);
    counterLabel.setPreferredSize(new Dimension(600, 20));
    counterLabel.setHorizontalAlignment(JLabel.LEFT);
    labelPanel.add(counterLabel, gridBagConstraints);
    hwcPanel.add(labelPanel);

    hwc2_prof =
        new CollectData(
            hwc1_prof,
            '0',
            null,
            ' ',
            AnLocale.getString("Counter Name:"),
            hwc_name_data,
            JTextField.LEFT,
            true,
            true,
            ipc_str_hwc2_prof);

    hwcPanel.add(Box.createRigidArea(new Dimension(8, 8)));
    hwcPanel.add(
        hwc1_val =
            new CollectData(
                hwc2_prof,
                '0',
                null,
                ' ',
                AnLocale.getString("Profiling Rate:"),
                hwc_data,
                JTextField.LEFT,
                true,
                true,
                ipc_str_hwc1_val));

    dspace_hwc1 =
        new CollectData(
            hwc1_val,
            '0',
            null,
            ' ',
            AnLocale.getString("Hardware Counter Dataspace :"),
            hwc_dsp_data,
            JTextField.LEFT,
            true,
            false,
            ipc_str_dspace_hwc1);

    hwcPanel.add(Box.createRigidArea(new Dimension(8, 8)));
    if (!hwcFlatList.isEmpty()) {
      HWCEntry first = hwcFlatList.get(0);
      hwc_reg_data = setRegData(first.getI18n());
    }
    hwcPanel.add(
        reg_alloc =
            new CollectData(
                hwc1_val,
                '0',
                null,
                ' ',
                AnLocale.getString("Hardware Counter Register:"),
                hwc_reg_data,
                JTextField.LEFT,
                true,
                false,
                ipc_str_hwc_reg_alloc));
    hwcPanel.add(Box.createRigidArea(new Dimension(8, 8)));
    hwcPanel.add(
        ctr_attr =
            new CollectData(
                hwc1_val,
                '0',
                null,
                ' ',
                AnLocale.getString("Attribute:"),
                hwc_att_data,
                JTextField.LEFT,
                true,
                false,
                ipc_str_hwc_ctr_attr));

    // Attr Buttons
    JPanel attrButtonPanel1 = new JPanel(new GridBagLayout());
    gridBagConstraints = new GridBagConstraints();
    attrButtonPanel1.setOpaque(false);
    addAttr = new JButton(AnLocale.getString("Add Attribute"));
    addAttr.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Add Attribute"));
    addAttr.addActionListener(this);
    addAttr.setEnabled(false);
    gridBagConstraints.gridx = 0;
    gridBagConstraints.insets = new Insets(4, 8, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    attrButtonPanel1.add(addAttr, gridBagConstraints);
    modAttr = new JButton(AnLocale.getString("Modify Attribute"));
    addAttr.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Modify Attribute"));
    modAttr.addActionListener(this);
    hwcPanel.add(attrButtonPanel1);

    hwcPanel.add(Box.createRigidArea(new Dimension(16, 16)));

    // Create attribute config panel
    JPanel attrPanel = new JPanel(new BorderLayout());
    attrPanel.setBorder(new EmptyBorder(new Insets(1, 8, 1, 8)));
    attrPanel.setOpaque(false);

    // Selected attr label
    selectedAttrLabel = new JLabel(AnLocale.getString("Selected Attributes:"));
    selectedAttrLabel.setOpaque(false);
    AnUtility.setAccessibleContext(
        selectedAttrLabel.getAccessibleContext(), selectedAttrLabel.getText());
    selectedAttrLabel.setAlignmentX(JDialog.LEFT_ALIGNMENT);
    attrPanel.add(selectedAttrLabel, BorderLayout.NORTH);

    // Selected list
    sel_attr = new ArrayList();
    attrList = new JList(sel_attr.toArray());
    attrList.getAccessibleContext().setAccessibleName(AnLocale.getString("Attribute Selection"));
    attrList
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Attribute Selection"));
    // list.addListSelectionListener(this);
    attrScrollPane = new AnJScrollPane();
    attrScrollPane.setBorder(BorderFactory.createLineBorder(AnEnvironment.SCROLLBAR_BORDER_COLOR));
    attrScrollPane.setViewportView(attrList);
    Dimension newSize = new Dimension(hwcPanel.getPreferredSize().width - 600, 50);
    attrScrollPane.setPreferredSize(newSize);
    attrPanel.add(attrScrollPane, BorderLayout.CENTER);
    hwcPanel.add(attrPanel);

    // Attr Buttons
    JPanel attrButtonPanel2 = new JPanel(new GridBagLayout());
    gridBagConstraints = new GridBagConstraints();
    attrButtonPanel2.setOpaque(false);
    remAttr = new JButton(AnLocale.getString("Remove Attribute"));
    AnUtility.setAccessibleContext(remAttr.getAccessibleContext(), remAttr.getText());
    //        remAttr.setMnemonic(getLocaleStr('R', "MNEM_COLLECTOR_DELETE_ATTRIBUTE"));
    addAttr.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Remove Attribute"));
    remAttr.addActionListener(this);
    gridBagConstraints.gridx = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(4, 8, 0, 0);
    attrButtonPanel2.add(remAttr, gridBagConstraints);
    hwcPanel.add(attrButtonPanel2);

    hwcPanel.add(Box.createRigidArea(new Dimension(8, 8)));
    setAttrButtons();
    setAttributeCombo();

    // Create and initialize dialog buttons.
    buttonPanel = new JPanel();
    updateButton = new JButton(AnLocale.getString("Update"));
    updateButton.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Update"));
    updateButton.addActionListener(this);
    cancelButton = new JButton(AnLocale.getString("Cancel"));
    cancelButton.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Cancel"));
    cancelButton.addActionListener(this);
    buttonPanel.add(updateButton);
    buttonPanel.add(cancelButton);

    basePanel.add(hwcPanel);

    // Set dialog to selected counter
    hwc2_prof.combo.setSelectedItem(entry.getI18n());
    return basePanel;
  }

  // Initialize GUI components
  private void initComponents() {
    final JPanel exp_panel, data_panel, preview, cmd_panel;
    final CollectUtility.CList list, list_dbx, list_jdk;
    final JButton button;
    final JPanel targetPanel = new JPanel();
    final JPanel firstPanel = new JPanel();
    tab_pane = new JTabbedPane();
    AnUtility.setAccessibleContext(tab_pane.getAccessibleContext(), title);

    // Add target & arguments
    list = new CollectUtility.CList(false);
    list_dbx = new CollectUtility.CList(false); // hidden list
    if (!profile_running_process) {
      CollectUtility.CList list_target = new CollectUtility.CList(false);
      boolean required = true;
      if (system_profiling) {
        required = false;
      }
      target =
          new CollectExp(
              CEXP_TARGET,
              list_target,
              ipc_str_empty,
              AnLocale.getString('T', "MNEM_COLLECTOR_TARGET"),
              CSTR_TARGET,
              true,
              '4',
              required);
      target.text.setToolTipText(AnLocale.getString("The path name of the target program"));
      args =
          new CollectExp(
              CEXP_NA,
              list_target,
              ipc_str_empty,
              AnLocale.getString('A', "MNEM_COLLECTOR_ARGUMENTS"),
              AnLocale.getString("Arguments"),
              false,
              '0');
      args.text.setToolTipText(AnLocale.getString("Target program's command-line arguments"));
      work_dir =
          new CollectExp(
              CEXP_WORK,
              list_target,
              Analyzer.getInstance().getWorkingDirectory(),
              AnLocale.getString('W', "MNEM_COLLECTOR_WORKING_DIR"),
              CSTR_WORK,
              true,
              '7');
      work_dir.text.setToolTipText(
          AnLocale.getString("Full path of the directory where the target program should run"));
      work_dir.setEditable(true);
      env_vars =
          new CollectExp(
              CEXP_ENV,
              list_target,
              ipc_str_empty,
              AnLocale.getString('V', "MNEM_COLLECTOR_ENVIRONMENT_VARIABLES"),
              AnLocale.getString("Environment Variables"),
              false,
              '0');
      env_vars.text.setToolTipText(
          AnLocale.getString(
              "Environment variables needed by target program, specified as: NAME1=value1"
                  + " NAME2=value2"));
      list_target.setAlignmentX();
      list_target.setAlignmentY();

      TitledBorder titleBorder;
      titleBorder =
          BorderFactory.createTitledBorder(
              new LineBorder(AnEnvironment.ABOUT_BOX_BORDER_COLOR, 1),
              AnLocale.getString("Specify Application to Profile"));
      // titleBorder.setTitleFont(titlefont);
      targetPanel.setBorder(titleBorder);
      targetPanel.setLayout(new BoxLayout(targetPanel, BoxLayout.Y_AXIS));
      targetPanel.add(list_target);
    }
    if (!profile_running_process && !system_profiling) {
      // Add experiment name/directory/group
      exp_name =
          new CollectExp(
              CEXP_NAME,
              list,
              CGetExpName(null),
              AnLocale.getString('x', "MNEM_COLLECTOR_EXPERIMENT_NAME"),
              AnLocale.getString("Experiment Name"),
              false,
              '0');
      exp_name.text.setToolTipText(
          AnLocale.getString("File name ending in .er to store collected data as an experiment"));
    } else {
      if (profile_running_process) {
        // AnLog.log("analyzer: profiling running process.\n");
        targetPanel.setLayout(new BorderLayout());
        collectPanel2 = new CollectPanel2(anWindow);
        targetPanel.add(collectPanel2, BorderLayout.CENTER);
      } else { // system_profiling
        // AnLog.log("analyzer: system profiling.\n");
        collectPanel3 = new CollectPanel3(anWindow);
        targetPanel.add(collectPanel3);
      }
      if (profile_running_process) {
        target =
            new CollectExp(
                CEXP_TARGET,
                list_dbx,
                ipc_str_empty,
                AnLocale.getString('T', "MNEM_COLLECTOR_TARGET"),
                CSTR_TARGET,
                true,
                '4',
                true);
        work_dir =
            new CollectExp(
                CEXP_WORK,
                list_dbx,
                Analyzer.getInstance().getWorkingDirectory(),
                AnLocale.getString('W', "MNEM_COLLECTOR_WORKING_DIR"),
                CSTR_WORK,
                false,
                '7');
        env_vars =
            new CollectExp(
                CEXP_ENV,
                list_dbx,
                ipc_str_empty,
                AnLocale.getString('V', "MNEM_COLLECTOR_ENVIRONMENT_VARIABLES"),
                AnLocale.getString("Environment Variables"),
                false,
                '0');
      }
      exp_name =
          new CollectExp(
              CEXP_NAME,
              list_dbx,
              CGetExpName(null),
              AnLocale.getString('x', "MNEM_COLLECTOR_EXPERIMENT_NAME"),
              AnLocale.getString("Experiment Name"),
              false,
              '0');
    }
    list.setAlignmentX();
    list.setAlignmentY();

    firstPanel.setLayout(new BorderLayout());
    firstPanel.add(targetPanel, BorderLayout.NORTH);
    exp_panel = new JPanel();
    exp_panel.setLayout(new BoxLayout(exp_panel, BoxLayout.Y_AXIS));
    // exp_panel.add(target_panel);
    TitledBorder titleBorder;
    titleBorder =
        BorderFactory.createTitledBorder(
            new LineBorder(AnEnvironment.ABOUT_BOX_BORDER_COLOR, 1),
            AnLocale.getString("Specify Experiment"));
    // titleBorder.setTitleFont(titlefont);
    exp_panel.setBorder(titleBorder);
    exp_panel.add(list);

    JPanel exp_panel2 = new JPanel();
    exp_panel2.setLayout(new BoxLayout(exp_panel2, BoxLayout.Y_AXIS));
    // exp_panel.add(target_panel);
    TitledBorder titleBorder2;
    titleBorder2 =
        BorderFactory.createTitledBorder(
            new LineBorder(AnEnvironment.ABOUT_BOX_BORDER_COLOR, 1),
            AnLocale.getString("Advanced Experiment Settings"));
    // titleBorder2.setTitleFont(titlefont);
    exp_panel2.setBorder(titleBorder2);

    exp_panel2.add(
        exp_limit =
            new CollectData(
                true,
                ' ',
                null,
                '0',
                AnLocale.getString("Data Limit (MB):"),
                limit_data,
                JTextField.RIGHT,
                false,
                ipc_str_exp_limit));
    exp_limit.text.setToolTipText(
        AnLocale.getString("Profile the application until the experiment reaches this size"));
    left_list.add(exp_limit);

    exp_panel2.add(
        time_limit =
            new CollectData(
                true,
                ' ',
                null,
                '0',
                AnLocale.getString("Time Limit (Seconds):"),
                limit_time,
                JTextField.RIGHT,
                false,
                ipc_str_time_limit));
    time_limit.text.setToolTipText(
        AnLocale.getString("Profile the application for this amount of time"));
    left_list.add(time_limit);

    exp_panel2.add(Box.createRigidArea(new Dimension(4, 4)));
    exp_panel2.add(
        arch_exp =
            new CollectData(
                true,
                ' ',
                // NM null, '0', getLocaleStr("Archived/Copied into Experiment:"), arch_data,
                // JTextField.RIGHT, false));
                null,
                '0',
                AnLocale.getString("Archive Mode:"),
                arch_data,
                JTextField.RIGHT,
                false,
                ipc_str_arch_exp));
    arch_exp.text.setToolTipText(
        AnLocale.getString("Archive the experiment for analysis on another system"));
    exp_panel2.add(Box.createRigidArea(new Dimension(4, 4)));
    left_list.add(arch_exp);

    if (!profile_running_process) {
      exp_panel2.add(
          descendant =
              new CollectData(
                  true,
                  ' ',
                  null,
                  '0',
                  AnLocale.getString("Follow Descendant Processes:"),
                  desc_data,
                  JTextField.RIGHT,
                  false,
                  ipc_str_descendant));
      descendant.text.setToolTipText(
          AnLocale.getString("Profile the target program and child processes it starts"));
      exp_panel2.add(Box.createRigidArea(new Dimension(4, 4)));
      left_list.add(descendant);
    }
    if (system_profiling) {
      exp_panel2.add(
          prof_idle =
              new CollectData(
                  true,
                  ' ',
                  null,
                  '0',
                  AnLocale.getString("Do not profile idle CPUs:"),
                  prof_idle_data,
                  JTextField.RIGHT,
                  false,
                  ipc_str_prof_idle));
      prof_idle.text.setToolTipText(
          AnLocale.getString("Do not record profile events from idle CPUs"));
      exp_panel2.add(Box.createRigidArea(new Dimension(4, 4)));
      left_list.add(prof_idle);
    }

    pause_sig =
        new CollectData(
            true,
            ' ',
            null,
            '0',
            AnLocale.getString("Signal to Pause/Resume Collection:"),
            sig_data,
            JTextField.RIGHT,
            false,
            ipc_str_pause_resume_sig);
    pause_sig.text.setToolTipText(
        AnLocale.getString("Signal you will use to pause and resume profiling the application"));
    left_list.add(pause_sig);
    exp_panel2.add(pause_sig);

    final ButtonGroup group = new ButtonGroup();
    group.add(startPaused = new JRadioButton(AnLocale.getString("Paused"), true));
    //        startPaused.setMnemonic(getLocaleStr('d', "MNEM_COLLECTOR_START_PAUSED"));
    startPaused.setToolTipText(
        AnLocale.getString("Begin profiling the application when you send the first signal"));
    group.add(startResumed = new JRadioButton(AnLocale.getString("Resumed"), false));
    //        startResumed.setMnemonic(getLocaleStr('m', "MNEM_COLLECTOR_START_RESUMED"));
    startResumed.setToolTipText(
        AnLocale.getString(
            "Profile the application immediately as its process starts and pause when you send the"
                + " first signal"));
    final JPanel bg_panel = new JPanel();
    start_state = new JLabel(AnLocale.getString("Start state: "));
    start_state.setToolTipText(
        AnLocale.getString(
            "Whenever the signal is delivered to the process, the Collector switches between paused"
                + " (no data is recorded) and resumed (data is recorded) states"));
    bg_panel.add(start_state);
    bg_panel.add(startPaused);
    bg_panel.add(startResumed);

    start_state.setEnabled(false);
    startPaused.setEnabled(false);
    startResumed.setEnabled(false);
    pause_sig.combo.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(final ItemEvent event) {
            final boolean selected = !event.getItem().equals(AnLocale.getString("Off"));
            start_state.setEnabled(selected);
            startPaused.setEnabled(selected);
            startResumed.setEnabled(selected);
          }
        });

    exp_panel2.add(bg_panel);
    exp_panel.add(new JLabel(ipc_str_space)); // just space
    exp_panel.add(exp_panel2);
    pause_sig.setAlignmentX(left_list);

    sample_sig =
        new CollectData(
            /* NM true */
            /* NM true */ false,
            ' ',
            AnLocale.getString("Manual Samples of Process Resource Utilization"),
            ' ',
            AnLocale.getString("Signal:"),
            sig_data,
            JTextField.RIGHT,
            true,
            ipc_str_sample_sig);
    sample_sig.check.setToolTipText(
        AnLocale.getString("Time-stamped samples taken when you send a signal to the program"));
    sample_sig.text.setToolTipText(
        AnLocale.getString("Signal you will send to take a manual sample"));

    // Add data to collect
    data_panel = new JPanel();
    data_panel.setLayout(new BoxLayout(data_panel, BoxLayout.Y_AXIS));
    // NM data_panel.setBorder(new javax.swing.border.EmptyBorder(5, 5, 0, 0));
    TitledBorder dataBorder;
    String dataTitale =
        AnLocale.getString(
            "Specify the type of data to collect.  As you select data types, other incompatible"
                + " data types are disabled.");
    dataBorder = BorderFactory.createTitledBorder(new LineBorder(Color.BLUE, 0), dataTitale);
    // dataBorder.setTitleFont(titlefont);
    data_panel.setBorder(dataBorder);
    data_panel
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Data to Collect"));
    data_panel.add(Box.createRigidArea(new Dimension(4, 4))); // add some space

    data_panel.add(
        clock_prof =
            new CollectData(
                clkprof_on_by_default,
                ' ',
                AnLocale.getString("Clock Profiling"),
                ' ',
                AnLocale.getString("Profiling Rate:"),
                clock_data,
                JTextField.RIGHT,
                true,
                ipc_str_clkprof));
    clock_prof.check.setToolTipText(
        AnLocale.getString(
            "Record application state at regular intervals to show where time is spent in"
                + " functions"));
    clock_prof.text.setToolTipText(
        AnLocale.getString("Time between Clock Profiling recordings (milliseconds)"));

    data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
    data_panel.add(
        hwc1_prof =
            new CollectData(
                hwcprof_on_by_default,
                ' ',
                AnLocale.getString("Hardware Counter Profiling"),
                '0',
                null,
                null,
                JTextField.LEFT,
                true,
                ipc_str_hwcprof));
    hwc1_prof.check.setToolTipText(
        AnLocale.getString("Track the frequency of processor events specified by counters"));
    if (hwcFlatList.isEmpty()) {
      hwc1_prof.check.setEnabled(false);
    }
    data_panel.add(Box.createRigidArea(new Dimension(4, 4)));

    initHWCPanel(data_panel);

    final boolean hwc1_available = hwc1_prof.check.isEnabled();
    hwc1_prof.check.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(final ItemEvent event) {
            final boolean selected = ((JCheckBox) event.getItem()).isSelected();
            if (hwc1_available) {
              counterLabel.setEnabled(selected);
              propertyButton.setEnabled(selected);
              addHWCButton.setEnabled(selected);
              hwcList.setEnabled(selected);
              //                    showButton.setEnabled(selected);
              updateButtons();
            }
          }
        });

    jvm_prof =
        new CollectData(
            javaprof_on_by_default,
            ' ',
            AnLocale.getString("Java Profiling"),
            '0',
            null,
            null,
            JTextField.RIGHT,
            true /* NM false */,
            ipc_str_javaprof);
    jvm_prof.check.setToolTipText(
        AnLocale.getString("Enable recognition and recording of Java classes and methods"));
    if (!system_profiling) {
      data_panel.add(jvm_prof);
    }
    data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
    data_panel.add(
        sample =
            new CollectData(
                sample_on_by_default,
                ' ',
                AnLocale.getString("Periodic Samples of Process Resource Utilization"),
                ' ',
                AnLocale.getString("Interval (sec.):"),
                samp_data,
                JTextField.RIGHT,
                true,
                ipc_str_sample));
    sample.check.setToolTipText(
        AnLocale.getString(
            "Time-stamped samples that show execution state of the profiled program, used in"
                + " Timeline"));
    sample.text.setToolTipText(
        AnLocale.getString("Number of seconds between samples, normally 1 second"));

    if (null != sample_sig) {
      data_panel.add(Box.createRigidArea(new Dimension(4, 4))); // new box
      data_panel.add(sample_sig);
    }

    data_panel.add(Box.createRigidArea(new Dimension(4, 4)));

    io_trace =
        new CollectData(
            false,
            ' ',
            AnLocale.getString("I/O Tracing"),
            ' ',
            null,
            null,
            JTextField.RIGHT,
            true,
            ipc_str_iotrace);
    io_trace.check.setToolTipText(AnLocale.getString("Track input/output operations"));
    if (!profile_running_process && !system_profiling) {
      data_panel.add(io_trace);
      data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
    }

    heap_trace =
        new CollectData(
            false,
            ' ',
            AnLocale.getString("Heap Tracing"),
            '0',
            null,
            null,
            JTextField.RIGHT,
            true,
            ipc_str_heaptrace);
    heap_trace.check.setToolTipText(
        AnLocale.getString(
            "Track memory allocation activity to reveal memory leaks or inefficiency"));
    if (!profile_running_process && !system_profiling) {
      data_panel.add(heap_trace);
    }

    sync_trace =
        new CollectData(
            false,
            ' ',
            AnLocale.getString("Synchronization Wait Tracing"),
            ' ',
            AnLocale.getString("Minimum Delay (usec.):"),
            sync_data,
            JTextField.RIGHT,
            true,
            ipc_str_synctrace);
    sync_trace.check.setToolTipText(
        AnLocale.getString(
            "Track delays in calls to thread-synchronization routines in multithreaded programs"));
    sync_trace.text.setToolTipText(
        AnLocale.getString("Threshold for recording a thread synchonization wait (microseconds)"));
    if (!profile_running_process && !system_profiling) {
      data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
      data_panel.add(sync_trace);
      data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
    }

    mpi_trace =
        new CollectData(
            false,
            ' ',
            AnLocale.getString("MPI Tracing for specified MPI"),
            ' ',
            AnLocale.getString("MPI version:"),
            mpi_data,
            JTextField.RIGHT,
            true,
            ipc_str_MPIexpt);
    mpi_trace.check.setToolTipText(
        AnLocale.getString("Track MPI calls for progams that use Message Passing Interface"));
    mpi_trace.text.setToolTipText(AnLocale.getString("The version of MPI your program uses"));
    if (mpi_support() && !profile_running_process && !system_profiling) {
      data_panel.add(mpi_trace);
      data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
    }

    if (!profile_running_process && !system_profiling) {
      list_jdk = new CollectUtility.CList(true);
      // The code below will be removed soon
      target_jdk =
          new TargetJDK(
              list_jdk,
              jvm_prof,
              AnLocale.getString('T', "MNEM_COLLECTOR_TARGET_JDK"),
              CSTR_TARGET_JDK,
              AnLocale.getString('B', "MNEM_COLLECTOR_JDK_BROWSE"));
      vm_args =
          new CollectExp(
              CEXP_NA,
              list_jdk,
              ipc_str_empty,
              AnLocale.getString('V', "MNEM_COLLECTOR_JAVA_VM_PARAMETERS"),
              AnLocale.getString("JVM parameters"),
              false,
              '0',
              0,
              false);
    }

    if (profile_running_process) {
      data_panel.add(
          dbxsample =
              new CollectData(
                  false,
                  ' ',
                  AnLocale.getString("Record Sample When dbx Stops Process"),
                  '0',
                  null,
                  null,
                  JTextField.RIGHT,
                  true,
                  ipc_str_dbxsample));
    }

    // Threading errors checking
    deadlock_trace =
        new CollectData(
            false,
            ' ',
            AnLocale.getString("Deadlock Detection"),
            ' ',
            null,
            null,
            JTextField.RIGHT,
            true,
            ipc_str_thatrace);
    deadlock_trace.check.setToolTipText(
        AnLocale.getString("Detecting dead locks in threads synchronization"));
    data_race =
        new CollectData(
            false,
            ' ',
            AnLocale.getString("Data Race Detection"),
            ' ',
            null,
            null,
            JTextField.RIGHT,
            true,
            ipc_str_thatrace);
    data_race.check.setToolTipText(
        AnLocale.getString(
            "Locate competing data access events in instrumented multithreaded programs"));

    // Function/Instruction Counts
    count_data =
        new CollectData(
            false,
            ' ',
            AnLocale.getString("Function/Instruction Counts"),
            ' ',
            AnLocale.getString("Instrumentation:"),
            count_types,
            JTextField.RIGHT,
            true,
            ipc_str_count);
    count_data.check.setToolTipText(
        AnLocale.getString(
            "Counting Function calls and Instructions. This option cannot be used with other"
                + " profiling options"));

    // if GUI is started as RDT, deselect clock-based profiling by default
    String cmode = AnUtility.getenv("SP_ANALYZER_CONFIG_MODE");
    if (cmode != null && cmode.equals("R")) {
      clock_prof.check.setSelected(false);
    }
    // if GUI is started as RDT, select associated checkboxes by default
    if (cmode != null && cmode.equals("R")) {
      sample.check.setSelected(false);
      sample.check.setEnabled(false);
      data_race.check.setSelected(true);
      deadlock_trace.check.setSelected(true);
    }
    // Check if data_race can be set
    String val = ipc_str_race;
    String err = Collector.setCollectorControlValue(data_race.name, val); // IPC call
    if ((null != err) || profile_running_process || system_profiling) {
      data_race.check.setSelected(false);
      data_race.check.setEnabled(false);
      data_race.available = false;
    } else {
      data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
      data_panel.add(data_race);
    }
    if (!data_race.check.isSelected()) {
      Collector.unsetCollectorControlValue(data_race.name); // IPC call
    }
    // Check if deadlock_trace can be set
    val = ipc_str_deadlock;
    err = Collector.setCollectorControlValue(deadlock_trace.name, val); // IPC call
    if ((null != err) || profile_running_process || system_profiling) {
      deadlock_trace.check.setSelected(false);
      deadlock_trace.check.setEnabled(false);
      deadlock_trace.available = false;
    } else {
      data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
      data_panel.add(deadlock_trace);
    }
    if (!deadlock_trace.check.isSelected()) {
      Collector.unsetCollectorControlValue(deadlock_trace.name); // IPC call
    }
    // Check if count_data can be set
    val = ipc_str_on;
    err = Collector.setCollectorControlValue(count_data.name, val); // IPC call
    if ((null != err) || profile_running_process || system_profiling) {
      count_data.check.setEnabled(false);
      count_data.check.setSelected(false);
      count_data.available = false;
    } else {
      data_panel.add(Box.createRigidArea(new Dimension(4, 4)));
      data_panel.add(count_data);
    }
    Collector.unsetCollectorControlValue(count_data.name); // IPC call

    data_panel.add(Box.createRigidArea(new Dimension(4, 4))); // add some space

    sample.setAlignmentX(data_list);

    if (!profile_running_process && !system_profiling) {
      firstPanel.add(exp_panel);
    } else {
      data_panel.add(exp_panel2);
    }
    tab_pane.add(AnLocale.getString("General"), firstPanel); // NM
    tab_pane.add(AnLocale.getString("Data to Collect"), data_panel);

    cmd_panel = new JPanel(new BorderLayout());
    cmd_panel.add(tab_pane, BorderLayout.CENTER);

    // if (!profile_running_process) {
    // Preview command
    button = new JButton(AnLocale.getString("Preview Command:"));
    button.setMargin(CollectUtility.buttonMargin);
    Dimension d = new Dimension(160, 14);
    button.setPreferredSize(d);
    button.setMaximumSize(d);
    button.setMnemonic(AnLocale.getString('P', "MNEM_COLLECTOR_PREVIEW_COMMAND"));
    button.setToolTipText(
        AnLocale.getString("Shows the 'collect' command that will be run using your selections"));
    button.addActionListener(this);
    button.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Preview Command:"));

    preview_cmd = new JTextField(40);
    AnUtility.setAccessibleContext(
        preview_cmd.getAccessibleContext(), AnLocale.getString("Preview Command:"));
    preview_cmd.setEditable(false);
    preview_cmd.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
    preview = new JPanel(new BorderLayout());
    preview.add(button, BorderLayout.WEST);
    preview.add(preview_cmd, BorderLayout.CENTER);
    /*
    preview_cmd = new JTextArea(1, 40);
    preview_cmd.setEditable(false);
    preview_cmd.getAccessibleContext().setAccessibleName(getLocaleStr("Preview collect command"));
    preview_cmd.getAccessibleContext().setAccessibleDescription(getLocaleStr("Preview collect command"));

    final JScrollPane preview_cmd_scroll = new AnJScrollPane(preview_cmd, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    preview_cmd_scroll.getAccessibleContext().setAccessibleName(getLocaleStr("Preview collect command"));
    preview_cmd_scroll.getAccessibleContext().setAccessibleDescription(getLocaleStr("Preview collect command"));

    preview = new JPanel(new BorderLayout());
    preview.setBorder(new TitledBorder(getLocaleStr("Preview collect command")));
    preview.add(button, BorderLayout.WEST);
    preview.add(preview_cmd_scroll, BorderLayout.CENTER);
    */
    cmd_panel.add(preview, BorderLayout.SOUTH);
    // tab_pane.add("3. " + getLocaleStr("Input/Output"), getOutputPane());
    tab_pane.add(AnLocale.getString("Output"), getOutputPane());
    // }

    tab_pane.setVerifyInputWhenFocusTarget(false);

    // for (int i = 0; i < tab_pane.getTabCount(); i++) {
    //    tab_pane.setMnemonicAt(i, new Integer(i + 1).toString().charAt(0));
    // } // numbers are removed - new mnemonics are needed
    //        work_panel = new AnJScrollPane();
    //        work_panel.setViewportView(cmd_panel);
    work_panel = new JPanel(new BorderLayout());
    work_panel.add(cmd_panel, BorderLayout.CENTER);
  }

  public void initHWCPanel(JPanel data_panel) {
    hwcSelectDialog = null;
    selectedHWCList = new ArrayList();
    hwcPanel = new JPanel(new BorderLayout());
    hwcPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 6, 8));
    counterLabel = new JLabel(AnLocale.getString("Selected Hardware Counters:"));
    counterLabel.setEnabled(false);
    counterLabel.setToolTipText(AnLocale.getString("List of hardware counters to profile"));
    hwcPanel.add(counterLabel, BorderLayout.NORTH);

    hwcList = new JList(selectedHWCList.toArray());
    // Set tooltip
    hwcList.addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(MouseEvent evt) {
            ListModel model = hwcList.getModel();
            int index = hwcList.locationToIndex(evt.getPoint());
            if (index >= 0) {
              HWCEntry entry = (HWCEntry) model.getElementAt(index);
              String ttText = entry.getDescriptionText(hwcNameMap);
              hwcList.setToolTipText(ttText);
            }
          }
        });
    // Handle Enter key
    hwcList.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
              if (propertyButton.isEnabled()) {
                propertyButton.doClick();
                e.consume();
              }
            }
            if (e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyCode() == 8) {
              if (removeButton.isEnabled()) {
                removeButton.doClick();
                e.consume();
              }
            }
          }
        });
    // Handle double-click
    hwcList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              if (propertyButton.isEnabled()) {
                propertyButton.doClick();
                e.consume();
              }
            }
          }
        });
    hwcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    AnUtility.setAccessibleContext(
        hwcList.getAccessibleContext(), AnLocale.getString("List of hardware counters to profile"));
    // list.addListSelectionListener(this);
    hwcListScrollPane = new AnJScrollPane();
    hwcListScrollPane.setViewportView(hwcList);
    hwcListScrollPane.setBorder(new LineBorder(AnEnvironment.ABOUT_BOX_BORDER_COLOR, 1));
    Dimension newSize = new Dimension(data_panel.getPreferredSize().width - 600, 70);
    hwcListScrollPane.setPreferredSize(newSize);
    hwcPanel.add(hwcListScrollPane, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();

    gridBagConstraints.gridx = 0;
    gridBagConstraints.insets = new Insets(4, 0, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    addHWCButton = new JButton(AnLocale.getString("Add"));
    addHWCButton
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Add hardware counters"));
    addHWCButton.setToolTipText(AnLocale.getString("Add counters to profile"));
    addHWCButton.setEnabled(false);
    addHWCButton.addActionListener(this);
    addHWCButton.setMnemonic(AnLocale.getString('A', "CollectPanel.addHWCButton"));
    buttonPanel.add(addHWCButton, gridBagConstraints);

    gridBagConstraints.gridx = 1;
    gridBagConstraints.insets = new Insets(4, 4, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    propertyButton = new JButton(AnLocale.getString("Properties"));
    propertyButton
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Set counter properties"));
    propertyButton.setToolTipText(AnLocale.getString("Change counter properties"));
    propertyButton.setEnabled(false);
    propertyButton.addActionListener(this);
    buttonPanel.add(propertyButton, gridBagConstraints);

    gridBagConstraints.gridx = 2;
    gridBagConstraints.insets = new Insets(4, 4, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    removeButton = new JButton(AnLocale.getString("Remove"));
    removeButton
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Remove counter"));
    removeButton.setToolTipText(AnLocale.getString("Remove selected counter"));
    removeButton.addActionListener(this);
    buttonPanel.add(removeButton, gridBagConstraints);

    //        gridBagConstraints.gridx = 3;
    //        gridBagConstraints.insets = new Insets(4, 0, 0, 0);
    //        gridBagConstraints.anchor = GridBagConstraints.EAST;
    ////        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    //        gridBagConstraints.weightx = 1.0;
    //        showButton = new JButton(getString("Available Counters"));
    //        showButton.getAccessibleContext().setAccessibleDescription(getString("Available
    // Counters"));
    //        showButton.setToolTipText(getString("Show all hardware counters that can be profiled
    // on the current system"));
    //        showButton.setEnabled(false);
    //        showButton.addActionListener(this);
    //        buttonPanel.add(showButton, gridBagConstraints);
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new Insets(4, 0, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    errorLabel = new JLabel();
    errorLabel.setForeground(Color.red);
    buttonPanel.add(errorLabel, gridBagConstraints);
    hwcPanel.add(buttonPanel, BorderLayout.SOUTH);

    updateButtons();

    data_panel.add(hwcPanel);
  }

  // Set all HWC related controls
  public void updateButtons() {
    if ((selectedHWCList.isEmpty()) || (!hwc1_prof.check.isSelected())) {
      hwcList.setListData(selectedHWCList.toArray());
      removeButton.setEnabled(false);
      propertyButton.setEnabled(false);
      hwcListScrollPane.setEnabled(false);
    } else {
      hwcList.setListData(selectedHWCList.toArray());
      hwcList.setSelectedIndex(0);
      removeButton.setEnabled(true);
      propertyButton.setEnabled(true);
      hwcListScrollPane.setEnabled(true);
    }
    hwcListScrollPane.revalidate();
    hwcListScrollPane.repaint();

    // Check for errors
    setErrorText(null);
    // Check max numbers of counters
    if (selectedHWCList.size() > maxHWC) {
      setErrorText(
          String.format(
              AnLocale.getString("The maximum number of counters allowed is %d"), maxHWC));
    }
    if (getErrorText().length() == 0) {
      // Check for identical entries
      for (int i = 0; i < selectedHWCList.size(); i++) {
        HWCEntry entry1 = selectedHWCList.get(i);
        String collectorString1 =
            entry1.getCollectorString().replaceAll("/.*", "").replaceAll(",.*", "");
        for (int j = i + 1; j < selectedHWCList.size(); j++) {
          HWCEntry entry2 = selectedHWCList.get(j);
          String collectorString2 =
              entry2.getCollectorString().replaceAll("/.*", "").replaceAll(",.*", "");
          if (collectorString1.equals(collectorString2)) {
            setErrorText(
                String.format(
                    AnLocale.getString("Identical counters in list: \'%s\' and \'%s\'"),
                    entry1.getCollectorString(),
                    entry2.getCollectorString()));
            break;
          }
        }
      }
    }
    autoUpdate();
  }

  private void setErrorText(String errorText) {
    if (errorText != null && !errorText.isEmpty()) {
      errorLabel.setText(errorText);
      errorLabel.setVisible(true);
    } else {
      errorLabel.setText("");
      errorLabel.setVisible(false);
    }
  }

  private String getErrorText() {
    return errorLabel.getText();
  }

  // Set all HWC attribute related controls
  public void setAttrButtons() {
    attrList.setListData(sel_attr.toArray());
    if (sel_attr.isEmpty()) {
      remAttr.setEnabled(false);
      modAttr.setEnabled(false);
      attrScrollPane.setEnabled(false);
    } else {
      attrList.setSelectedIndex(0);
      remAttr.setEnabled(true);
      modAttr.setEnabled(true);
      attrScrollPane.setEnabled(true);
    }
    attrScrollPane.revalidate();
    attrScrollPane.repaint();
  }

  // Set Attribute comboBox depending on type of counter
  public void setAttributeCombo() {
    String select_ctr = (String) hwc2_prof.combo.getSelectedItem();
    if (!hasAttributes(select_ctr)) {
      sel_attr.clear();
      setAttrButtons();
      attr_warn = false;
      ctr_attr.combo.setEnabled(false);
      addAttr.setEnabled(false);
      attrScrollPane.setEnabled(false);
      ctr_attr.field.setEnabled(false);
    } else {
      String select_attr = (String) ctr_attr.combo.getSelectedItem();
      // Get selected attribute and disable/enable buttons based on that
      if ((select_attr != null) && (select_attr.equals(AnLocale.getString("None")))) {
        if (hwc_att_data[0].length == 1) {
          ctr_attr.combo.setEnabled(false);
        } else {
          ctr_attr.combo.setEnabled(true);
        }
        addAttr.setEnabled(false);
        attrScrollPane.setEnabled(false);
        ctr_attr.field.setEnabled(false);
      } else {
        addAttr.setEnabled(true);
        ctr_attr.combo.setEnabled(true);
        attrScrollPane.setEnabled(true);
        ctr_attr.field.setEnabled(true);
      }
    }
  }

  // check if counter has attributes
  public boolean hasAttributes(String ctr_name) {
    // machine has no attributes
    if (hwc_attr_list.length == 0) {
      return false;
    }
    HWCEntry hw_type = hwcI18NMap.get(ctr_name);
    if (hw_type == null) {
      return false;
    }
    return hw_type.supportsAttrs();
  }

  // Add JDK path combobox with Browse button
  public final class TargetJDK extends CollectUtility.AnComboBox
      implements CollectUtility.Browseable, ItemListener {

    final JPanel panel;
    final JCheckBox check;
    final JButton button;
    final JLabel text;

    public TargetJDK(
        final CollectUtility.CList list,
        final CollectData cdata,
        final char mnemonic,
        final String label,
        final char browser_mnem) {
      super(50);

      this.check = cdata.check;

      String str, head;

      final List headers = new ArrayList(6);
      headers.add(AnLocale.getString("Path:"));
      headers.add("$JDK_HOME");
      headers.add("$JAVA_PATH");
      headers.add(AnLocale.getString("Installed Java"));
      headers.add(AnLocale.getString("Current JVM"));
      headers.add(AnLocale.getString("Default (PATH based)"));

      add(CollectUtility.AnComboBox.COMBO_TEXT, (String) headers.get(0), ipc_str_empty, true);

      for (int i = 1; i < 6; i++) {
        head = (String) headers.get(i);
        // process environment variables
        if (head.startsWith("$")) {
          str = getenv(head.substring(1));
          if (str != null && str.equals(ipc_str_empty)) {
            head += ":";
            add(CollectUtility.AnComboBox.COMBO_TEXT, head, str, false);
          }
        } else if (head.equals(AnLocale.getString("Installed Java"))) {
          head += ":";
          String os_name = System.getProperty("os.name");
          if (os_name.equals("Linux")) {
            str = "/usr/java/j2sdk1.4.2_06";
          } else {
            str = "/usr/jdk/j2sdk1.4.2_06";
          }
          add(CollectUtility.AnComboBox.COMBO_TEXT, head, str, false);
        } else if (head.equals(AnLocale.getString("Current JVM"))) {
          head += ":";
          str = System.getProperty("java.home");
          add(CollectUtility.AnComboBox.COMBO_TEXT, head, str, false);
        } else if (head.equals(AnLocale.getString("Default (PATH based)"))) {
          str = ipc_str_empty;
          add(CollectUtility.AnComboBox.COMBO_ITEM, head, str, false);
        }
      }

      this.setSelectedIndex(1); // select one of JDK pathes

      text = (JLabel) CollectUtility.getItem(label + ":");
      text.setDisplayedMnemonic(mnemonic);
      text.getAccessibleContext().setAccessibleName(label);
      text.getAccessibleContext().setAccessibleDescription(label);
      text.setLabelFor(this);

      panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      panel.add(this);

      button = new JButton(AnLocale.getString("Browse"), AnUtility.open_icon);
      //            button.setMnemonic(browser_mnem);

      button.setMargin(CollectUtility.buttonMargin);
      button.addActionListener(CollectPanel.this);
      // button.addActionListener(this)--;
      button.setActionCommand(label);
      button.setToolTipText(AnLocale.getString("Browse..."));
      //            button.setMnemonic(browser_mnem);
      panel.add(button);

      list.add(text, panel);
      if (check != null) {
        check.addItemListener(this);
      }

      text.setEnabled(cdata.isChecked());
      button.setEnabled(cdata.isChecked());
      setEnabled(cdata.isChecked());
      setEditable(cdata.isChecked());
    }

    // Set value
    @Override
    public void setValue(final String path) {
      final String java_bin = path + File.separatorChar + "bin" + File.separatorChar + "java";
      if (!(new File(java_bin).exists())) {
        showError(java_bin + AnLocale.getString(" doesnt't exist"));
      } else {
        setSelectedIndex(0);
        this.setText(path);
      }
    }

    @Override
    public String getValue() {
      return getText();
    }

    // Enable/disable setting
    @Override
    public void itemStateChanged(final ItemEvent event) {
      if (event.getSource() == check) {
        setEnabled(check.isSelected());
        setEditable(check.isSelected());
        text.setEnabled(check.isSelected());
        button.setEnabled(check.isSelected());
        vm_args.text.setEnabled(check.isSelected());
        vm_args.setEnabled(check.isSelected());
      }
    }
  }

  // Add label/text_field with browser
  public final class CollectExp extends CollectUtility.CText
      implements FocusListener, CollectUtility.Browseable {

    final int type;
    String value;
    final JLabel text;
    final JPanel panel;

    public CollectExp(
        final int type,
        final CollectUtility.CList list,
        final String value,
        final char mnemonic,
        final String label,
        final boolean browser,
        final char browser_mnem,
        final int width,
        boolean required) {

      super(value, width, JTextField.LEFT);

      this.type = type;
      this.value = value;

      setEditable(true);
      addFocusListener(this);

      if (required) {
        text = (JLabel) CollectUtility.getItem(label + ": *");
      } else {
        text = (JLabel) CollectUtility.getItem(label + ":");
      }
      text.setDisplayedMnemonic(mnemonic);
      text.getAccessibleContext().setAccessibleName(label);
      text.getAccessibleContext().setAccessibleDescription(label);
      text.setLabelFor(this);

      panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      panel.add(this);

      // Need file browser
      if (browser) {
        final JButton button = new JButton("...");
        //                button.setMargin(CollectUtility.buttonMargin);
        button.addActionListener(CollectPanel.this);
        button.setActionCommand(label);
        button.setToolTipText(AnLocale.getString("Browse..."));
        // button.setMnemonic(browser_mnem);

        panel.add(button);
      }

      list.add(text, panel);
    }

    public CollectExp(
        final int type,
        final CollectUtility.CList list,
        final String value,
        final char mnemonic,
        final String label,
        final boolean browser,
        final char browser_mnem) {
      this(type, list, value, mnemonic, label, browser, browser_mnem, 60, false);
    }

    public CollectExp(
        final int type,
        final CollectUtility.CList list,
        final String value,
        final char mnemonic,
        final String label,
        final boolean browser,
        final char browser_mnem,
        boolean required) {
      this(type, list, value, mnemonic, label, browser, browser_mnem, 60, required);
    }

    // Set value
    @Override
    public void setValue(final String path) {
      if (checkValue(path)) {
        setText(path);
      }
    }

    @Override
    public String getValue() {
      return getText();
    }

    // Check value
    public boolean checkValue(final String path) {
      String exp_name_suffix = ".er";
      switch (type) {
        case CEXP_TARGET:
          updateTitle(path);
          break;
        case CEXP_NAME:
          if ((path.length() != 0) && !path.endsWith(exp_name_suffix)) {
            showError(AnLocale.getString("The experiment name must end in the string .er"));
            value = path + exp_name_suffix;
            return false;
          }
          break;
        case CEXP_DIR:
          String en = exp_name.getValue();
          if (null != en && en.endsWith(exp_name_suffix)) {
            // do nothing, user specified the correct experiment name
          } else if (null != en && en.length() > 0) {
            // user specified invalid experiment name - add ".er"
            en = en + exp_name_suffix;
            exp_name.setText(en);
          } else {
            // experiment name is null or empty
            AnUtility.checkIPCOnWrongThread(false);
            exp_name.setText(CGetExpName(path.length() == 0 ? "." : path));
            AnUtility.checkIPCOnWrongThread(true);
          }
          break;
        case CEXP_GROUP:
          if ((path.length() != 0) && !path.endsWith(".erg")) {
            showError(AnLocale.getString("The group name must end in the string .erg"));
            return false;
          }
          break;
      }

      value = path;

      return true;
    }

    // Lost focus
    @Override
    public void focusLost(final FocusEvent event) {
      if (event.isTemporary()) {
        return;
      }

      if (!checkValue(getText())) {
        setText(value);
      }
    }

    // Gain focus
    @Override
    public void focusGained(final FocusEvent event) {}
  }

  // TEMPORARY SOLUTION
  // This flag indicates that the error dialog is shown,
  // but the problem is not fixed yet.
  boolean errorCollectorControlValue = false;

  // For collect data
  public final class CollectData extends JPanel implements ItemListener {

    final JCheckBox check;
    final JButton button;
    JLabel text;
    JComboBox combo;
    JTextField field;
    final String name;
    final String[][] data;
    CollectData parent, child;
    String cust_val;
    boolean add_child_data;
    boolean available;

    // Constructor for the 1st row
    public CollectData(
        final boolean set,
        final char mnemonic,
        final String title,
        final char mnemonic_r,
        final String label,
        final String[][] data,
        final int alignment,
        final boolean do_align,
        final String name) {
      this.name = name;
      this.data = data;
      parent = null;
      child = null;
      button = null;
      add_child_data = false;
      available = true;

      setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));

      // Enable or disable CheckBox
      if (title != null) {
        check = new JCheckBox(title, set);
        check.setMargin(CollectUtility.buttonMargin);
        if (mnemonic != ' ' && mnemonic != '0') {
          check.setMnemonic(mnemonic);
        }
        check.getAccessibleContext().setAccessibleDescription(title);
        add(check);
      } else {
        check = null;
      }

      // Extra options
      if (data != null) {
        setOption(set, mnemonic_r, label, alignment);
      } else if (check != null) {
        check.addItemListener(this); // NM
      }

      if (title == null) {
        if (mnemonic != ' ' && mnemonic != '0') {
          text.setDisplayedMnemonic(mnemonic);
          text.setLabelFor(combo);
        }
        text.setEnabled(set);
      } else if (text != null && combo != null) {
        text.setLabelFor(combo);
        text.setEnabled(set);
      }

      if (do_align) {
        data_list.add(this);
      }
    }

    // Constructor for the 2nd row if any
    public CollectData(
        final CollectData parent,
        final char mnemonic,
        final String title,
        final char mnemonic_r,
        final String label,
        final String[][] data,
        final int alignment,
        final boolean do_align,
        final boolean add_child_data,
        final String name) {
      final JPanel head_panel;

      this.name = name;
      this.data = data;
      this.parent = parent;
      child = null;
      parent.child = this;
      parent.add_child_data = add_child_data;
      check = parent.check;

      setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));

      // Show button
      if (title != null) {
        button = new JButton(title);
        button.setMargin(CollectUtility.buttonMargin);
        if (mnemonic != ' ') {
          button.setMnemonic(mnemonic);
        }
        button.addActionListener(CollectPanel.this);
        button.getAccessibleContext().setAccessibleDescription(title);
        head_panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        head_panel.setOpaque(false);
        head_panel.add(button);
        add(head_panel);
      } else {
        button = null;
        //                add(new JPanel());
      }

      // Extra options
      setOption(check.isSelected(), mnemonic_r, label, alignment);

      if (do_align) {
        data_list.add(this);
      }

      if (check != null) {
        check.setOpaque(false);
      }
      if (button != null) {
        button.setOpaque(false);
      }
      if (text != null) {
        text.setOpaque(false);
      }
      if (combo != null) {
        combo.setOpaque(false);
      }
      if (field != null) {
        field.setOpaque(false);
      }
      setOpaque(false);
    }

    // Extra options
    public void setOption(
        boolean set, final char mnemonic, final String label, final int alignment) {
      final String[] list;
      final String init_val;

      add(text = (JLabel) CollectUtility.getItem(label));
      text.getAccessibleContext().setAccessibleName(label);
      text.getAccessibleContext().setAccessibleDescription(label);
      if (mnemonic != ' ' && mnemonic != '0') {
        text.setDisplayedMnemonic(mnemonic);
      }

      if (data[0].length == 0) {
        check.setEnabled(false);
        text.setEnabled(false);

        set = false;
        list = new String[1];
        list[0] = init_val = cust_val = AnLocale.getString("N/A");
      } else {
        list = data[0];

        if (data[1] == null) // text value = combo
        {
          init_val = cust_val = data[0][0];
        } else if ((parent != null) && (parent.data != null) && (parent.data[0].length == 0)) {
          // not available
          if (button != null) {
            button.setEnabled(false);
          }

          text.setEnabled(false);

          init_val = cust_val = data[1][0];
        } else { // text value = data[1]
          init_val = data[1][0];
          cust_val = data[1][data[1].length - 1];
        }
      }

      add(combo = new JComboBox(list));
      add(field = new JTextField(init_val, 8));
      combo.setEnabled(set);
      // combo.setSelectedItem(list[0]);

      final JLabel lab = ((JLabel) CollectUtility.getItem(label));
      lab.setLabelFor(combo);
      lab.getAccessibleContext().setAccessibleName(label);
      lab.getAccessibleContext().setAccessibleDescription(label);
      combo.getAccessibleContext().setAccessibleName(label);
      combo.getAccessibleContext().setAccessibleDescription(label);
      field.getAccessibleContext().setAccessibleName(label);
      field.getAccessibleContext().setAccessibleDescription(label);
      field.setEnabled(set && (data[1] == null));
      field.setHorizontalAlignment(alignment);

      if (check != null) {
        check.addItemListener(this);
      }

      combo.addItemListener(this);
    }

    // Get the value
    public String getValue() {
      final String value;

      if (check == null) {
        return field.getText();
      }

      if (!check.isSelected()) {
        return null;
      }

      if (field == null) {
        return ipc_str_on;
      }
      value = field.getText();

      if (child == null || !add_child_data) {
        return value;
      }

      return value + ipc_str_comma + child.getValue();
    }

    // Get the check value
    public boolean isChecked() {
      if (check == null || !check.isSelected()) {
        return false;
      }
      return true;
    }

    // Set checkbox
    public void setChecked(final boolean checked) {
      if (check != null) {
        check.setSelected(checked);
      }
    }

    // Enable/disable setting
    @Override
    public void itemStateChanged(final ItemEvent event) {
      final Object src = event.getSource();
      final int state, size;
      String item;
      synchronized (globalSem) {
        if (inItemStateChanged) {
          return; // no recursion
        }
        inItemStateChanged = true;
        if (src == check) {
          state = event.getStateChange();

          if (state == ItemEvent.SELECTED) {
            if ((src == count_data.check)
                || (src == deadlock_trace.check)
                || (src == data_race.check)) {
              // Disable conflicting controls
              if (src == count_data.check) {
                clock_prof.check.setEnabled(false);
                clock_prof.check.setSelected(false);
                clock_prof.combo.setEnabled(false);
                clock_prof.text.setEnabled(false);
                hwc1_prof.check.setEnabled(false);
                hwc1_prof.check.setSelected(false);
                // hwc1_prof.??.setEnabled(false);
                // hwc1_prof.??.setEnabled(false);
                sample.check.setEnabled(false);
                sample.check.setSelected(false);
                sample.combo.setEnabled(false);
                sample.text.setEnabled(false);
                sample_sig.check.setEnabled(false);
                sample_sig.check.setSelected(false);
                sample_sig.combo.setEnabled(false);
                sample_sig.text.setEnabled(false);
              }
              sync_trace.check.setEnabled(false);
              sync_trace.check.setSelected(false);
              sync_trace.combo.setEnabled(false);
              sync_trace.text.setEnabled(false);
              io_trace.check.setEnabled(false);
              io_trace.check.setSelected(false);
              heap_trace.check.setEnabled(false);
              heap_trace.check.setSelected(false);
              if ((src == deadlock_trace.check) || (src == data_race.check)) {
                count_data.check.setEnabled(false);
                count_data.check.setSelected(false);
              } else {
                deadlock_trace.check.setEnabled(false);
                deadlock_trace.check.setSelected(false);
                data_race.check.setEnabled(false);
                data_race.check.setSelected(false);
              }
              mpi_trace.check.setEnabled(false);
              mpi_trace.check.setSelected(false);
              jvm_prof.check.setEnabled(false);
              jvm_prof.check.setSelected(false);
              if ((target_jdk != null) && (target_jdk.check != null)) {
                target_jdk.check.setEnabled(false);
                target_jdk.text.setEnabled(false);
              }
              if ((vm_args != null) && (vm_args.text != null)) {
                vm_args.text.setEnabled(false);
              }
              if (vm_args != null) {
                vm_args.setEnabled(false);
              }
            } else {
              count_data.check.setEnabled(false);
              count_data.check.setSelected(false);
              // NM deadlock_trace.check.setEnabled(false);
              // NM data_race.check.setEnabled(false);
              if ((null != heap_trace) && (null != jvm_prof)) {
                if (src == jvm_prof.check) {
                  heap_trace.check.setEnabled(false);
                  heap_trace.check.setSelected(false);
                }
                if (src == heap_trace.check) {
                  jvm_prof.check.setEnabled(false);
                  jvm_prof.check.setSelected(false);
                }
              }
            }
            // NM combo.setEnabled(true);
            String val = ipc_str_on;
            if (text != null) {
              text.setEnabled(true);
            }
            if (combo != null) {
              combo.setEnabled(true);
              // Enable it if it is 'Custom'
              item = (String) combo.getSelectedItem();
              field.setEnabled((data[1] == null) || item.startsWith(AnLocale.getString("Custom")));
              if (item.startsWith(AnLocale.getString("Custom"))) {
                if (text != null) {
                  val = field.getText(); // Correct?
                }
              } else if (data[1] == null) {
                val = item;
              } else {
                val = field.getText(); // Correct?
                // find value
                // size = data[0].length;
                // for (int i = 0; i < size; i++) {
                //    if (item.equals(data[0][i])) {
                //        //field.setText(data[1][i]);
                //        val = data[1][i];
                //        break;
                //    }
                // }
              }
            }
            String err = null;
            String cur_name = name;
            if ((ipc_str_hwc1_val.equals(name))
                || (ipc_str_hwc2_prof.equals(name))
                || (ipc_str_dspace_hwc1.equals(name))
                || (ipc_str_hwc_reg_alloc.equals(name))
                || (ipc_str_hwc_ctr_attr.equals(name))) {
              // TEMPORARY: use ipc_str_hwcprof
              cur_name = ipc_str_hwcprof;
              if (((hwc1_prof.getValue()) != null) && (selectedHWCList.size() > 0)) {
                String ctrlist = selectedHWCList.get(0).getName();
                for (int i = 1; i < selectedHWCList.size(); i++) {
                  ctrlist = ctrlist + ipc_str_comma + selectedHWCList.get(i).getName();
                }
                val = ctrlist;
              }
            }
            if (ipc_str_on.equals(val) && (ipc_str_MPIexpt.equals(cur_name))) {
              val = "CT"; // TEMPORARY: "-M" is not properly implemented yet
            }
            if (ipc_str_hwcprof.equals(cur_name)
                && (ipc_str_on.equals(val) || val.startsWith(ipc_str_hwc_default))) {
              // skip this call (TEMPORARY: "-h on" is not implemented yet)
              // err = null; // We come here when user clicks on "Hardware Counter Profiling"
            } else if ((ipc_str_zero.equals(val)
                    || ipc_str_None.equals(val)
                    || ipc_str_off.equals(val))
                && (ipc_str_hwcprof.equals(cur_name))) {
              // skip this call (HWC value is not selected yet)
            } else {
              err = Collector.setCollectorControlValue(cur_name, val); // IPC call
            }
            if (err != null) {
              // Check if this error is already on the screen
              if (errorCollectorControlValue) {
                // Let user fix this problem
              } else {
                errorCollectorControlValue = true;
                // Show error/warning dialog
                // System.err.println("Error 1: setCollectorControlValue(" + cur_name + ", " + val +
                // ") returned: " + err); // DEBUG
                AnUtility.showMessage(work_panel, err, JOptionPane.ERROR_MESSAGE);
                // Get value
                String cur_value = Collector.getCollectorControlValue(cur_name); // IPC call
                // System.err.println("getCollectorControlValue(" + cur_name + ") returned: " +
                // cur_value); // DEBUG
                if (ipc_str_MPIexpt.equals(cur_name) || (ipc_str_hwcprof.equals(cur_name))) {
                  // TEMPORARY: skip update
                } else if (null != field) {
                  item = (String) combo.getSelectedItem();
                  if (item.startsWith(AnLocale.getString("Custom"))) {
                    field.setText(cur_value);
                  }
                }
              }
            } else {
              errorCollectorControlValue = false;
            }
          } else if (state == ItemEvent.DESELECTED) {
            if ((ipc_str_hwc1_val.equals(name))
                || (ipc_str_hwc2_prof.equals(name))
                || (ipc_str_dspace_hwc1.equals(name))
                || (ipc_str_hwc_reg_alloc.equals(name))
                || (ipc_str_hwc_ctr_attr.equals(name))) {
              // TEMPORARY: skip unset
            } else {
              Collector.unsetCollectorControlValue(name);
            }
            if (combo != null) {
              combo.setEnabled(false);
            }
            if (text != null) {
              text.setEnabled(false);
            }
            if (field != null) {
              field.setEnabled(false);
            }
            if ((null != heap_trace) && (null != jvm_prof)) {
              if (src == jvm_prof.check) {
                javaprof_not_changed_by_user = false;
                heap_trace.check.setEnabled(true);
              }
              if (src == heap_trace.check) {
                jvm_prof.check.setEnabled(true);
                jvm_prof.check.setSelected(javaprof_not_changed_by_user);
              }
            }
            if (src == sample.check) {
              sample_not_changed_by_user = false;
            }
            if (src == clock_prof.check) {
              clkprof_not_changed_by_user = false;
            }
            if ((src == count_data.check)
                || (src == deadlock_trace.check)
                || (src == data_race.check)) {
              if (!(count_data.check.isSelected()
                  || deadlock_trace.check.isSelected()
                  || data_race.check.isSelected())) {
                // Enable all other controls
                clock_prof.check.setEnabled(true);
                clock_prof.check.setSelected(clkprof_not_changed_by_user);
                clock_prof.combo.setEnabled(clkprof_not_changed_by_user);
                clock_prof.text.setEnabled(clkprof_not_changed_by_user);
                sync_trace.check.setEnabled(true);
                hwc1_prof.check.setEnabled(true);
                sample.check.setEnabled(true);
                sample.check.setSelected(sample_not_changed_by_user);
                sample.combo.setEnabled(sample_not_changed_by_user);
                sample.text.setEnabled(sample_not_changed_by_user);
                sample_sig.check.setEnabled(true);
                io_trace.check.setEnabled(true);
                heap_trace.check.setEnabled(true);
                if (count_data.available) {
                  count_data.check.setEnabled(true);
                }
                if (deadlock_trace.available) {
                  deadlock_trace.check.setEnabled(true);
                }
                if (data_race.available) {
                  data_race.check.setEnabled(true);
                }
                mpi_trace.check.setEnabled(true);
                jvm_prof.check.setEnabled(true);
                jvm_prof.check.setSelected(javaprof_not_changed_by_user);
              }
            } else if (!(clock_prof.check.isSelected()
                || sync_trace.check.isSelected()
                || hwc1_prof.check.isSelected()
                || sample.check.isSelected()
                || sample_sig.check.isSelected()
                || io_trace.check.isSelected()
                || heap_trace.check.isSelected()
                || mpi_trace.check.isSelected()
                || jvm_prof.check.isSelected())) {
              if (count_data.available) {
                count_data.check.setEnabled(true);
              }
              if (deadlock_trace.available) {
                deadlock_trace.check.setEnabled(true);
              }
              if (data_race.available) {
                data_race.check.setEnabled(true);
              }
            }
          }
        } else if (src == combo) {
          if (text.getText().equals(AnLocale.getString("Attribute:"))) {
            setAttributeCombo();
            attr_warn = true;
          }
          if (text.getText().equals(AnLocale.getString("Counter Name:"))) {
            String select_ctr = (String) hwc2_prof.combo.getSelectedItem();
            setAttributeCombo();
            hwc_reg_data = setRegData(select_ctr);
          }
          if (text.getText().equals(AnLocale.getString("Hardware Counter Register:"))) {
            field.setText((String) combo.getSelectedItem());
          }
          item = (String) combo.getSelectedItem();

          // Save the custom value for later use
          if (item.startsWith(AnLocale.getString("Custom"))) {
            final int sidx = combo.getSelectedIndex();
            cust_val = data[1][sidx];
          }
          item = (String) event.getItem();
          // Enable it if it is 'Custom'
          if (item.startsWith(AnLocale.getString("Custom"))
              || text.getText().equals(AnLocale.getString("Attribute:"))) {
            field.setEnabled(true);
            field.setText(cust_val);
            String val = cust_val;
            if (val.isEmpty()) {
              val = "0"; // default value for empty string
              if ((src == exp_limit) || (src == exp_limit.combo)) {
                val = "none"; // default value for unlimited size
              }
            }
            if (ipc_str_descendant.equals(name)) {
              descendant.field.setToolTipText(
                  AnLocale.getString(
                      "Specify regular expression to match base name of the executable to follow"));
            }
            boolean need_check = true;
            if (ipc_str_hwc2_prof.equals(name) && val.startsWith(ipc_str_hwc_default)) {
              need_check =
                  false; // TEMPORARY: skip check - CLI cannot check the default HWC set yet
            }
            if ((ipc_str_hwc1_val.equals(name))
                || (ipc_str_dspace_hwc1.equals(name))
                || (ipc_str_hwc_reg_alloc.equals(name))
                || (ipc_str_hwc_ctr_attr.equals(name))) {
              // TEMPORARY: skip check - need new API to check these details
              need_check = false; // TEMPORARY: Need new API
            }
            if (need_check) {
              String err = Collector.setCollectorControlValue(name, val); // IPC call
              if (err != null) {
                // System.err.println("Error 2: setCollectorControlValue(" + name + ", " + val + ")
                // returned: " + err); // DEBUG
                AnUtility.showMessage(work_panel, err, JOptionPane.ERROR_MESSAGE);
              }
            }
            inItemStateChanged = false;
            return; // End of critical section
          } else if (data[1] == null) {
            field.setText(item); // ???
            String err = Collector.setCollectorControlValue(name, item);
            if (err != null) {
              // System.err.println("Error 3: setCollectorControlValue(" + name + ", " + item + ")
              // returned: " + err); // DEBUG
              AnUtility.showMessage(work_panel, err, JOptionPane.ERROR_MESSAGE);
            }
            inItemStateChanged = false;
            return; // End of critical section
          } else {
            field.setEnabled(false);
          }
          // Update value
          size = data[0].length;

          for (int i = 0; i < size; i++) {
            if (item.equals(data[0][i])) {
              field.setText(data[1][i]);
              String val = data[1][i];
              if (val.isEmpty()) {
                val = "0"; // default value for empty string
                if (src == exp_limit.combo) {
                  val = "none"; // default value for unlimited size
                }
              }
              // Check value
              if (ipc_str_hwc2_prof.equals(name) && ipc_str_hwc_default.equals(val)) {
                break; // TEMPORARY: CLI cannot check the default HWC set yet
              }
              if ((ipc_str_hwc1_val.equals(name))
                  || (ipc_str_dspace_hwc1.equals(name))
                  || (ipc_str_hwc_reg_alloc.equals(name))
                  || (ipc_str_hwc_ctr_attr.equals(name))) {
                // TEMPORARY: skip check - need new API to check these details
                break; // TEMPORARY: Need new API
              }
              AnUtility.checkIPCOnWrongThread(false);
              String err = Collector.setCollectorControlValue(name, val); // TEMPORARY
              AnUtility.checkIPCOnWrongThread(true);
              if (err != null) {
                // System.err.println("Error 4: setCollectorControlValue(" + name + ", " + val + ")
                // returned: " + err); // DEBUG
                JOptionPane.showMessageDialog(
                    (AnWindow.getInstance()).getFrame(),
                    err,
                    AnLocale.getString("Information"),
                    JOptionPane.INFORMATION_MESSAGE);
              }
              AnUtility.checkIPCOnWrongThread(false);
              err = Collector.getCollectorControlValue(name); // TEMPORARY
              AnUtility.checkIPCOnWrongThread(true);
              break;
            }
          }
        }
        inItemStateChanged = false;
      } // End of critical section
    }

    // Align label/combo-box
    public void setAlignmentX(final ArrayList data_list) {
      int i;
      for (i = 0; i < 4; i++) // 1.check 2.label 3.option 4.text
      {
        setAlignmentX(i, data_list);
      }
    }

    public void setAlignmentX(final int index, final ArrayList data_list) {
      int width, max_width;
      CollectData cd;
      JComponent cmp;
      Dimension psize;
      Iterator iter = data_list.iterator();

      max_width = 0;

      // Find the maximum width
      while (iter.hasNext()) {
        cd = (CollectData) iter.next();
        if (index >= cd.getComponentCount()) {
          continue;
        }

        cmp = (JComponent) cd.getComponent(index);
        width = cmp.getPreferredSize().width;
        if (max_width < width) {
          max_width = width;
        }
      }

      // Set the maximum width
      iter = data_list.iterator();

      while (iter.hasNext()) {
        cd = (CollectData) iter.next();

        if (index >= cd.getComponentCount()) {
          continue;
        }

        cmp = (JComponent) cd.getComponent(index);
        psize = cmp.getPreferredSize();
        psize.width = max_width;

        cmp.setPreferredSize(psize);
      }
    }
  }

  // Set visible
  @Override
  public final void setVisible(final boolean set) {
    super.setVisible(set);

    if (!set) {
      return;
    }

    // Make the most interesting part, the end, visible
    if (!profile_running_process && !system_profiling) {
      target.setVisibleAlign(CollectUtility.TEXT_RIGHT, true);
    }

    exp_name.setVisibleAlign(CollectUtility.TEXT_RIGHT, true);
    work_dir.setVisibleAlign(CollectUtility.TEXT_RIGHT, false);
  }

  /**
   * Class CollectChooser creates AnChooser using AnWindow.getAnChooser() method and provides
   * methods: getFile setDialogTitle
   */
  final class CollectChooser {

    AnChooser ac = null;

    public CollectChooser(final int chooser_type) {
      String dir = null;
      ac = anWindow.getAnChooser(title, chooser_type, dir);
    }

    /**
     * Sets Dialog Title
     *
     * @param title
     */
    public void setDialogTitle(final String title) {
      ac.setDialogTitle(title);
    }

    /**
     * Gets file name from the file chooser and returns value via text.setValue(name);
     *
     * @param cmd
     */
    public void getFile(final String cmd) {
      final CollectUtility.Browseable text;
      AnFile path_file; // NM replace
      String path;

      if (cmd.equals(CSTR_TARGET)) {
        text = target;
        path = text.getValue().trim();
        if (path.equals(ipc_str_empty) || !path.startsWith("/" /*File.separator*/)) {
          path = work_dir.getText();
          if (!path.equals(ipc_str_empty)) {
            path_file = new AnFile(path);
          } else {
            path = "./";
            path_file = new AnFile(path);
          }
        } else {
          path_file = new AnFile(path);
          if (path_file.isFile()) {
            path_file = (AnFile) path_file.getParentFile();
          }
        }
        ac.setFileSelectionMode(AnChooser.FILES_ONLY);
      } else if (cmd.equals(CSTR_WORK)) {
        text = work_dir;
        path_file = new AnFile(text.getValue()); // NM replace
        ac.setFileSelectionMode(AnChooser.DIRECTORIES_ONLY);
      } else if (cmd.equals(CSTR_TARGET_JDK)) {
        text = target_jdk;
        if (text.getValue() != null) {
          path_file = new AnFile(text.getValue()); // NM replace
        } else {
          path_file = new AnFile("/usr/j2se"); // NM replace
        }
        ac.setFileSelectionMode(AnChooser.FILES_AND_DIRECTORIES);
      } else {
        return;
      }

      ac.setCurrentDirectory(path_file);

      if ((ac.showOpenDialog(work_panel) == AnChooser.APPROVE_OPTION)
          && ((ac.getSelectedFile()) != null)) {
        path_file = new AnFile(ac.getSelectedFile().getAbsolutePath());
        String name = path_file.getAbsolutePath();
        /*
        if (!Analyzer.getInstance().isRemote()) {
        // remove .class and full path for java targets
        if (cmd.equals(CollectPanel.CSTR_TARGET) && AnUtility.getMimeFormat(path_file)
        == AnUtility.MIME_JAVA_CLASS_FILE) {
        if (name.endsWith(".class"))
        {
        name = name.substring(0, name.lastIndexOf(".class"));
        }
        final int sep_ind = name.lastIndexOf(File.separatorChar);
        if (sep_ind != -1) {
        name = name.substring(sep_ind + 1);
        }
        }
        }
        */
        text.setValue(name);
      }
    }
  } // end of Class CollectChooser

  //
  public interface Provider {

    public void setTitleStr(String title);
  }
  /*-------------------------------------    CONSTANTS    --------------------------------------------------------*/
  public static final String[] aux = {AnLocale.getString("Pause"), AnLocale.getString("Sample")};
  public static final char[] mnemonic = {
    AnLocale.getString('u', "MNEM_COLLECTOR_PAUSE"),
    AnLocale.getString('S', "MNEM_COLLECTOR_SAMPLE")
  };
  public static final String title = AnLocale.getString("Profile Application");
  public static final String title1 = AnLocale.getString("Profile Running Process");
  public static final String title2 = AnLocale.getString("Profile Kernel");
  // target types and possible errors
  static final int TARG_ELF_EXECUTABLE = 0;
  static final int TARG_JAVA_CLASS_FILE = 1;
  static final int TARG_JAR_FILE = 2;
  static final int TARG_UNKNOWN_TYPE = -1;
  static final int TARG_NOT_EXISTS = -2;
  static final int TARG_IS_NOT_FILE = -3;
  static final int TARG_IS_NOT_READABLE = -4;
}

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

package org.gprofng.mpmt;

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.collect.CollectUtility;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.event.AnSelectionEvent;
import org.gprofng.mpmt.util.gui.AnJPanel;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.gui.AnUtility.AnLabel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.accessibility.AccessibleContext;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class SummaryPanel extends AnJPanel implements AnChangeListener {

  private final int win_id;
  private final AnWindow window;
  private long cur_sel_obj, cur_sel_type, last_sel_obj;
  private JPanel info_panel;
  private AnList func_info, metrics_info;
  private AnLabel metric_title;
  private JComponent[] func_text, excl_text, ev_text, incl_text, iv_text;
  private boolean computed, cur_is_data_or_index, func_renew, metrics_renew;
  private int[] align;
  private Object summaryPanelLock = new Object();

  // Constructor
  public SummaryPanel() {
    window = AnWindow.getInstance();
    win_id = 0;
    computed = false;
    cur_sel_obj = 0;
    last_sel_obj = 0;
    cur_sel_type = AnDisplay.DSP_Null;
    cur_is_data_or_index = false;
    func_renew = true;
    metrics_renew = true;
    align = null;

    // Initialize GUI components & set preferred size
    initComponents();

    registerKeyboardAction(
        new Analyzer.HelpAction(AnVariable.HELP_TabsSummary),
        "help",
        KeyboardShortcuts.helpActionShortcut,
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    AccessibleContext context = getAccessibleContext();
    String loc_string = AnLocale.getString("Summary");
    context.setAccessibleName(loc_string);
    context.setAccessibleDescription(loc_string);
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
    // System.out.println("OverviewView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
        reset();
        break;
      case EXPERIMENTS_LOADED_FAILED:
      case EXPERIMENTS_LOADED:
        setBaseOnly(window.getSettings().getCompareModeSetting().comparingExperiments());
        // Nothing
        break;
      case FILTER_CHANGED:
        computed = false;
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
      case SELECTION_UPDATE:
        // Nothing
        break;
      case SELECTION_CHANGED:
        AnSelectionEvent selectionEvent = (AnSelectionEvent) e.getSource();
        computed = false;
        computeOnAWorkerThread(
            selectionEvent.getSelObj(),
            selectionEvent.getType(),
            selectionEvent.getSubtype(),
            selectionEvent.getVersion());
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

  // public GraphControls toolbar_gctl;
  // Initialize GUI components
  private void initComponents() {
    setLayout(new BorderLayout());
    Dimension fillerDim = new Dimension(10, 10);
    //        toolbar_gctl = new GraphControls(this.m_window);
    //        toolbar_gctl.is_UpDownAvail = false;
    //        toolbar_gctl.is_ColorAvail = false;
    //        toolbar_gctl.getCreateToolbar();
    //        toolbar_gctl.tbar_backward.setEnabled (false);
    //        toolbar_gctl.tbar_forward.setEnabled(false);
    //        toolbar_gctl.tbar_zoom_in.setEnabled(false);
    //        toolbar_gctl.tbar_zoom_out.setEnabled(false);
    //        toolbar_gctl.tbar_zoom_reset.setEnabled(false);

    info_panel = new AnJPanel();
    info_panel.setLayout(new BoxLayout(info_panel, BoxLayout.Y_AXIS));

    info_panel.add(new Box.Filler(fillerDim, fillerDim, fillerDim));
    //        AnLabel anf = (AnLabel) AnUtility.getHeader(AnLocale.getString("Selected Object:"));
    //        info_panel.add(anf);
    //        JToolBar tb = toolbar_gctl.getToolbar();
    //        tb.setFloatable(false);
    //        JPanel tbPanel = new ANJPanel(new BorderLayout());
    //        tbPanel.add(tb, BorderLayout.PAGE_START);
    //
    //        info_panel.add(tbPanel);
    func_info = new AnList(true);
    info_panel.add(func_info);
    //        anf.setLabelFor(func_info);
    func_info.getAccessibleContext().setAccessibleName(AnLocale.getString("Function info"));
    func_info.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Function info"));
    //        info_panel.add(new JSeparator(SwingConstants.HORIZONTAL));
    info_panel.add(new Box.Filler(fillerDim, fillerDim, fillerDim));
    AnLabel anm = (AnLabel) AnUtility.getHeader("");
    metric_title = anm;
    info_panel.add(anm);
    metrics_info = new AnList(true);
    info_panel.add(metrics_info);
    info_panel.add(new Box.Filler(fillerDim, fillerDim, fillerDim));
    anm.setLabelFor(metrics_info);
    metrics_info.getAccessibleContext().setAccessibleName(AnLocale.getString("Metrics info"));
    metrics_info
        .getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Metrics info"));
    //        JScrollPane scrollPane = new AnJScrollPane(info_panel);
    //        scrollPane.setBorder(null);
    add(info_panel, BorderLayout.CENTER);

    //        func_info.setBorder(AnVariable.boxBorder);
    //        metrics_info.setBorder(AnVariable.boxBorder);
    //        setPreferredSize(new Dimension(AnVariable.INFO_PRF_SIZE));
    setMinimumSize(AnVariable.INFO_MIN_SIZE);
  }

  // Reset the metrics items (number of func items remains the same).
  private void reset() {
    metrics_renew = true;
  }

  /*
   * Use summaryPanelLock so doCompute is called in the same order as computeOnAWorkerThread is called. See 18765370 - Selection Details incorrect after "go back" and "go forward" in src/dis view
   */
  private void computeOnAWorkerThread(
      final long[] sel_obj, final int type, final int subtype, final int version) {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            synchronized (summaryPanelLock) {
              synchronized (AnVariable.mainFlowLock) {
                doCompute(sel_obj, type, subtype, version);
              }
            }
          }
        },
        "Summary_thread");
    ;
  }

  // Compute summary display
  private void doCompute(
      final long[] sel_obj, final int type, final int subtype, final int version) {
    AnUtility.checkIfOnAWTThread(false);
    final boolean is_data_or_index;

    if (computed
        && (cur_sel_type == type)
        && (type != AnDisplay.DSP_CallerCalleeSelf)
        && (cur_sel_obj == sel_obj[0])
        && (last_sel_obj == sel_obj[sel_obj.length - 1])) { // multiselection
      return;
    }

    if ((type == AnDisplay.DSP_MemoryObject)
        || (type == AnDisplay.DSP_DataLayout)
        || (type == AnDisplay.DSP_DataObjects)
        || (type == AnDisplay.DSP_IndexObject)
        || (type == AnDisplay.DSP_IO)
        || (type == AnDisplay.DSP_IOFileDescriptors)
        || (type == AnDisplay.DSP_IOCallStacks)
        || (type == AnDisplay.DSP_Heap)) {
      is_data_or_index = true;
    } else {
      is_data_or_index = false;
    }

    if ((cur_is_data_or_index != is_data_or_index)
        || ((is_data_or_index == true) && (cur_sel_type != type))) {
      func_renew = true;
      metrics_renew = true;
    }

    cur_sel_obj = sel_obj[0];
    cur_sel_type = type;
    cur_is_data_or_index = is_data_or_index;
    final Object[] data;
    if (version == 1) {
      data = getSummary(sel_obj, type, subtype); // IPC
    } else if (version == 2) {
      data = getSummaryV2(sel_obj, type, subtype); // IPC
    } else {
      data = null;
    }
    final Object[] marks;
    if (type == AnDisplay.DSP_Source
        || type == AnDisplay.DSP_SourceV2
        || type == AnDisplay.DSP_Disassembly
        || type == AnDisplay.DSP_DisassemblyV2
        || type == AnDisplay.DSP_SourceDisassembly) {
      marks = getSummaryHotMarks(sel_obj, type);
    } else {
      marks = null;
    }

    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            updateSummary(data, type, marks);
          }
        });
    computed = true;
  }

  private void updateSummary(final Object[] data, int type, final Object[] marks) {
    // System.out.println("SummaryPanel type: " + type);
    if (data == null) {
      return;
    }
    boolean need_validate = false;
    char[] mnemonic;
    final String[] value, iv_label, e_label, ev_label, i_label;
    final double[] clock, exclusive, e_percent, inclusive, i_percent;
    final int[] vtype;
    int size, columns, len, i;
    JLabel jlabel;
    JPanel panel;
    JScrollPane tx_scroll;
    String[] label;
    final Dimension msize;
    final boolean show_inclusive, exclusive_is_data_derived;
    if ((type == AnDisplay.DSP_MemoryObject)
        || (type == AnDisplay.DSP_DataLayout)
        || (type == AnDisplay.DSP_DataObjects)) {
      show_inclusive = false;
      exclusive_is_data_derived = true;
    } else if ((type == AnDisplay.DSP_IndexObject)
        || (type == AnDisplay.DSP_IO)
        || (type == AnDisplay.DSP_IOFileDescriptors)
        || (type == AnDisplay.DSP_IOCallStacks)
        || (type == AnDisplay.DSP_Heap)) {
      show_inclusive = false;
      exclusive_is_data_derived = false;
    } else {
      show_inclusive = true;
      exclusive_is_data_derived = false;
    }

    // "Bugs 16624403 and 19539622" (leave this string intact for searches)
    boolean func_is_Total = false;

    final Object[] func = (Object[]) data[0];
    if (func != null) {
      align = (int[]) func[0];
      mnemonic = (char[]) func[1];
      label = (String[]) func[2];
      value = (String[]) func[3];
      size = label.length;

      // "Bugs 16624403 and 19539622" (leave this string intact for searches)
      // check if this func is <Total>
      for (i = 0; i < size; i++) {
        if (label[i].equals("Name") && value[i].equals("<Total>")) func_is_Total = true;
      }

      if (func_renew) {
        func_info.removeAll();

        func_text = new JComponent[size];

        for (i = 0; i < size; i++) {
          jlabel = (JLabel) AnUtility.getItem(label[i] + ":");
          //                    jlabel.setDisplayedMnemonic(mnemonic[i]);
          jlabel.getAccessibleContext().setAccessibleName(label[i]);
          jlabel.getAccessibleContext().setAccessibleDescription(label[i]);

          if ((type == AnDisplay.DSP_DataObjects) && (i == size - 1)) {
            func_text[i] = AnUtility.getTextArea(value[i] != null ? value[i] : "");
            (func_text[i]).setFont(CollectUtility.text_font);

            tx_scroll = new AnJScrollPane(func_text[i]);
            tx_scroll.setPreferredSize(new Dimension(func_text[0].getPreferredSize().width, 120));
            tx_scroll.setMaximumSize(new Dimension(func_text[0].getMaximumSize().width, 120));

            func_info.add(jlabel, tx_scroll);
          } else {
            func_text[i] = AnUtility.getText(value[i] != null ? value[i] : "", 30);

            func_info.add(jlabel, func_text[i]);
          }

          jlabel.setLabelFor(func_text[i]);
        }

        // Fix the height
        msize = func_info.getMaximumSize();
        msize.height = func_info.getPreferredSize().height;
        func_info.setMaximumSize(msize);

        func_info.invalidate();

        func_renew = false;
        need_validate = true;
      } else {
        for (i = 0; i < size; i++) {
          if (value.length < size) { // TEMPORARY TRAP
            AnLog.log(
                "analyzer: race condition in updateSummary(): size="
                    + size
                    + " value.length="
                    + value.length);
            return;
          } else if ((type == AnDisplay.DSP_DataObjects) && (i == size - 1)) {
            if (!(func_text[i] instanceof JTextArea)) {
              AnLog.log(
                  "analyzer: invalid cast in updateSummary(): "
                      + func_text[i]
                      + " is not a JTextArea");
              return;
            }
            ((JTextArea) func_text[i]).setText(value[i] != null ? value[i] : "");
          } else {
            if (!(func_text[i] instanceof JTextField)) {
              AnLog.log(
                  "analyzer: invalid cast in updateSummary(): "
                      + func_text[i]
                      + " is not a JTextField");
              return;
            }
            ((JTextField) func_text[i]).setText(value[i] != null ? value[i] : "");
          }
        }
      }
    } else {
      func_info.removeAll();
      func_info.addLabel(new JLabel(AnLocale.getString("No Data")));
    }

    final Object[] metric = (Object[]) data[1];
    if (metric != null) {
      label = (String[]) metric[0];
      clock = (double[]) metric[1];
      exclusive = (double[]) metric[2];
      e_percent = (double[]) metric[3];
      inclusive = (double[]) metric[4];
      i_percent = (double[]) metric[5];
      vtype = (int[]) metric[6];
      size = label.length;
      e_label = new String[size];
      ev_label = new String[size];
      i_label = new String[size];
      iv_label = new String[size];
      columns = 0;

      for (i = 0; i < size; i++) {
        ev_label[i] = iv_label[i] = null;

        switch (vtype[i]) {
          case AnMetric.VT_DOUBLE:
            e_label[i] = (new AnDouble(exclusive[i])).toPercentQuote(e_percent[i]);
            if (show_inclusive) {
              i_label[i] = (new AnDouble(inclusive[i])).toPercentQuote(i_percent[i]);
            }
            break;
          case AnMetric.VT_INT:
            e_label[i] = (new AnInteger((int) exclusive[i])).toPercentQuote(e_percent[i]);
            if (show_inclusive) {
              i_label[i] = (new AnInteger((int) inclusive[i])).toPercentQuote(i_percent[i]);
            }
            break;
          case AnMetric.VT_LLONG:
          case AnMetric.VT_ULLONG:
            if (clock[i] == 0.0) {
              e_label[i] = (new AnLong((long) exclusive[i])).toPercentQuote(e_percent[i]);
              if (show_inclusive) {
                i_label[i] = (new AnLong((long) inclusive[i])).toPercentQuote(i_percent[i]);
              }
            } else {
              ev_label[i] = (new AnLong((long) exclusive[i])).toQuoteSpace();
              e_label[i] = (new AnDouble(exclusive[i] / clock[i])).toPercentQuote(e_percent[i]);
              if (show_inclusive) {
                iv_label[i] = (new AnLong((long) inclusive[i])).toQuoteSpace();
                i_label[i] = (new AnDouble(inclusive[i] / clock[i])).toPercentQuote(i_percent[i]);
              }
            }
            break;
          default:
            e_label[i] = i_label[i] = " ";
            break;
        }

        // "Bugs 16624403 and 19539622" (leave this string intact for searches)
        if (func_is_Total) {
          if (label[i].equals("Block Covered %") || label[i].equals("Instr Covered %")) {
            if (e_label[i] != null && !e_label[i].equals(" ")) e_label[i] = " ";
            if (i_label[i] != null && !i_label[i].equals(" ")) i_label[i] = " ";
            if (ev_label[i] != null && !ev_label[i].equals(" ")) ev_label[i] = " ";
            if (iv_label[i] != null && !iv_label[i].equals(" ")) iv_label[i] = " ";
          }
        }

        // Compute the maximum width
        len = e_label[i].length();
        if (columns < len) {
          columns = len;
        }

        if (show_inclusive) {
          len = i_label[i].length();
          if (columns < len) {
            columns = len;
          }
        }

        if (ev_label[i] != null) {
          len = ev_label[i].length();
          if (columns < len) {
            columns = len;
          }

          if (show_inclusive) {
            len = iv_label[i].length();
            if (columns < len) {
              columns = len;
            }
          }
        }
      }

      if (metrics_renew) {
        metrics_info.removeAll();

        panel = new AnJPanel(new GridLayout(1, 2));
        if (show_inclusive) {
          panel.add(AnUtility.getHeader(AnLocale.getString("Exclusive"), AnUtility.excl_icon));
          panel.add(AnUtility.getHeader(AnLocale.getString("Inclusive"), AnUtility.incl_icon));
        } else if (exclusive_is_data_derived) {
          panel.add(AnUtility.getHeader(AnLocale.getString("Data-derived"), AnUtility.data_icon));
        } else {
          panel.add(AnUtility.getHeader(AnLocale.getString("Exclusive"), AnUtility.excl_icon));
        }

        metrics_info.add((JComponent) null, panel);

        excl_text = new AnUtility.AnText[size];
        ev_text = new AnUtility.AnText[size];
        incl_text = new AnUtility.AnText[size];
        iv_text = new AnUtility.AnText[size];

        for (i = 0; i < size; i++) {
          excl_text[i] = AnUtility.getNumber(e_label[i], columns);
          incl_text[i] = AnUtility.getNumber(i_label[i], columns);

          if (show_inclusive) {
            panel = new AnJPanel(new GridLayout(1, 2));
            panel.add(excl_text[i]);
            panel.add(incl_text[i]);
          } else {
            panel = new AnJPanel(new GridLayout(1, 1));
            panel.add(excl_text[i]);
          }

          jlabel = (JLabel) AnUtility.getItem(label[i] + ":");
          /* YXXX TBR Thomas, feel free to delete this
          if (mnemonic[i] != '\0') {
          jlabel.setDisplayedMnemonic(mnemonic[i]);
          }
          */
          jlabel.setLabelFor(excl_text[i]);

          metrics_info.add(jlabel, panel);
          // A11y: add labels for inclusive metrics
          if (show_inclusive) {
            JLabel inclabel = new JLabel(label[i]);
            /* YXXX TBR Thomas, feel free to delete this
            if (mnemonic[i] != '\0') {
            inclabel.setDisplayedMnemonic(mnemonic[i]);
            }
            */
            inclabel.setLabelFor(incl_text[i]);
            inclabel.setVisible(false);
            metrics_info.add(inclabel, panel);
          }

          if (ev_label[i] != null) {
            ev_text[i] = AnUtility.getNumber(ev_label[i], columns);
            iv_text[i] = AnUtility.getNumber(iv_label[i], columns);

            if (show_inclusive) {
              panel = new AnJPanel(new GridLayout(1, 2));
              panel.add(ev_text[i]);
              panel.add(iv_text[i]);
            } else {
              panel = new AnJPanel(new GridLayout(1, 1));
              panel.add(ev_text[i]);
            }
            metrics_info.add(AnUtility.getItem(AnLocale.getString("\" count:")), panel);
          } else {
            ev_text[i] = iv_text[i] = null;
          }
        }

        if (marks != null) {
          int[] nmetric = (int[]) marks[0];
          int[] inc = (int[]) marks[1];
          for (i = 0; i < nmetric.length; i++) {
            if (inc[i] == 1) {
              ((JTextField) incl_text[nmetric[i]]).setBackground(AnVariable.HILIT_INC_COLOR);
            } else {
              ((JTextField) excl_text[nmetric[i]]).setBackground(AnVariable.HILIT_COLOR);
            }
          }
        }

        metrics_info.setAlignmentX();
        metrics_info.invalidate();

        metrics_renew = false;
        need_validate = true;
      } else {
        if (columns != 0) {
          columns += 2;
        }
        for (i = 0; i < size; i++) {
          ((JTextField) excl_text[i]).setText(e_label[i]);
          ((JTextField) excl_text[i]).setColumns(columns);
          ((JTextField) incl_text[i]).setText(i_label[i]);
          ((JTextField) incl_text[i]).setColumns(columns);
          ((JTextField) excl_text[i]).setBackground(Color.white);
          ((JTextField) incl_text[i]).setBackground(Color.white);

          if ((ev_text[i] != null) && (ev_label[i] != null)) {
            ((JTextField) ev_text[i]).setText(ev_label[i]);
            ((JTextField) ev_text[i]).setColumns(columns);
            ((JTextField) iv_text[i]).setText(iv_label[i]);
            ((JTextField) iv_text[i]).setColumns(columns);
          }
        }
        if (marks != null) {
          int[] nmetric = (int[]) marks[0];
          int[] inc = (int[]) marks[1];
          for (i = 0; i < nmetric.length; i++) {
            if (inc[i] == 1) {
              ((JTextField) incl_text[nmetric[i]]).setBackground(AnVariable.HILIT_INC_COLOR);
            } else {
              ((JTextField) excl_text[nmetric[i]]).setBackground(AnVariable.HILIT_COLOR);
            }
          }
        }
      }
    } else {
      metrics_info.removeAll();
      final JLabel lab = new JLabel(AnLocale.getString("No Data"));
      metrics_info.addLabel(lab);
      lab.getAccessibleContext().setAccessibleName(AnLocale.getString("No Data"));
      lab.getAccessibleContext().setAccessibleDescription(AnLocale.getString("No Data"));
    }

    if (need_validate) {
      info_panel.validate();
    }

    // Make the most interesting part, the end, visible
    size = func_text.length;
    for (i = 0; i < size; i++) {
      if ((type == AnDisplay.DSP_DataObjects) && (i == size - 1)) {
        if (!(func_text[i] instanceof AnUtility.AnTextArea)) {
          AnLog.log(
              "analyzer: invalid cast in updateSummary(): "
                  + func_text[i]
                  + " is not an AnUtility.AnTextArea");
          continue;
        }
        ((AnUtility.AnTextArea) func_text[i]).setCaretPosition(0);
      } else {
        int align_v = 0;
        if (align.length > i) {
          align_v = align[i];
        }
        if (!(func_text[i] instanceof AnUtility.AnText)) {
          AnLog.log(
              "analyzer: invalid cast in updateSummary(): "
                  + func_text[i]
                  + " is not an AnUtility.AnText");
          continue;
        }
        ((AnUtility.AnText) func_text[i]).setVisibleAlign(align_v, need_validate);
      }
    }
  }

  // Clear summary display after error
  public void clear() {
    func_renew = true;
    func_info.removeAll();
    metrics_info.removeAll();
  }

  // XXXX set metrics title for compare mode to indicate base group only
  public void setBaseOnly(boolean baseOnly) {
    if (baseOnly == true) {
      metric_title.setText(AnLocale.getString("Metrics (base group only)"));
    } else {
      metric_title.setText("");
    }

    metric_title.repaint();
  }

  // Native methods from liber_dbe.so
  private Object[] getSummary(final long[] sel_obj, final int type, final int subtype) {
    synchronized (IPC.lock) {
      window.IPC().send("getSummary");
      window.IPC().send(win_id);
      window.IPC().send(sel_obj);
      window.IPC().send(type);
      window.IPC().send(subtype);
      return (Object[]) window.IPC().recvObject();
    }
  }

  private Object[] getSummaryV2(final long[] sel_obj, final int type, final int subtype) {
    synchronized (IPC.lock) {
      window.IPC().send("getSummaryV2");
      window.IPC().send(0);
      window.IPC().send(sel_obj);
      window.IPC().send(type);
      window.IPC().send(subtype);
      return (Object[]) window.IPC().recvObject();
    }
  }

  private Object[] getSummaryHotMarks(final long[] sel_obj, final int type) {
    synchronized (IPC.lock) {
      window.IPC().send("getSummaryHotMarks");
      window.IPC().send(win_id);
      window.IPC().send(sel_obj);
      window.IPC().send(type);
      return (Object[]) window.IPC().recvObject();
    }
  }
}

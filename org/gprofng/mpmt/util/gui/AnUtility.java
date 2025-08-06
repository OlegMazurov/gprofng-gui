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

package org.gprofng.mpmt.util.gui;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.collect.CollectUtility;
import org.gprofng.mpmt.metrics.MetricColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.accessibility.AccessibleContext;
import javax.imageio.ImageIO;
import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

public final class AnUtility {

  private static boolean checkIPCOnWrongThread = true; // Checks ony if true
  private static int checkIPCOnWrongThreadCounter = 0; // Checks only if counter == 0
  private static final int TEXT_LEFT = 1;
  // public final static int	TEXT_CENTER = 2;
  // public final static int	TEXT_RIGHT  = 3;
  public static final int ERROR_MSG = 1;
  public static final int WARNING_MSG = 2;
  public static final int PSTAT_MSG = 3;
  public static final int PWARN_MSG = 4;
  public static final int SPACES_BETWEEN_COLUMNS = 3;
  public static final String SPACE = " ";
  public static final String EOL = "\n";
  // mime types for files
  public static final int MIME_ELF_EXECUTABLE = 0x7f454c46;
  public static final int MIME_JAVA_CLASS_FILE = 0xcafebabe;
  public static final int MIME_JAR_FILE = 0x504b0304;
  private static final int MIME_UNKNOWN_FILE_TYPE = 0x00000000;
  public static final int MIME_DIRECTORY = 1;
  public static final int MIME_EXECUTABLE = 2;
  public static final int MIME_CANNOT_READ_FILE = 0xFFFFFFFF;
  public static final Cursor norm_cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
  public static final Cursor wait_cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
  private static final String package_mpmt = "org/gprofng/mpmt/icons/"; // NOI18N
  private static final String green_bar_image = "green100.png"; // NOI18N
  // Icons
  public static final ImageIcon addColumnIcon = getImageIcon("addtablecolumns.png"); // NOI18N
  public static final ImageIcon add_icon = getImageIcon("add.png"); // NOI18N
  public static final ImageIcon appl_filt_icon = getImageIcon("apply_filter.png"); // NOI18N
  public static final ImageIcon attr_icon = getImageIcon("attributed.png"); // NOI18N
  public static final ImageIcon back_filt_icon = getImageIcon("back_filter.png"); // NOI18N
  public static final ImageIcon back_icon = getImageIcon("backward.png"); // NOI18N
  public static final ImageIcon blankIcon = getImageIcon("blank.png"); // NOI18N
  public static final ImageIcon bubble_icon = getImageIcon("bubble_icon_16.png"); // NOI18N
  public static final ImageIcon cale_icon = getImageIcon("callee.png"); // NOI18N
  public static final ImageIcon calr_icon = getImageIcon("caller.png"); // NOI18N
  public static final ImageIcon cls_icon = getImageIcon("class.png"); // NOI18N
  public static final ImageIcon collect_icon = getImageIcon("collect.png"); // NOI18N
  public static final ImageIcon colr_icon = getImageIcon("color.png"); // NOI18N
  public static final ImageIcon compare_icon = getImageIcon("compare.png"); // NOI18N
  public static final ImageIcon connect_icon = getImageIcon("connect.png"); // NOI18NNOI18N
  public static final ImageIcon data_icon = getImageIcon("data.png"); // NOI18N
  public static final ImageIcon del_icon = getImageIcon("delete.png"); // NOI18N
  public static final ImageIcon down_icon = getImageIcon("down.png"); // NOI18N
  public static final ImageIcon ebad_icon = getImageIcon("badexperiment.png"); // NOI18N
  public static final ImageIcon egrp_icon = getImageIcon("expgroup.png"); // NOI18N
  public static final ImageIcon elf_icon = getImageIcon("exe_elf.png"); // NOI18N
  public static final ImageIcon eold_icon = getImageIcon("oldexperiment.png"); // NOI18N
  public static final ImageIcon epcl_icon = getImageIcon("expandcollaps.png"); // NOI18N
  public static final ImageIcon circleBlueIcon = getImageIcon("circleBlue.gif"); // NOI18N
  public static final ImageIcon circleRedIcon = getImageIcon("circleRed.gif"); // NOI18NNOI18N
  public static final ImageIcon circleGreenIcon = getImageIcon("circleGreen.gif"); // NOI18N
  public static final ImageIcon circleOrangeIcon = getImageIcon("circleOrange.gif"); // NOI18N
  public static final ImageIcon ewarn_icon = getImageIcon("warnexperiment.png"); // NOI18N
  public static final ImageIcon excl_icon = getImageIcon("exclusive.png"); // NOI18N
  public static final ImageIcon expt_icon = getImageIcon("experiment.png"); // NOI18N
  public static final ImageIcon fileResolved = getImageIcon("fileResolved.png"); // NOI18N
  public static final ImageIcon fileUnResolved = getImageIcon("fileUnResolved.png"); // NOI18N
  public static final ImageIcon filt_icon = getImageIcon("filter.png"); // NOI18N
  public static final ImageIcon forw_icon = getImageIcon("forward.png"); // NOI18N
  public static final ImageIcon func_icon = getImageIcon("func_item.png"); // NOI18N
  public static final ImageIcon fwd_filt_icon = getImageIcon("forward_filter.png"); // NOI18N
  public static final ImageIcon gear_icon = getImageIcon("gear.png"); // NOI18N
  public static final ImageIcon goBackwardIcon = getImageIcon("gobackward.png"); // NOI18N
  public static final ImageIcon goDownIcon = getImageIcon("godown.png"); // NOI18N
  public static final ImageIcon goForwardIcon = getImageIcon("goforward.png"); // NOI18N
  public static final ImageIcon goUpIcon = getImageIcon("goup.png"); // NOI18N
  public static final ImageIcon hamburgerFocusedIcon =
      getImageIcon("hamburgerFocused.png"); // NOI18N
  public static final ImageIcon hamburgerIcon = getImageIcon("hamburger.png"); // NOI18N
  public static final ImageIcon hamburgerBlankIcon = getImageIcon("hamburgerBlank.png"); // NOI18N
  public static final ImageIcon heap_icon = getImageIcon("heaptrace.png"); // NOI18N
  public static final ImageIcon hollowArrowDownIcon = getImageIcon("hollowArrowDown.png"); // NOI18N
  public static final ImageIcon hollowArrowLeftIcon = getImageIcon("hollowArrowLeft.png"); // NOI18N
  public static final ImageIcon hollowArrowRightIcon =
      getImageIcon("hollowArrowRight.png"); // NOI18N
  public static final ImageIcon hollowArrowUpIcon = getImageIcon("hollowArrowUp.png"); // NOI18N
  public static final ImageIcon hot_icon = getImageIcon("hot.png"); // NOI18N
  public static final ImageIcon hwc_icon = getImageIcon("hwc.png"); // NOI18N
  public static final ImageIcon incl_icon = getImageIcon("inclusive.png"); // NOI18N
  public static final ImageIcon iotrace_icon = getImageIcon("i_o_usage_16.png"); // NOI18N
  public static final ImageIcon jar_icon = getImageIcon("jar.png"); // NOI18N
  public static final ImageIcon mpi_icon = getImageIcon("mpitrace.png"); // NOI18N
  public static final ImageIcon mtall_icon = getImageIcon("apply_mtrs.png"); // NOI18N
  public static final ImageIcon moreIcon = getImageIcon("more_icon.png"); // NOI18N
  public static final ImageIcon next_icon = getImageIcon("find_next.png"); // NOI18N
  public static final ImageIcon open_icon = getImageIcon("open.png"); // NOI18N
  public static final ImageIcon panhand_icon = getImageIcon("panhand.png"); // NOI18N
  public static final ImageIcon prev_icon = getImageIcon("find_prev.png"); // NOI18N
  public static final ImageIcon prof_icon = getImageIcon("profile.png"); // NOI18N
  public static final ImageIcon races_icon = getImageIcon("races.png"); // NOI18N
  public static final ImageIcon redo_icon = getImageIcon("redo.png"); // NOI18N
  public static final ImageIcon removeFocusedIcon = getImageIcon("removeFocused.png"); // NOI18N
  public static final ImageIcon removeIcon = getImageIcon("remove.png"); // NOI18N
  public static final ImageIcon removeIconBlank = getImageIcon("removeBlank.png"); // NOI18N
  public static final ImageIcon restore_icon = getImageIcon("stop.png"); // NOI18N
  public static final ImageIcon rset_icon = getImageIcon("reset.png"); // NOI18N
  public static final ImageIcon samp_icon = getImageIcon("sample.png"); // NOI18N
  public static final ImageIcon save_icon = getImageIcon("save.png"); // NOI18N
  public static final ImageIcon smallArrowBlankIcon = getImageIcon("smallBlank.png"); // NOI18N
  public static final ImageIcon smallArrowDownFocusedIcon =
      getImageIcon("small_down_focused.png"); // NOI18N
  public static final ImageIcon smallArrowUpFocusedIcon =
      getImageIcon("small_up_focused.png"); // NOI18N
  public static final ImageIcon smallArrowDownIcon = getImageIcon("small_down.png"); // NOI18N
  public static final ImageIcon smallArrowLeftIcon = getImageIcon("small_left.png"); // NOI18N
  public static final ImageIcon smallArrowRightIcon = getImageIcon("small_right.png"); // NOI18N
  public static final ImageIcon smallArrowUpIcon = getImageIcon("small_up.png"); // NOI18N
  public static final ImageIcon sodn_icon = getImageIcon("sort_dn.png"); // NOI18N
  public static final ImageIcon soup_icon = getImageIcon("sort_up.png"); // NOI18N
  public static final ImageIcon sync_icon = getImageIcon("synctrace.png"); // NOI18N
  public static final ImageIcon undo_icon = getImageIcon("undo.png"); // NOI18N
  public static final ImageIcon up_icon = getImageIcon("up.png"); // NOI18N
  public static final ImageIcon valuesIcon = getImageIcon("values.png"); // NOI18N
  public static final ImageIcon warningIcon = getImageIcon("warning.png"); // NOI18N
  public static final ImageIcon errorIcon = getImageIcon("error.png"); // NOI18N
  public static final ImageIcon warningNewIcon = getImageIcon("warningNew.png"); // NOI18N
  public static final ImageIcon errorNewIcon = getImageIcon("errorNew.png"); // NOI18N
  public static final ImageIcon errorWarningNewIcon = getImageIcon("errorWarningNew.png"); // NOI18N
  public static final ImageIcon zmin_icon = getImageIcon("zoom_in.png"); // NOI18N
  public static final ImageIcon zmot_icon = getImageIcon("zoom_out.png"); // NOI18N
  public static final ImageIcon numberIcon = getImageIcon("number.png"); // NOI18N
  public static final ImageIcon timeIcon = getImageIcon("time.png"); // NOI18N
  public static final ImageIcon percentIcon = getImageIcon("percent.png"); // NOI18N
  public static final ImageIcon nextViewIcon = getImageIcon("nextview.png"); // NOI18N
  public static final ImageIcon previousViewIcon = getImageIcon("previousview.png"); // NOI18N
  public static final ImageIcon base1Icon = getImageIcon("base1.png"); // NOI18N
  public static final ImageIcon base2Icon = getImageIcon("base2.png"); // NOI18N

  public static final ImageIcon compareAbsoluteIcon = getImageIcon("compareAbsolute.png"); // NOI18N
  public static final ImageIcon compareDeltaIcon = getImageIcon("compareDelta.png"); // NOI18N
  public static final ImageIcon compareRatioIcon = getImageIcon("compareRatio.png"); // NOI18N
  public static final ImageIcon compareReverseIcon = getImageIcon("compareReverse.png"); // NOI18N
  public static final ImageIcon compareHamburgerIcon =
      getImageIcon("compareHamburger.png"); // NOI18N
  // Other icons
  public static final ImageIcon analyzer_icon = getImageIcon("analyzer64.png"); // NOI18N

  private static final int chkbox_height = (new JLabel(" ")).getPreferredSize().height; // NOI18N;
  private static WeakReference<Map<String, Integer>> wrSigMap;
  private static boolean lib_path_set = false;
  private static AnThreadGroup threadGroup = new AnThreadGroup();

  public static int getMimeFormat(final File file) {
    if (file.isDirectory()) {
      return MIME_DIRECTORY;
    }
    if (file.canExecute()) {
      if (Analyzer.getInstance().remoteConnection == null) {
        try {
          final DataInputStream dis = new DataInputStream(
              new BufferedInputStream(new FileInputStream(file)));
          int elf_type = 0;
          int iMagicNumber = dis.readInt();
          if (iMagicNumber == MIME_ELF_EXECUTABLE) { // reading elf header
            dis.skipBytes(12);
            elf_type = dis.readShort(); // check for elf executable
          }
          dis.close();
          if (elf_type == 0x0002 || elf_type != 0x0200) { // MSB or LSB elf
            return MIME_ELF_EXECUTABLE;
          }
        } catch (IOException e) {
          return MIME_CANNOT_READ_FILE;
        }
      }
      return MIME_EXECUTABLE;
    }
    String nm = file.getName();
    if (nm.endsWith(".class'")) {
      return MIME_JAVA_CLASS_FILE;
    }
    if (nm.endsWith(".jar'")) {
      return MIME_JAR_FILE;
    }
    return MIME_UNKNOWN_FILE_TYPE;
  }

  public static boolean isTarget(final File file) {
    final int type = getMimeFormat(file);
    return (type == MIME_ELF_EXECUTABLE || type == MIME_JAVA_CLASS_FILE || type == MIME_JAR_FILE);
  }

  public static String getRemoteOutput(String cmd) {
    String rc = Analyzer.getInstance().remoteConnectCommand;
    ArrayList<String> args = new ArrayList<>(
        Arrays.asList(rc.split("\\s+")));
    args.add(cmd);

    ProcessBuilder processBuilder = new ProcessBuilder(args);
    String lines = "";
    try {
      Process process = processBuilder.start();

      // Read standard output
      BufferedReader stdoutReader = new BufferedReader(
          new InputStreamReader(process.getInputStream()));
      for (;;) {
        String line;
        line = stdoutReader.readLine();
        if (line == null) {
          break;
        }
        lines += line + "\n";
      }
      process.waitFor();
    } catch (IOException | InterruptedException ex) {
    }
    return lines;
  }

  // General check box
  public static final class AnCheckBox extends JCheckBox {

    public AnCheckBox(final String text) {
      this(text, false);
    }

    public AnCheckBox(final String text, final boolean selected) {
      super(text, selected);

      final Dimension psize;

      setMargin(AnVariable.buttonMargin);
      setHorizontalAlignment(JCheckBox.CENTER);

      psize = getPreferredSize();
      psize.height = chkbox_height;
      setPreferredSize(psize);

      setOpaque(false);
      if (text != null) {
        getAccessibleContext().setAccessibleDescription(text);
        getAccessibleContext().setAccessibleName(text);
      }
    }
  }

  public static final class AnRadioButton extends JRadioButton {

    public AnRadioButton() {
      super();
      setOpaque(false);
    }

    public AnRadioButton(String text, boolean selected) {
      super(text, selected);
      setOpaque(false);
      getAccessibleContext().setAccessibleDescription(text);
      getAccessibleContext().setAccessibleName(text);
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      if (hasFocus() && getText().length() == 0) {
        // Accessibility
        g.setColor(new Color(194 - 40, 214 - 40, 233 - 40));
        g.drawLine(getWidth() - 1, 4, getWidth() - 1, getHeight() - 5);
      }
    }
  }

  // General labels
  public static final class AnLabel extends JLabel {

    public AnLabel(final String text, final int alignment, final boolean init_a11y) {
      super(text, alignment);

      final float falignment;

      if (alignment == RIGHT) {
        falignment = RIGHT_ALIGNMENT;
      } else if (alignment == LEFT) {
        falignment = LEFT_ALIGNMENT;
      } else {
        falignment = CENTER_ALIGNMENT;
      }

      setAlignmentX(falignment);
      setForeground(AnVariable.LABEL_COLOR);
      setBorder(AnVariable.labelBorder);
      if (AnEnvironment.isLFNimbus()) {
        setFont(getFont().deriveFont(Font.BOLD));
      }
      if (init_a11y) {
        initializeA11y();
      }
    }

    public AnLabel(
        final String text, final Icon icon, final int alignment, final boolean init_a11y) {
      this(text, alignment, init_a11y);
      if (AnEnvironment.isLFNimbus()) {
        setFont(getFont().deriveFont(Font.BOLD));
      }

      if (icon != null) {
        setIcon(icon);
      }
    }

    private void initializeA11y(final String accessibleName, final String accessibleDesc) {
      final AccessibleContext context = this.getAccessibleContext();
      final String accName =
          (accessibleName != null && (!accessibleName.equals("")))
              ? accessibleName
              : // NOI18N
              this.getText();
      final String accDesc =
          (accessibleDesc != null && (!accessibleDesc.equals("")))
              ? accessibleDesc
              : // NOI18N
              accName;
      context.setAccessibleName(accName);
      context.setAccessibleDescription(accDesc);
    }

    private void initializeA11y() {
      initializeA11y(this.getText(), this.getText());
    }
  }

  // General textarea
  public static final class AnTextArea extends JTextArea {

    public AnTextArea(final String text, final boolean editable) {
      super(text);
      AnUtility.setAccessibleContext(this.getAccessibleContext(), "");
      setBackground(CollectUtility.text_color);
      setEditable(editable);
      setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

      setCaret(
          new javax.swing.text.DefaultCaret() {
            public void focusGained(final FocusEvent event) {
              if (getComponent().isEnabled()) {
                setVisible(true);
              }

              setSelectionVisible(true);

              // Don't use selectAll which will move Caret to the end.
              setCaretPosition(getDocument().getLength());
              moveCaretPosition(0);
            }

            public void focusLost(final FocusEvent event) {
              super.focusLost(event);

              select(0, 0);
            }
          });
    }
  }

  // General textfield
  public static final class AnText extends JTextField {

    int text_align;

    public AnText(final String text, final int columns, final int alignment) {
      super(text, columns);

      text_align = TEXT_LEFT;

      setFont(CollectUtility.text_font);
      setBackground(CollectUtility.text_color);
      if (AnEnvironment.isLFNimbus()) {
        setBackground(AnEnvironment.NIMBUS_PANEL_LIGHT_BACKGROUND);
        setBorder(null);
      }
      setEditable(false);
      setHorizontalAlignment(alignment);
      setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
      this.getAccessibleContext().setAccessibleName(text);
      this.getAccessibleContext().setAccessibleDescription(text);
      setCaret(
          new javax.swing.text.DefaultCaret() {
            @Override
            public void focusGained(final FocusEvent event) {
              if (getComponent().isEnabled()) {
                setVisible(true);
              }

              setSelectionVisible(true);

              // Don't use selectAll which will move Caret to the end.
              if (text_align == TEXT_LEFT) {
                // This try/catch is an ugly workaround for fixing bug #5025668.
                // NPE caused because of java 1.4.2_02 DefaultCaret bug.
                // TODO: remove the workaround when java 1.5 becomes the default
                // one supported by analyzer.
                try {
                  setDot(getDocument().getLength());
                  moveDot(0);
                } catch (NullPointerException npe) {
                  /* do nothing; workaround*/
                }
              } else { // Same code as selectAll
                setDot(0);
                moveDot(getDocument().getLength());
              }
            }

            @Override
            public void focusLost(final FocusEvent event) {
              super.focusLost(event);

              setDot(text_align == TEXT_LEFT ? 0 : getDocument().getLength());
            }
          });
    }

    public void setVisibleAlign(final int text_align, final boolean delay) {

      this.text_align = text_align;

      if (delay) {
        setCaretPosition(text_align == TEXT_LEFT ? 0 : getDocument().getLength());
      } else {
        // Don't use setCaretPosition which caused flash
        SwingUtilities.invokeLater(
            new Runnable() {
              @Override
              public void run() {
                BoundedRangeModel visibility;
                visibility = getHorizontalVisibility();
                visibility.setValue(
                    text_align == TEXT_LEFT ? visibility.getMinimum() : visibility.getMaximum());
              }
            });
      }
    }
  }

  public static JComponent getTextArea(final String text) {
    final AnTextArea text_area;

    text_area = new AnUtility.AnTextArea(text, false);
    text_area.setBorder(AnVariable.textBorder);

    return text_area;
  }

  public static JComponent getHeader(final String text) {
    return new AnLabel(text, JLabel.CENTER, true);
  }

  public static JComponent getHeader(final String text, final Icon icon) {
    return new AnLabel(text, icon, JLabel.CENTER, true);
  }

  public static JComponent getTitle(final String text) {
    return new AnLabel(text, JLabel.LEFT, true);
  }

  public static JComponent getTitle(final String text, final Icon icon) {
    return new AnLabel(text, icon, JLabel.LEFT, true);
  }

  public static JComponent getItem(final String text) {
    return new AnLabel(text, JLabel.RIGHT, true);
  }

  public static JComponent getText(final String text, final int columns) {
    return new AnText(text, columns == 0 ? 0 : columns + 2, JTextField.LEFT);
  }

  public static JComponent getNumber(final String text, final int columns) {
    return new AnText(text, columns == 0 ? 0 : columns + 2, JTextField.RIGHT);
  }

  // Class for button
  public static final class ResponseAction extends JButton {

    public ResponseAction(
        final String text,
        final Icon icon,
        final char mnemonic,
        final String tooltip,
        final Insets margin,
        final ActionListener listener) {
      super(text, icon);
      final AccessibleContext context = this.getAccessibleContext();
      context.setAccessibleName(text);
      context.setAccessibleDescription(text);

      if (margin != null) {
        setMargin(AnVariable.buttonMargin);
      }

      if (mnemonic != '\0') {
        setMnemonic(mnemonic);
      }

      if (tooltip != null) {
        setToolTipText(tooltip);
      }

      if (text != null) {
        setActionCommand(text);
      } else if (tooltip != null) {
        setActionCommand(tooltip);
      }

      addActionListener(listener);
    }

    public ResponseAction(
        final Icon icon, final char mnemonic, final String tooltip, final ActionListener listener) {
      this(null, icon, mnemonic, tooltip, AnVariable.buttonMargin, listener);
    }

    public ResponseAction(final Icon icon, final String tooltip, final ActionListener listener) {
      this(null, icon, '\0', tooltip, AnVariable.buttonMargin, listener);
    }

    public ResponseAction(
        final Icon icon,
        final String tooltip,
        final String actionCommand,
        final ActionListener listener) {
      this(icon, tooltip, listener);
      setActionCommand(actionCommand);
    }

    public ResponseAction(
        final String text, final Icon icon, final char mnemonic, final ActionListener listener) {
      this(text, icon, mnemonic, null, AnVariable.buttonMargin, listener);
    }

    public ResponseAction(final String text, final char mnemonic, final ActionListener listener) {
      this(text, null, mnemonic, listener);
    }
  }

  // Get experiment list
  public static List<String> getExpList(final String[] args) {
    final List<String> exp_list = new ArrayList<>();
    final int size = args.length;
    for (int i = 0; i < size; i++) {
      List<String> list = getGroupList(args[i]);
      if (list.size() > 1) {
        exp_list.add(args[i]);
      } else {
        exp_list.addAll(list);
      }
    }
    return exp_list;
  }

  // Get experiment from group file
  public static List<String> getGroupList(String path) {
    String line;
    final List<String> exp_list = new ArrayList<>();
    BufferedReader br = null;

    try {
      br = new BufferedReader(new FileReader(path));
      if (((line = br.readLine()) == null) || !line.equals("#analyzer experiment group")) // NOI18N
      {
        throw (new IOException());
      }

      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (!line.startsWith("#") && !line.equals("")) // NOI18N
        {
          exp_list.add(line);
        }
      }
    } catch (IOException e) {
      if (path.endsWith("/")) // NOI18N
      {
        path = path.substring(0, path.length() - 1);
      }

      exp_list.add(path);
    } finally {
      try {
        if (br != null) {
          br.close();
        }
      } catch (IOException e) {
      }
    }

    return exp_list;
  }

  // Get image from resource, from the default path
  public static ImageIcon getImageIcon(final String name) {
    final URL iconURL;

    iconURL = Analyzer.cls_loader.getResource(package_mpmt + name);
    if (iconURL == null) {
      return null;
    } else {
      return new ImageIcon(iconURL);
    }
  }

  /**
   * Get ImageIcon bar for specified metric
   *
   * @param metric - Use AnMetric.getComd()
   * @param width
   * @param height
   * @return ImageIcon
   */
  public static ImageIcon getImageIconBar(String metric, int width, int height) {
    ImageIcon icon = null;
    BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = bi.createGraphics();
    Color color = null;
    if (metric != null) {
      color = MetricColors.getColor(metric);
    }
    try {
      if (color == null) { // Read image
        final URL iconURL100 = Analyzer.cls_loader.getResource(package_mpmt + green_bar_image);
        BufferedImage in = ImageIO.read(iconURL100);
        g2.drawImage(in, 0, 0, null);
      } else { // Draw image
        g2.setColor(color);
        // g2.fill3DRect(0, 0, width, height, true /* raised */);
        g2.fillRect(0, 0, width, height);
      }
      g2.dispose();
    } catch (Exception e) {
      return null; // cannot create
    }
    ;
    icon = new ImageIcon(bi);
    return icon;
  }

  // Remove the trailing new-line
  public static String trimNewLine(final String org) {
    int last;

    for (last = org.length() - 1; last >= 0; last--) {
      if (org.charAt(last) != '\n') // NOI18N
      {
        break;
      }
    }

    return org.substring(0, last + 1);
  }

  // Add the trailing new-line to the end of string if needed
  public static String addNewLine(final String org) {
    if (org.charAt(org.length() - 1) == '\n') {
      return org;
    } else {
      return new String(org + '\n');
    }
  }

  // Find the class installed home location
  public static String findResourceHome(final ClassLoader loader, final String resource) {
    final URL url;
    final String file;
    String home;
    final int first, index;
    int last;

    if ((url = loader.getResource(resource)) == null) {
      return null;
    }

    file = url.getFile();
    first = file.indexOf('/'); // NOI18N
    last = file.indexOf(".jar!", first); // NOI18N
    last = file.lastIndexOf('/', last); // NOI18N

    if ((first == -1) || (last == -1)) {
      try {
        home = (new File("../..")).getCanonicalPath(); // NOI18N
      } catch (IOException e) {
        home = "../.."; // NOI18N
      }
    } else {
      if ((index = file.lastIndexOf('/', last - 1)) != -1) // NOI18N
      {
        last = index;
      }

      home = file.substring(first, file.lastIndexOf('/', last - 1));
    }

    return home;
  }

  //    // Get Throwable stack trace
  //    public static String getStackTrace(Throwable th) {
  //	StringWriter	str_writer;
  //
  //	str_writer = new StringWriter();
  //	th.printStackTrace(new PrintWriter(str_writer));
  //
  //	return str_writer.toString();
  //    }
  // Add title in work area
  public static final JLabel setLabel(final JPanel panel, final String text) {
    final JComponent label;

    panel.add(label = AnUtility.getTitle(text), BorderLayout.NORTH);
    AccessibleContext context = panel.getAccessibleContext();
    context.setAccessibleName(text);
    context.setAccessibleDescription(text);
    context = label.getAccessibleContext();
    context.setAccessibleName(text);
    context.setAccessibleDescription(text);
    return (JLabel) label;
  }

  // Display dialog with message string
  public static void showMessage(final Component comp, final String msg, final int err_type) {
    if (msg != null) {
      SwingUtilities.invokeLater(
          new Runnable() {
            @Override
            public void run() {
              final String title;
              switch (err_type) {
                case JOptionPane.ERROR_MESSAGE:
                  title = AnLocale.getString("Error"); // NOI18N
                  break;
                case JOptionPane.WARNING_MESSAGE:
                  title = AnLocale.getString("Warning"); // NOI18N
                  break;
                case JOptionPane.INFORMATION_MESSAGE:
                  title = AnLocale.getString("Information"); // NOI18N
                  break;
                case JOptionPane.QUESTION_MESSAGE:
                  title = AnLocale.getString("Question"); // NOI18N
                  break;
                default:
                  title = AnLocale.getString("Message"); // NOI18N
                  break;
              }
              try {
                // XXX workaround for Swing bug 4254022 (fixed in JDK 1.6)
                // ie. don't display more than 500 lines in dialog box
                String message = msg;
                int count = 0;
                int ndx = 0;
                for (; ; ) {
                  ndx = msg.indexOf('\n', ndx + 1);
                  if (ndx < 0) {
                    break;
                  }
                  count++;
                  if (count > 500) {
                    message = msg.substring(0, ndx + 1) + "..."; // NOI18N
                    break;
                  }
                }

                JOptionPane.showMessageDialog(comp, message, title, err_type);
              } catch (ArrayIndexOutOfBoundsException ae) {
                System.out.println("****Debug PROBLEM****IGNORING EXCP"); // NOI18N
                ae.printStackTrace();
              }
            }
          });
    }
  }

  // Returns numerical representation of the character signal value
  public static int getSignalValue(String signal) {
    int ret = -1;
    try {
      ret = Integer.parseInt(signal);
    } catch (NumberFormatException e) {
      Map<String, Integer> sig_map = (wrSigMap == null) ? null : wrSigMap.get();
      if (sig_map == null) {
        sig_map = new HashMap<String, Integer>(100);
        sig_map.put("SIGHUP", 1); // NOI18N
        sig_map.put("SIGINT", 2); // NOI18N
        sig_map.put("SIGQUIT", 3); // NOI18N
        sig_map.put("SIGILL", 4); // NOI18N
        sig_map.put("SIGTRAP", 5); // NOI18N
        sig_map.put("SIGABRT", 6); // NOI18N
        sig_map.put("SIGIOT", 6); // NOI18N
        sig_map.put("SIGEMT", 7); // NOI18N
        sig_map.put("SIGFPE", 8); // NOI18N
        sig_map.put("SIGKILL", 9); // NOI18N
        sig_map.put("SIGBUS", 10); // NOI18N
        sig_map.put("SIGSEGV", 11); // NOI18N
        sig_map.put("SIGSYS", 12); // NOI18N
        sig_map.put("SIGPIPE", 13); // NOI18N
        sig_map.put("SIGALRM", 14); // NOI18N
        sig_map.put("SIGTERM", 15); // NOI18N
        sig_map.put("SIGUSR1", 16); // NOI18N - wrong number on Linux
        sig_map.put("SIGUSR2", 17); // NOI18N - wrong number on Linux
        sig_map.put("SIGCHLD", 18); // NOI18N
        sig_map.put("SIGCLD", 18); // NOI18N
        sig_map.put("SIGPWR", 19); // NOI18N
        sig_map.put("SIGWINCH", 20); // NOI18N
        sig_map.put("SIGURG", 21); // NOI18N
        sig_map.put("SIGIO", 22); // NOI18N
        sig_map.put("SIGSTOP", 23); // NOI18N
        sig_map.put("SIGTSTP", 24); // NOI18N
        sig_map.put("SIGCONT", 25); // NOI18N
        sig_map.put("SIGTTIN", 26); // NOI18N
        sig_map.put("SIGTTOU", 27); // NOI18N
        sig_map.put("SIGVTALRM", 28); // NOI18N
        sig_map.put("SIGPROF", 29); // NOI18N
        sig_map.put("SIGXCPU", 30); // NOI18N
        sig_map.put("SIGXFSZ", 31); // NOI18N
        wrSigMap = new WeakReference<Map<String, Integer>>(sig_map);
      }

      signal = signal.trim();
      if (!signal.startsWith("SIG")) {
        signal = "SIG" + signal; // NOI18N
      }
      ret = getSignalValueIPC(signal);
      if (ret > 0) {
        return ret;
      }
      final Integer obj = sig_map.get(signal);
      if (obj != null) {
        ret = obj.intValue();
      }
    }
    return ret;
  }

  public static void setLibPath() {
    if (!lib_path_set) {
      lib_path_set = true;
    }
  }

  /** Get the value of an environment variable */
  public static String getenv(final String name) {
    return System.getenv(name);
  }
  // mouse enter/exit listener for buttons
  private static final MouseInputAdapter sharedMouseListener =
      new MouseInputAdapter() {
        @Override
        public void mouseEntered(MouseEvent evt) {
          JButton btn = (JButton) evt.getSource();
          if (btn.isEnabled()) {
            btn.setBorderPainted(true);
            btn.setContentAreaFilled(true);
          }
        }

        @Override
        public void mouseExited(MouseEvent evt) {
          JButton btn = (JButton) evt.getSource();
          if (btn.isEnabled()) {
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
          }
        }
      };

  // set button look & feel to match mars IDE
  public static void setButtonLF(JButton button) {
    button.setBorderPainted(false);
    button.setContentAreaFilled(false);
    button.setFocusable(false);
    button.addMouseListener(sharedMouseListener);
  }

  public static String dirname(String path) {
    String name = path;
    int i = path.lastIndexOf("/"); // NOI18N
    if (i > 0) {
      name = path.substring(0, i);
    }
    return name;
  }

  public static String basename(String path) {
    String name = path;
    int i = path.lastIndexOf("/"); // NOI18N
    if (i >= 0 && i < (path.length() - 1)) {
      name = path.substring(i + 1);
    }
    return name;
  }

  public static void setAccessibleContext(AccessibleContext ac, String txt) {
    if (ac != null) {
      ac.setAccessibleName(txt);
      ac.setAccessibleDescription(txt);
    }
  }

  public static void setAccessibleContext(JLabel label) {
    setAccessibleContext(label.getAccessibleContext(), label.getText());
  }

  public static void setTextAndAccessibleContext(JLabel label, String text) {
    label.setText(text);
    setAccessibleContext(label.getAccessibleContext(), text);
  }

  public static void setTextAndAccessibleContext(JCheckBox checkBox, String text) {
    checkBox.setText(text);
    setAccessibleContext(checkBox.getAccessibleContext(), text);
  }

  public static void setTextAndAccessibleContext(JButton button, String text) {
    button.setText(text);
    setAccessibleContext(button.getAccessibleContext(), text);
  }

  public static void setTTAndAccessibleContext(JComponent component, String text) {
    component.setToolTipText(text);
    setAccessibleContext(component.getAccessibleContext(), text);
  }

  /**
   * Dispatch a task to the AWT thread
   *
   * @param r
   */
  public static void dispatchOnSwingThread(Runnable r) {
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
      return;
    }
    try {
      SwingUtilities.invokeAndWait(r);
    } catch (Exception ex) {
      System.err.println("dispatchOnSwingThread exception: " + ex); // NOI18N
      ex.printStackTrace();
    }
  }

  /**
   * Dispatch a task to the AWT thread
   *
   * @param r
   */
  public static void invokeLaterOnSwingThread(Runnable r) {
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
    } else {
      SwingUtilities.invokeLater(r);
    }
  }

  /**
   * Dispatch a task to a worker thread
   *
   * @param task
   * @param threadName
   */
  public static Thread dispatchOnAWorkerThread(final Runnable task, String threadName) {
    final Thread worker;
    try {
      worker =
          new Thread(threadGroup, threadName) {
            @Override
            public void run() {
              task.run();
            }
          };
      worker.start();
      return worker;
    } catch (Exception ex) {
      System.err.println("dispatchOnWorkerThread exception: " + ex); // NOI18N
    }
    return null;
  }

  private static class AnThreadGroup extends ThreadGroup {

    public AnThreadGroup() {
      super("analyzer_group"); // NOI18N
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
      e.printStackTrace();
      String tmp = e.getLocalizedMessage();
      String msg =
          AnLocale.getString("Uncaught Exception: ")
              + e
              + '\n'
              + // NOI18N
              ((tmp == null)
                  ? AnLocale.getString("gp-display-text exited due to internal error") + '\n'
                  : "")
              + // NOI18N
              AnLocale.getString("Exception in thread ")
              + t.getName()
              + '.'; // NOI18N
      Analyzer.getInstance().endIPC(msg);
    }
  }

  public static boolean checkIfOnAWTThread(boolean shouldBe) {
    // FIXUP: enable after Beta
    boolean ok = true;
    if (SwingUtilities.isEventDispatchThread()) {
      if (!shouldBe) {
        reportWrongThread(shouldBe);
        ok = false;
      }
    } else if (shouldBe) {
      reportWrongThread(shouldBe);
      ok = false;
    }
    return ok;
  }

  private static void reportWrongThread(boolean shouldBe) {
    if ((debugFlags & DEBUG_AWT_THR) == 0) {
      return;
    }
    Exception awtException = new Exception();
    StackTraceElement[] se = awtException.getStackTrace();
    StringBuilder s = new StringBuilder();
    String msg = shouldBe ? "Should be on AWT" : "Should not be on AWT";
    s.append("*** Wrong Thread: " + msg + "\n");
    s.append(Thread.currentThread().getName() + "\n");
    for (int i = 0; i < se.length; i++) {
      s.append(se[i].toString() + "\n");
    }
    AnLog.log(s.toString());
    System.err.println("DEBUG: Invalid use of AWT thread. See log file: "
        + AnLog.getLogFilePath());
  }

  public static void checkIPCOnWrongThread(boolean set) {
    if (set) {
      checkIPCOnWrongThreadCounter--;
    } else {
      checkIPCOnWrongThreadCounter++;
    }
  }

  public static boolean checkIPCOnWrongThread() {
    return checkIPCOnWrongThread && checkIPCOnWrongThreadCounter == 0;
  }

  public static String toFullPath(String path) {
    String ret = path;
    if (!path.startsWith("/")) { // NOI18N
      ret = Analyzer.getInstance().getWorkingDirectory() + "/" + path; // NOI18N
    }
    return ret;
  }

  /**
   * Remove duplicate '/' characters in path:
   * @param path
   * @return
   */
  public static String clearPath(String path) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) == '/' && i > 0 && path.charAt(i - 1) == '/') {
          continue;
      }
      sb.append(path.charAt(i));
    }
    return sb.toString();
  }

  public static String getShortString(String string, int maxLength) {
    int len = string.length();
    if ((len - 1) <= maxLength) {
      return string;
    } else {
      return "..." + string.substring(len - maxLength); // NOI18N
    }
  }

  public static void copyDirectory(File fromDirectoryFile, File toDirectoryFile)
      throws IOException {
    if (fromDirectoryFile.exists() && fromDirectoryFile.isDirectory()) {
      if (!toDirectoryFile.exists()) {
        toDirectoryFile.mkdirs();
      }

      String[] children = fromDirectoryFile.list();
      for (String children1 : children) {
        File fromFile = new File(fromDirectoryFile, children1);
        File toFile = new File(toDirectoryFile, children1);
        if (fromFile.isDirectory()) {
          copyDirectory(fromFile, toFile);
        } else {
          copyFile(fromFile, toFile);
        }
      }
    } else {
      // FIXUP: do something reasonable
    }
  }

  public static void copyFile(File fromFile, File toFile) throws IOException {
    if (fromFile.exists() && !fromFile.isDirectory()) {
      InputStream inputStream = new FileInputStream(fromFile);
      OutputStream outputStream = new FileOutputStream(toFile);

      byte[] buffer = new byte[1024];
      int nbytes;
      while ((nbytes = inputStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, nbytes);
      }
      inputStream.close();
      outputStream.close();
    } else {
      // FIXUP: do something reasonable
    }
  }

  public static void copyToClipboard(String text) {
    if (text != null && text.length() > 0) {
      StringSelection data = new StringSelection(text);
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(data, data);
    }
  }

  public static String keyStrokeToStringFormatted(KeyStroke keyStroke) {
    return "  (" + keyStrokeToString(keyStroke) + ")"; // NOI18N
  }

  /** Simple KeyStroke to String. Does not (yet) handle META, ALT modifiers and some other keys. */
  public static String keyStrokeToString(KeyStroke keyStroke) {
    if (keyStroke == null) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    int modifiers = keyStroke.getModifiers();

    if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
      buf.append("Ctrl+"); // NOI18N
    }
    if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
      buf.append("Meta+"); // NOI18N
    }
    if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
      buf.append("Alt+"); // NOI18N
    }
    if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
      buf.append("Shift+"); // NOI18N
    }
    if (buf.length() > 0) {
      buf.deleteCharAt(buf.length() - 1);
      buf.append("-"); // NOI18N
    }
    int x = keyStroke.getKeyCode();
    switch (x) {
      case KeyEvent.VK_F1:
        buf.append("F1");
        break;
      case KeyEvent.VK_F2:
        buf.append("F2");
        break;
      case KeyEvent.VK_F3:
        buf.append("F3");
        break;
      case KeyEvent.VK_F4:
        buf.append("F4");
        break;
      case KeyEvent.VK_F5:
        buf.append("F5");
        break;
      case KeyEvent.VK_F6:
        buf.append("F6");
        break;
      case KeyEvent.VK_F7:
        buf.append("F7");
        break;
      case KeyEvent.VK_F8:
        buf.append("F8");
        break;
      case KeyEvent.VK_F9:
        buf.append("F9");
        break;
      case KeyEvent.VK_F10:
        buf.append("F10");
        break;
      case KeyEvent.VK_F11:
        buf.append("F11");
        break;
      case KeyEvent.VK_F12:
        buf.append("F21");
        break;
      case KeyEvent.VK_UP:
        buf.append("Up");
        break;
      case KeyEvent.VK_DOWN:
        buf.append("Down");
        break;
      case KeyEvent.VK_LEFT:
        buf.append("Left");
        break;
      case KeyEvent.VK_RIGHT:
        buf.append("Right");
        break;
      case KeyEvent.VK_ENTER:
        buf.append("Enter");
        break;
      case KeyEvent.VK_BACK_SPACE:
        buf.append("Backspace");
        break;
      default:
        buf.append((char) (keyStroke.getKeyCode()));
        break;
    }

    return buf.toString();
  }

  private static int getSignalValueIPC(final String signal) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getSignalValue"); // NOI18N
      anWindow.IPC().send(signal);
      return anWindow.IPC().recvInt();
    }
  }

  public static String escapeSpecialHTMLCharacters(String s) {
    s = s.replace("&", "&amp;"); // NOI18N
    s = s.replace("<", "&lt;"); // NOI18N
    s = s.replace(">", "&gt;"); // NOI18N
    s = s.replace("'", "&apos;"); // NOI18N
    s = s.replace("\"", "&quot;"); // NOI18N
    return s;
  }

  public static int debugFlags = 0;
  public static int DEBUG_AWT_THR = 1;
}

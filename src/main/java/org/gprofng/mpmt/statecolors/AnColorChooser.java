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

package org.gprofng.mpmt.statecolors;

import org.gprofng.mpmt.AnDialog;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer.HelpAction;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import javax.accessibility.AccessibleContext;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// YXXX some kind of synchronization needs to be added to color classes; they
// are sometimes accessed from non-awt threads
public final class AnColorChooser extends AnDialog
    implements ChangeListener, ListSelectionListener, MouseListener {

  private JColorChooser chooser;
  private JButton set_sel, set_all, reset, set_class;
  private JComboBox<String> combo_class;
  private JTextField str_class;
  private JRadioButton idle_normal, idle_inv, idle_color;
  private JLabel idle_label;

  private ChooserStateView function_list;
  private final StateColorMap color_map;

  // Constructor
  public AnColorChooser(final AnWindow awindow, final Frame frame) {
    super(awindow, frame, AnLocale.getString("Function Colors"), false, null, null, null);

    color_map = new StateColorMap();
    initComponents();

    ok.setVisible(false);
    apply.setVisible(false);
  }

  // Initialize GUI components
  private void initComponents() {
    final JComponent preview;
    JPanel panel;
    final JPanel set_panel;
    final JPanel legend_panel;
    final ImageIcon icon;
    final ButtonGroup group;

    JButton closeButton = getDefaultButtons()[2];
    //        closeButton.setMnemonic(AnLocale.getString('e', "ColorChooserCloseButtonMN"));
    getRootPane().setDefaultButton(closeButton);

    // Color chooser panel
    chooser = new JColorChooser();
    AnUtility.setAccessibleContext(
        chooser.getAccessibleContext(), AnLocale.getString("Color chooser"));
    chooser.setPreviewPanel(new JPanel());
    chooser.getSelectionModel().addChangeListener(this);

    preview = (JComponent) chooser.getPreviewPanel().getParent();
    if (preview != null) {
      preview.setBorder(null);
    }

    icon = new ImageIcon(StackState.createIcon(chooser.getColor(), 12, 12, true));

    set_panel = new JPanel();
    set_panel.setLayout(new BoxLayout(set_panel, BoxLayout.Y_AXIS));

    // Set-all & Reset buttons
    set_sel = new JButton(AnLocale.getString("Set Selected Functions"), icon);
    AnUtility.setAccessibleContext(set_sel.getAccessibleContext(), set_sel.getText());
    set_sel.setIconTextGap(8);
    set_sel.setMnemonic(AnLocale.getString('u', "MNEM_COLOR_CHOOSER_SET_SELECTED_FUNCTIONS"));
    set_sel.addActionListener(this);

    set_all = new JButton(AnLocale.getString("Set All Functions"), icon);
    AnUtility.setAccessibleContext(set_all.getAccessibleContext(), set_all.getText());
    set_all.setIconTextGap(8);
    set_all.setMnemonic(AnLocale.getString('A', "MNEM_COLOR_CHOOSER_SET_ALL_FUNCTIONS"));
    set_all.addActionListener(this);

    reset = new JButton(AnLocale.getString("Reset Default Colors"));
    AnUtility.setAccessibleContext(reset.getAccessibleContext(), reset.getText());
    reset.setMnemonic(AnLocale.getString('R', "MNEM_COLOR_CHOOSER_RESET_DEFAULT_COLORS"));
    reset.addActionListener(this);

    panel = new JPanel(new GridLayout(1, 0, 4, 0));
    panel.add(set_sel);
    panel.add(set_all);
    panel.add(reset);

    set_panel.add(panel);
    set_panel.add(Box.createVerticalStrut(4));

    // Set class color
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

    set_class = new JButton(AnLocale.getString("Set Functions:"), icon);
    AnUtility.setAccessibleContext(set_class.getAccessibleContext(), set_class.getText());
    set_class.setIconTextGap(8);
    set_class.setMnemonic(AnLocale.getString('F', "MNEM_COLOR_CHOOSER_SET_FUNCTIONS"));
    set_class.addActionListener(this);
    panel.add(set_class);

    combo_class = new JComboBox<>(ColorRule.SETC_STR);
    AnUtility.setAccessibleContext(
        combo_class.getAccessibleContext(), AnLocale.getString("Function filter"));
    panel.add(combo_class);

    str_class = new JTextField("", 10);
    AnUtility.setAccessibleContext(
        str_class.getAccessibleContext(), AnLocale.getString("Function text"));
    panel.add(str_class);

    set_panel.add(panel);
    set_panel.add(Box.createVerticalStrut(4));
    set_panel.add(new JSeparator(SwingConstants.HORIZONTAL));
    set_panel.add(Box.createVerticalStrut(4));

    // Set CPU idle color
    idle_normal = new JRadioButton(AnLocale.getString("Normal", "COLOR_CHOOSER_NORMAL"), true);
    AnUtility.setAccessibleContext(idle_normal.getAccessibleContext(), idle_normal.getText());
    idle_normal.setMnemonic(AnLocale.getString('N', "MNEM_COLOR_CHOOSER_NORMAL"));
    idle_normal.addActionListener(this);

    idle_inv = new JRadioButton(AnLocale.getString("Invisible"), false);
    AnUtility.setAccessibleContext(idle_inv.getAccessibleContext(), idle_inv.getText());
    idle_inv.setMnemonic(AnLocale.getString('I', "MNEM_COLOR_CHOOSER_INVISIBLE"));
    idle_inv.addActionListener(this);

    idle_color = new JRadioButton(AnLocale.getString("Selected Color"), false);
    AnUtility.setAccessibleContext(idle_color.getAccessibleContext(), idle_color.getText());
    idle_color.setMnemonic(AnLocale.getString('o', "MNEM_COLOR_CHOOSER_SELECTED_COLOR"));
    idle_color.addActionListener(this);
    idle_label = new JLabel(icon);
    AnUtility.setAccessibleContext(
        idle_label.getAccessibleContext(), AnLocale.getString("Selected Color"));

    group = new ButtonGroup();
    group.add(idle_normal);
    group.add(idle_inv);
    group.add(idle_color);

    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    final JLabel lbl = new JLabel(AnLocale.getString("Set CPU Idle Events Color:"));
    panel.add(lbl);
    lbl.getAccessibleContext().setAccessibleName(AnLocale.getString("CPU Idle Events Color"));
    lbl.getAccessibleContext()
        .setAccessibleDescription(AnLocale.getString("Set CPU Idle Events Color"));
    panel.add(idle_normal);
    panel.add(idle_inv);
    panel.add(idle_color);
    panel.add(idle_label);
    panel.add(Box.createGlue());

    set_panel.add(panel);

    // Function List Pane
    function_list = new ChooserStateView(color_map);
    function_list.addListSelectionListener(this);
    function_list.addMouseListener(this);

    JScrollPane functionListPane = new AnJScrollPane(function_list);
    HelpAction helpAction = new HelpAction(AnVariable.HELP_TabsLegend);
    panel.registerKeyboardAction(
        helpAction,
        "help",
        KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    panel.registerKeyboardAction(
        helpAction, "help", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), JComponent.WHEN_FOCUSED);
    panel.registerKeyboardAction(
        helpAction,
        "help",
        KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);
    AccessibleContext context = functionListPane.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Function List"));
    context.setAccessibleDescription(AnLocale.getString("Function List"));
    context = function_list.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Function List"));
    context.setAccessibleDescription(AnLocale.getString("Function List"));

    // Legend panel to display functions and related colors
    legend_panel = new JPanel();
    legend_panel.setLayout(new BoxLayout(legend_panel, BoxLayout.Y_AXIS));
    JTabbedPane pane = new JTabbedPane();
    pane.addTab(AnLocale.getString("Legend"), functionListPane);
    AnUtility.setAccessibleContext(pane.getAccessibleContext(), AnLocale.getString("Legend"));
    legend_panel.add(pane);

    // Set chooser outline
    panel = new JPanel(new BorderLayout());
    panel.add(chooser, BorderLayout.WEST);
    panel.add(legend_panel, BorderLayout.CENTER);
    panel.add(set_panel, BorderLayout.SOUTH);

    setAccessory(panel);
  }

  // Action performed
  @Override
  public void actionPerformed(final ActionEvent event) {
    final String cmd = event.getActionCommand();
    if (cmd.equals(AnLocale.getString("Set Selected Functions"))) {
      int indices[] = function_list.getSelectedIndices();
      int size = function_list.getModel().getSize();
      Color color = chooser.getColor();
      if (false /* indices.length == size && size>0 */) { // treat "ctrl-a" as all
        color_map.setRuleAllFunctions(color);
      } else {
        ArrayList<ColorRule> rules = new ArrayList<>();
        for (int jj = 0; jj < indices.length; jj++) {
          int ii = indices[jj];
          StackState state = function_list.getState(ii);
          if (state != null) {
            ColorRule rule = new ColorRule(color, ColorRule.SETC_MATCHES, state.getName(), false);
            rules.add(rule);
          }
        }
        color_map.setRules(rules);
      }
    } else if (cmd.equals(AnLocale.getString("Set All Functions"))) {
      color_map.setRuleAllFunctions(chooser.getColor());
    } else if (cmd.equals(AnLocale.getString("Reset Default Colors"))) {
      color_map.restoreToDefault();
    } else if (cmd.equals(AnLocale.getString("Set Functions:"))) {
      String spec = str_class.getText();
      spec = spec.trim();
      if (!spec.isEmpty()) {
        color_map.setRule(chooser.getColor(), combo_class.getSelectedIndex(), spec, false);
      }
    } else if (cmd.equals(AnLocale.getString("Normal"))) {
      window.setCPUIdleColor(false, null); // FIXUP: REARCH
    } else if (cmd.equals(AnLocale.getString("Invisible"))) {
      window.setCPUIdleColor(true, null); // FIXUP: REARCH
    } else if (cmd.equals(AnLocale.getString("Selected Color"))) {
      window.setCPUIdleColor(true, chooser.getColor()); // FIXUP: REARCH
    }
  }

  // Update color from the color chooser
  @Override
  public void stateChanged(final ChangeEvent event) {
    final ImageIcon icon;

    icon = new ImageIcon(StackState.createIcon(chooser.getColor(), 12, 12, true));
    set_class.setIcon(icon);
    set_sel.setIcon(icon);
    set_all.setIcon(icon);
    idle_label.setIcon(icon);
  }

  // (MUST BE CALLED ON AWT THREAD)
  private void funcSelChanged(EventObject event) {
    if (event.getSource() instanceof StackView) { // ChooserStateView
      StackView list = (StackView) event.getSource();
      StackState state = (StackState) list.getSelectedValue();
      if (state != null) {
        setSelectedFunction(state.getNumber());
      }
    }
  }

  @Override
  public void valueChanged(final ListSelectionEvent event) {
    funcSelChanged(event);
  }

  public final StateColorMap getColorMap() {
    return color_map;
  }

  // (MUST BE CALLED ON AWT THREAD)
  // <number> should be Histable::Function* (not Function->id)
  public void setSelectedFunction(long number) {
    Color c = color_map.getFuncColor(number);
    function_list.setSelectedFunction(number);
    chooser.setColor(c);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (!SwingUtilities.isLeftMouseButton(event)) {
      return;
    }

    funcSelChanged(event);
  }

  @Override
  public void mousePressed(MouseEvent e) {}

  @Override
  public void mouseReleased(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}
}

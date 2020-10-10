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

package org.gprofng.mpmt.collect;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.accessibility.AccessibleContext;
import javax.swing.BoundedRangeModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public final class CollectUtility {

  private static final int TEXT_LEFT = 1;
  // public final static int     TEXT_CENTER = 2;
  public static final int TEXT_RIGHT = 3;

  public static final Font text_font;
  public static final Color text_color;
  private static final Color LABEL_COLOR = Color.black;
  private static final int sizeLabel = 4;

  private static final Border labelBorder = new EmptyBorder(0, sizeLabel, 0, sizeLabel);
  public static final Insets buttonMargin = new Insets(1, 1, 1, 1);

  static {
    Font org_font = UIManager.getDefaults().getFont("TextField.font");

    text_font = new Font("Monospaced", org_font.getStyle(), org_font.getSize());
    if (AnEnvironment.isLFNimbus()) {
      text_color = AnEnvironment.NIMBUS_TEXT_BACKGROUND;
    } else {
      text_color = new Color(UIManager.getDefaults().getColor("TextArea.background").getRGB());
    }
  }

  protected static final class CLabel extends JLabel {

    public CLabel(final String text, final int alignment) {
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
      setForeground(LABEL_COLOR);
      setBorder(labelBorder);
    }
  }

  protected static final class CList extends JPanel {

    private final boolean resizable;
    private final JPanel label, value;

    // Constructor
    public CList(final boolean resizable) {
      this.resizable = resizable;

      label = new JPanel();
      label.setLayout(new BoxLayout(label, BoxLayout.Y_AXIS));
      value = new JPanel();
      value.setLayout(new BoxLayout(value, BoxLayout.Y_AXIS));

      if (resizable) {
        setLayout(new BorderLayout());

        add(label, BorderLayout.WEST);
        add(value, BorderLayout.CENTER);
      } else {
        add(label);
        add(value);
      }
    }

    // Add label
    public JComponent addLabel(final JComponent item) {
      return (JComponent) label.add((item != null) ? item : new JPanel());
    }

    // Add value
    public JComponent addValue(final JComponent item) {
      return (JComponent) value.add((item != null) ? item : new JPanel());
    }

    // Add label and value
    public void add(final JComponent litem, final JComponent vitem) {
      setAlignmentY(addLabel(litem), addValue(vitem));
    }

    // Align labels and values vertically
    public void setAlignmentY(final JComponent litem, final JComponent vitem) {
      final Dimension lsize, vsize;
      Dimension msize;

      lsize = new Dimension(litem.getPreferredSize());
      vsize = new Dimension(vitem.getPreferredSize());

      if (lsize.height < vsize.height) {
        lsize.height = vsize.height;
      } else {
        vsize.height = lsize.height;
      }

      // Set labels size
      if (resizable) {
        msize = litem.getMaximumSize();
        msize.height = lsize.height;
        litem.setMaximumSize(msize);
      } else {
        litem.setMaximumSize(new Dimension(lsize));
      }

      litem.setMinimumSize(new Dimension(lsize));
      litem.setPreferredSize(new Dimension(lsize));

      // Set values size
      if (resizable) {
        msize = vitem.getMaximumSize();
        msize.height = vsize.height;
        vitem.setMaximumSize(msize);
      } else {
        vitem.setMaximumSize(new Dimension(vsize));
      }
      vitem.setMinimumSize(new Dimension(vsize));
      vitem.setPreferredSize(new Dimension(vsize));
    }

    // Align Labels and Values vertically
    public void setAlignmentY() {
      final Component[] labels = label.getComponents();
      final Component[] values = value.getComponents();
      final int size = Math.min(labels.length, values.length);

      for (int i = 0; i < size; i++) {
        setAlignmentY((JComponent) labels[i], (JComponent) values[i]);
      }
    }

    // Align items horizontally
    public void setAlignmentX(final Component[] items) {
      final int size;
      int width, max_width;
      Dimension psize;

      size = items.length;
      max_width = 0;
      for (int i = 0; i < size; i++) {
        width = items[i].getPreferredSize().width;
        if (max_width < width) {
          max_width = width;
        }
      }

      for (int i = 0; i < size; i++) {
        psize = items[i].getPreferredSize();
        psize.width = max_width;
        ((JComponent) items[i]).setPreferredSize(psize);
      }
    }

    // Align the Values only horizontally, usually the Labels does not need
    // further alignment
    public void setAlignmentX() {
      setAlignmentX(value.getComponents());
    }

    // Clean up
    @Override
    public void removeAll() {
      label.removeAll();
      value.removeAll();
    }
  }

  // General textfield
  protected static class CText extends JTextField {

    int text_align;

    public CText(final String text, final int columns, final int alignment) {
      super(text, columns);

      text_align = TEXT_LEFT;

      setFont(text_font);
      setBackground(text_color);
      setEditable(false);
      setHorizontalAlignment(alignment);
      setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

      setCaret(
          new javax.swing.text.DefaultCaret() {
            public void focusGained(final FocusEvent event) {
              if (getComponent().isEnabled()) {
                setVisible(true);
              }

              setSelectionVisible(true);

              // Don't use selectAll which will move Caret to the end.
              if (text_align == TEXT_LEFT) {
                setDot(getDocument().getLength());
                moveDot(0);
              } else { // Same code as selectAll
                setDot(0);
                moveDot(getDocument().getLength());
              }
            }

            public void focusLost(final FocusEvent event) {
              super.focusLost(event);

              setDot(text_align == TEXT_LEFT ? 0 : getDocument().getLength());
            }
          });
    }

    public final void setVisibleAlign(final int text_align, final boolean delay) {
      final BoundedRangeModel visibility;

      this.text_align = text_align;

      if (delay) {
        setCaretPosition(text_align == TEXT_LEFT ? 0 : getDocument().getLength());
      } else {
        // Don't use setCaretPosition which caused flash
        visibility = getHorizontalVisibility();
        visibility.setValue(
            text_align == TEXT_LEFT ? visibility.getMinimum() : visibility.getMaximum());
      }
    }
  }

  protected static class AnComboBox extends JComboBox implements ActionListener {

    public static final int COMBO_TEXT = 0;
    public static final int COMBO_ITEM = 1;
    private AnComboBox.ComboObj current_obj;
    private final int width;

    public AnComboBox(final int width) {
      current_obj = null;
      this.width = width;

      setRenderer(new AnComboBox.AnComboRenderer());
      setEditor(new AnComboBox.AnComboEditor());
      setEditable(true);
      //            setBorder(new EtchedBorder(EtchedBorder.LOWERED));
      setBorder(new LineBorder(new Color(150, 150, 150), 1));
      //            setBorder(null);
    }

    public AnComboBox() {
      this(14); // default width
    }

    // Add item
    public final void add(
        final int type, final String header, final String str, final boolean changable) {
      addItem(new AnComboBox.ComboObj(type, false, header, str, changable));
    }

    public final void setText(final String str) {
      final AnComboBox.ComboObj obj = (AnComboBox.ComboObj) getEditor().getEditorComponent();
      if (obj.getType() == COMBO_TEXT) {
        obj.setText(str);
        if (current_obj.changable) {
          current_obj.setText(str);
        }
      }
    }

    // Get result
    public final String getText() {
      final AnComboBox.ComboObj obj = (AnComboBox.ComboObj) getEditor().getEditorComponent();

      if (obj.getType() == COMBO_ITEM) {
        return null;
      }

      return obj.getText();
    }

    // Get header text
    public final String getHeader() {
      final AnComboBox.ComboObj obj = (AnComboBox.ComboObj) getEditor().getEditorComponent();

      return obj.getHeader();
    }

    // Get editor's editable textfield
    public final Component getEditorText() {
      return ((AnComboBox.ComboObj) getEditor().getEditorComponent()).getTextField();
    }

    // Get location of the Editor's textfield
    public final Point getEditorLocation() {
      return ((AnComboBox.AnComboEditor) getEditor()).getTextLocation();
    }

    // Enter in the textfield
    public final void actionPerformed(final ActionEvent event) {
      final Object src = event.getSource();

      if (src instanceof JTextField) {
        current_obj.setText(((JTextField) src).getText());
      }
    }

    // Internal data object
    private final class ComboObj extends JPanel {

      private final boolean editor, changable;
      private int type;
      private String header;
      private final JLabel label;
      private final JTextField text;

      public ComboObj(
          final int type,
          final boolean editor,
          final String header,
          final String str,
          final boolean changable) {
        final BoxLayout layout;

        this.editor = editor;
        this.type = type;
        this.header = header;
        this.changable = changable;

        setLayout(layout = new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(new EmptyBorder(0, 1, 0, 0));

        label = new CLabel(header, JLabel.LEFT);
        label.setFont(label.getFont().deriveFont(Font.PLAIN));

        final AccessibleContext context = label.getAccessibleContext();
        context.setAccessibleName(header);
        context.setAccessibleDescription(header);

        text = new JTextField((type == COMBO_TEXT) ? width : 0);
        text.setFont(text_font);
        text.setBorder(null);

        this.add(label);
        this.add(text);

        setText(str);
        layout.layoutContainer(this);
      }

      public final int getType() {
        return type;
      }

      public final void setType(final int type) {
        this.type = type;
      }

      public final String getHeader() {
        return header;
      }

      public final void setHeader(final String header) {
        this.header = header;
        label.setText(header);
      }

      public final String getText() {
        return text.getText();
      }

      public final void setText(final String str) {
        text.setText(str);
        text.setEditable(editor && (type == COMBO_TEXT));
      }

      public final JTextField getTextField() {
        return text;
      }

      public final void selectAll() {
        text.selectAll();
        text.requestFocus();
      }

      public void addActionListener(final ActionListener l) {
        text.addActionListener(l);
      }

      public final void removeActionListener(final ActionListener l) {
        text.removeActionListener(l);
      }

      // Set background
      @Override
      public final void setBackground(final Color bg) {
        super.setBackground(bg);

        if (text != null) {
          text.setBackground(bg);
        }

        if (label != null) {
          label.setBackground(bg);
        }
      }

      // Set foreground
      @Override
      public final void setForeground(final Color fg) {
        super.setForeground(fg);

        if (text != null) {
          text.setForeground(fg);
        }

        if (label != null) {
          label.setForeground(fg);
        }
      }
    }

    // Renderer for AnCombo
    private final class AnComboRenderer extends DefaultListCellRenderer {

      @Override
      public final Component getListCellRendererComponent(
          final JList list,
          final Object value,
          final int index,
          final boolean isSelected,
          final boolean cellHasFocus) {
        final AnComboBox.ComboObj obj = (AnComboBox.ComboObj) value;

        if (isSelected) {
          obj.setBackground(list.getSelectionBackground());
          obj.setForeground(list.getSelectionForeground());
        } else {
          obj.setBackground(list.getBackground());
          obj.setForeground(list.getForeground());
        }

        return obj;
      }
    }

    // Editor for AnCombo
    private final class AnComboEditor implements ComboBoxEditor, FocusListener {

      private final AnComboBox.ComboObj editor;
      private final JTextField text;

      public AnComboEditor() {
        editor = new AnComboBox.ComboObj(COMBO_TEXT, true, "", "", true);
        editor.setBorder(null);

        text = editor.getTextField();
        text.addFocusListener(this);
      }

      // Return the component
      @Override
      public Component getEditorComponent() {
        return editor;
      }

      // Set the item that should be edited.
      @Override
      public void setItem(final Object anObject) {
        final AnComboBox.ComboObj obj = (AnComboBox.ComboObj) anObject;
        final int type;

        if (obj != null) {
          current_obj = obj;
          type = obj.getType();

          if (type == COMBO_TEXT) {
            text.requestFocus();
          }

          editor.setType(type);
          editor.setHeader(obj.getHeader());
          editor.setText(obj.getText());
        }
      }

      // Return the edited item
      @Override
      public Object getItem() {
        return editor;
      }

      // Get location of the textfield
      public Point getTextLocation() {
        return text.getLocationOnScreen();
      }

      // Ask the editor to start editing and to select everything
      @Override
      public void selectAll() {
        editor.selectAll();
      }

      // Add an ActionListener. An action event is generated when the edited
      // item changes
      @Override
      public void addActionListener(final ActionListener l) {
        editor.addActionListener(l);
      }

      // Remove an ActionListener
      @Override
      public void removeActionListener(final ActionListener l) {
        editor.removeActionListener(l);
      }

      // Lost focus
      @Override
      public void focusLost(final FocusEvent event) {
        if (event.isTemporary()) {
          return;
        }

        if (current_obj.changable) {
          current_obj.setText(text.getText());
        } else {
          text.setText(current_obj.getText());
        }
      }

      // Gain focus
      @Override
      public void focusGained(final FocusEvent event) {}
    }
  }

  protected interface Browseable {

    public void setValue(String path);

    public String getValue();
  }

  protected static JComponent getItem(final String text) {
    return new CLabel(text, JLabel.RIGHT);
  }
}

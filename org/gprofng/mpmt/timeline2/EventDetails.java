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


package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.AnInteger;
import org.gprofng.mpmt.AnList;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.timeline.events.EventDetail;
import org.gprofng.mpmt.timeline.events.ExtendedEvent;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Vector;
import javax.accessibility.AccessibleContext;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

// Initial version derived from timeline/TimelineDisp.java
// YXXX Ideally, clean this up for TL2
public class EventDetails {
  private static final int NUM_EVT_SPC_LBL = 20; // YXXX yuck: max # of eventSpecLabels/Text

  // state
  private boolean extendedShowing; // show extended fields & stacks on RH tab
  private ExtendedEvent previousSelectedEvent; // previous user selection
  private String[] oldLabels; // previous selection's details

  // panel to hold details
  private JPanel tbPanel;
  private AnList anList; // container for event details

  // fixed fields
  private JTextField expTxt;
  private JTextField typeTxt;
  private JTextField functionTxt;
  private JTextField timeTxt;
  private JTextField lwpTxt;
  private JTextField thrTxt;
  private JTextField cpuTxt;

  // event-specific fields
  private JTextField[] eventSpecText;
  private JLabel[] eventSpecLabels;

  private ArrayList<JTextField> allFields;
  private ArrayList<JTextField> extendedFields;
  private ArrayList<JLabel> extendedLabels;

  // Constructor
  public EventDetails(final AnWindow awindow) {
    extendedShowing = true;
    doInit();
  }

  // Initialize the event panel on right side
  public void doInit() {
    tbPanel = new JPanel(new BorderLayout());

    // EventDetail Pane: Timeline selection
    allFields = new ArrayList<JTextField>();
    extendedFields = new ArrayList<JTextField>();
    extendedLabels = new ArrayList<JLabel>();

    anList = new AnList(true);
    AccessibleContext context = anList.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Events"));
    context.setAccessibleDescription(AnLocale.getString("Events"));
    anList.setAlignmentY();
    anList.setBorder(AnVariable.boxBorder);

    expTxt = createField(AnLocale.getString("Process:")); // ,
    //                AnLocale.getString('E', "MNEM_TIMELINE_EVENT_EXPERIMENT_NAME"), anList);
    typeTxt = createField(AnLocale.getString("Event Type:", "TIMELINE_EVENT_EVENT_TYPE")); // ,
    //                AnLocale.getString('p', "MNEM_TIMELINE_EVENT_EVENT_TYPE"), anList);
    functionTxt = createField(AnLocale.getString("Leaf Function:")); // ,
    //                AnLocale.getString('n', "MNEM_TIMELINE_EVENT_LEAF_FUNCTION"), anList);
    timeTxt = createField(AnLocale.getString("Timestamp (sec.):")); // ,
    //                AnLocale.getString('i', "MNEM_TIMELINE_EVENT_TIMESTAMP"), anList);
    lwpTxt = createField(AnLocale.getString("LWP:")); // ,
    //                AnLocale.getString('W', "MNEM_TIMELINE_EVENT_LWP"), anList);
    thrTxt = createField(AnLocale.getString("Thread:")); // ,
    //                AnLocale.getString('h', "MNEM_TIMELINE_EVENT_THREAD"), anList);
    cpuTxt = createField(AnLocale.getString("CPU:")); // ,
    //                AnLocale.getString('c', "MNEM_TIMELINE_EVENT_CPU"), anList);

    eventSpecLabels = new JLabel[NUM_EVT_SPC_LBL];
    eventSpecText = new JTextField[NUM_EVT_SPC_LBL];

    final String longest;
    {
      int len = 0, ind = 0, strlen;
      final int numNames = TimelineVariable.mstate_info.getNumNames();
      for (int i = 0; i < numNames; i++) {
        String name = TimelineVariable.mstate_info.getNameByDisplayIdx(i);
        strlen = name.length();
        if (strlen > len) {
          len = strlen;
          ind = i;
        }
      }
      longest =
          TimelineVariable.mstate_info.getNameByDisplayIdx(
              ind); // YXXX bogus, really depends on font
    }

    for (int i = 0; i < NUM_EVT_SPC_LBL; i++) {
      AnUtility.AnLabel label =
          new AnUtility.AnLabel(
              longest + " ", new ImageIcon(StackState.createIcon(Color.black)), JLabel.RIGHT, true);
      //            label.setDisplayedMnemonic('z');
      eventSpecLabels[i] = label;
      eventSpecText[i] = (JTextField) AnUtility.getText("", 0);

      eventSpecLabels[i].setVisible(false);
      eventSpecText[i].setVisible(false);

      eventSpecLabels[i].setLabelFor(eventSpecText[i]);

      anList.add(eventSpecLabels[i], eventSpecText[i]);
      allFields.add(eventSpecText[i]);
    }

    tbPanel.add(anList);
  }

  // Creates the label and textfield pairs for the event pane on right
  private JTextField createField(final String label) { // , final char mnemonic,
    //            final AnList list) {
    final AnList list = anList;
    final JLabel lbl;
    final JTextField fld;

    lbl = (JLabel) AnUtility.getItem(label);
    fld = (JTextField) AnUtility.getText("", 0);

    //        lbl.setDisplayedMnemonic(mnemonic);
    lbl.setLabelFor(fld);
    AccessibleContext context = lbl.getAccessibleContext();
    context.setAccessibleName(label);
    context.setAccessibleDescription(label);
    context = fld.getAccessibleContext();
    context.setAccessibleName(label);
    context.setAccessibleDescription(label);
    list.add(lbl, fld);
    allFields.add(fld);
    extendedFields.add(fld);
    extendedLabels.add(lbl);

    return fld;
  }

  private void clearTimeline() {
    oldLabels = null;
    for (int i = 0; i < NUM_EVT_SPC_LBL; i++) {
      eventSpecText[i].setVisible(false);
      eventSpecLabels[i].setVisible(false);
    }

    setExtendedFields(true);

    for (JTextField e : allFields) {
      e.setText("");
    }
  }

  public JPanel setEvent(TimelineSelectionEvent evt) {
    EventDetail basicEvent = evt.eventDetail;
    RowDefinition rowDef = evt.getRowDefinition(); // may be null

    if (basicEvent == null) {
      oldLabels = null;
      for (int i = 0; i < NUM_EVT_SPC_LBL; i++) {
        eventSpecText[i].setVisible(false);
        eventSpecLabels[i].setVisible(false);
      }

      setExtendedFields(true);
      for (JTextField e : allFields) {
        e.setText("");
      }
      return tbPanel;
    }

    final String[] labels, info;
    //        final char[] mnemonics;
    final ImageIcon[] icons;
    labels = basicEvent.getEventSpecificLabels();
    //        mnemonics = basicEvent.getEventSpecificMnemonics();
    icons = basicEvent.getEventSpecificIcons();

    if (labels != oldLabels) {
      for (int i = 0; i < labels.length; i++) {
        eventSpecLabels[i].setText(labels[i]);
        //                eventSpecLabels[i].setDisplayedMnemonic(
        //                        (i < mnemonics.length) ? mnemonics[i] : '\0');

        if (i < icons.length) {
          eventSpecLabels[i].setIcon(icons[i]);
          eventSpecLabels[i].setHorizontalTextPosition(SwingConstants.LEADING);
        } else {
          eventSpecLabels[i].setIcon(null);
        }

        eventSpecLabels[i].setVisible(true);
      }

      for (int i = labels.length; i < NUM_EVT_SPC_LBL; i++) {
        eventSpecLabels[i].setVisible(false);
      }
    }

    oldLabels = labels;

    info = basicEvent.getEventSpecificInfo();
    if (info == null) {
      return tbPanel;
    }

    for (int i = 0; i < info.length; i++) {
      eventSpecText[i].setText(info[i]);
      eventSpecText[i].setVisible(true);
    }

    for (int i = info.length; i < NUM_EVT_SPC_LBL; i++) {
      eventSpecText[i].setVisible(false);
    }

    if (!(basicEvent instanceof ExtendedEvent)) {
      if (extendedShowing) {
        setExtendedFields(false);
      }
      // YXXX update_event(evt);
      return tbPanel;
    }

    if (!extendedShowing) {
      setExtendedFields(true);
    }

    final ExtendedEvent event = (ExtendedEvent) basicEvent;

    if (event == previousSelectedEvent) {
      return tbPanel;
    }

    // Fill in text field values in the EventDetail pane based on the
    // current event selection
    final String fName;
    Vector<StackState> stackFrames = evt.getStackFrames();
    if (stackFrames != null && !stackFrames.isEmpty()) {
      fName = stackFrames.get(0).getName();
    } else {
      fName = ""; // weird
    }
    functionTxt.setText(fName);
    functionTxt.setCaretPosition(0);

    lwpTxt.setText(new AnInteger(event.getLWP()).toString());
    thrTxt.setText(new AnInteger(event.getThread()).toString());

    if (event.getCPU() == ExtendedEvent.NO_CPU_INFO) {
      cpuTxt.setText(AnLocale.getString("(unknown)"));
    } else {
      cpuTxt.setText(new AnInteger(event.getCPU()).toString());
    }

    timeTxt.setText(TimelineVariable.strTimestamp(event.getTimestamp()));

    if (rowDef == null) {
      typeTxt.setText("");
      expTxt.setText("");
    } else {
      typeTxt.setText(rowDef.getDataDescUName());
      String expName = rowDef.getExpName();
      expTxt.setText(expName);
    }

    previousSelectedEvent = event;
    return tbPanel;
  }

  private void setExtendedFields(final boolean value) {
    for (JTextField e : extendedFields) {
      e.setVisible(value);
    }

    for (JLabel jl : extendedLabels) {
      jl.setVisible(value);
    }

    anList.setAlignmentX();
    extendedShowing = value;
  }
}

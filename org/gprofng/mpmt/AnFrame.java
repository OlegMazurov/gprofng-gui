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

import org.gprofng.mpmt.guitesting.GUITesting;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnMenuBar;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;

public final class AnFrame extends JFrame {

  private AnWindow window;
  private Analyzer analyzer;

  // Constructor
  public AnFrame(final Analyzer analyzer) {
    super(Analyzer.getAnalyzerReleaseName());
    getAccessibleContext().setAccessibleDescription(Analyzer.getAnalyzerReleaseName());
    this.analyzer = analyzer;
  }

  /** Doesn't require running IPC. Called once. AWT thread */
  public void initComponents() {
    AnUtility.checkIfOnAWTThread(true);
    // Menu bar
    JMenuBar menu_bar = new AnMenuBar();
    menu_bar.setBorder(null);
    setJMenuBar(menu_bar);

    // Register the event handlers
    addWindowListener(new WindowHandler(this));

    // Adjust the window
    UserPref userPref = UserPref.getInstance();
    Dimension size;
    Point location;
    size = userPref.getFrameSize();
    location = userPref.getFrameLocation();
    if (!insideScreen(size, location)) {
      // Try move window
      location = new Point(0, 0);
    }
    if (!GUITesting.getInstance().isRunningUnderGUITesting()) {
      if (!insideScreen(size, location)) {
        // Try to resize
        location = userPref.getDefaultFrameLocation();
        size = userPref.getDefaultFrameSize();
      }
    }
    setSize(size);
    setLocation(location);

    // AnWindow
    window = new AnWindow(analyzer, this, getContentPane(), menu_bar);

    if (AnUtility.analyzer_icon != null) {
      setIconImage(AnUtility.analyzer_icon.getImage());
    }
  }

  private boolean insideScreen(Dimension size, Point location) {
    // Note: add 6 pixels beacuse of full screen mode goes a bit outside screen
    return (location.x + size.width <= AnVariable.SCREEN_SIZE.width + 6
        && location.y + size.height <= AnVariable.SCREEN_SIZE.height);
  }

  // Get AnWindow
  public AnWindow getWindow() {
    return window;
  }

  // Window handler
  private final class WindowHandler extends WindowAdapter {

    private AnFrame anFrame;

    public WindowHandler(AnFrame anFrame) {
      this.anFrame = anFrame;
    }

    @Override
    public void windowClosing(final WindowEvent event) {
      AnWindow.getInstance().exitAction();
    }
  }
}

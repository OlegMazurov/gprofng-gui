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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class KeyboardShortcuts {
  // Focus shortcuts

  public static final KeyStroke viewsFocusShortCut =
      KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK);
  public static final KeyStroke mainViewFocusShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK);
  public static final KeyStroke subviewArea2CallerFocusShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK);
  public static final KeyStroke subviewArea2CalleeFocusShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_DOWN_MASK);
  public static final KeyStroke subviewArea1FocusShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_DOWN_MASK);
  public static final KeyStroke subviewArea3FocusShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.CTRL_DOWN_MASK);
  public static final KeyStroke statusAreaShortCut =
      KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.CTRL_DOWN_MASK);
  // Action shortcuts
  public static final KeyStroke helpActionShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
  public static final KeyStroke contextMenuActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK);
  // Menu shortcuts
  public static final KeyStroke profileApplicationActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke profileRunningProcessActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke profileKernelActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke openExperimentActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke compareExperimentsActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke aggregateExperimentsActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke showErrorsWarningsActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke connectActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke exportActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke viewsSettingsActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke metricsSettingsShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke libraryVisibilityActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke functionColorsActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke settingsActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke exportSettingsActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke importSettingsActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  // Next/Previous View shortcuts
  public static final KeyStroke nextViewActionShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0);
  public static final KeyStroke previousViewActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK);
  // Functions/Source/Disassembler/Source Disassembler views
  public static final KeyStroke forwardActionShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
  public static final KeyStroke backwardActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK);
  // Source/Disassembler/Source Disassembler views
  public static final KeyStroke sourceNextHotLineActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0);
  public static final KeyStroke sourcePreviousHotLineActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.SHIFT_DOWN_MASK);
  public static final KeyStroke sourceNextNonZeroLineActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
  public static final KeyStroke sourcePreviousNonZeroLineActionShortcut =
      KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK);
}

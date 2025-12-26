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

package org.gprofng.mpmt.progress;

import javax.swing.SwingUtilities;

class InternalHandle {
  private ProgressPanel progressPanel;
  private CancelContext cancelContext;

  protected InternalHandle(ProgressPanel progressPanel, CancelContext cancelContext) {
    this.progressPanel = progressPanel;
    setCancelContext(cancelContext);
  }

  protected void progress(final String message, final int workunit) {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            progressPanel.progress(message, workunit);
          }
        });
  }

  protected void finish() {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            progressPanel.setVisible(false);
          }
        });
  }

  protected void toDeterminate(final int workunits, final int xxx) {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            progressPanel.toDeterminate(workunits, xxx);
          }
        });
  }

  protected void toIndeterminate() {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            progressPanel.toIndeterminate();
          }
        });
  }

  protected void start(String message, int workunits, long estimate) {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            progressPanel.setVisible(true);
          }
        });
  }

  protected void setCancelContext(CancelContext cancelContext) {
    this.cancelContext = cancelContext;
    progressPanel.setCancelButtonEnabled(cancelContext != null);
  }

  protected CancelContext getCancelContext() {
    return cancelContext;
  }
}

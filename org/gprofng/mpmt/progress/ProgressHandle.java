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


package org.gprofng.mpmt.progress;

/**
 * Instances provided by the ProgressHandleFactory allow the users of the API to notify the progress
 * bar UI about changes in the state of the running task. Progress component will be visualized only
 * after one of the start() methods.
 */
public final class ProgressHandle {
  private InternalHandle internal;

  /** Creates a new instance of ProgressHandle */
  ProgressHandle(InternalHandle internal) {
    this.internal = internal;
  }

  /**
   * start the progress indication for indeterminate task. it will be visualized by a progress bar
   * in indeterminate mode. <code>start</code> method can be called just once.
   */
  public void start() {
    start(0, -1);
  }

  /**
   * start the progress indication for a task with known number of steps. <code>start</code> method
   * can be called just once.
   *
   * @param workunits total number of workunits that will be processed
   */
  public void start(int workunits) {
    start(workunits, -1);
  }

  /**
   * start the progress indication for a task with known number of steps and known time estimate for
   * completing the task. <code>start</code> method can be called just once.
   *
   * @param workunits total number of workunits that will be processed
   * @param estimate estimated time to process the task in seconds
   */
  public void start(int workunits, long estimate) {
    internal.start("", workunits, estimate);
  }

  /**
   * Currently determinate task (with percentage or time estimate) can be switched to indeterminate
   * mode. This method has to be called after calling <code>start</code> method and before calling
   * <code>finish</code> method (the task has to be running).
   */
  public void switchToIndeterminate() {
    internal.toIndeterminate();
  }

  /**
   * Currently indeterminate task can be switched to show percentage completed. This method has to
   * be called after calling <code>start</code> method and before calling <code>finish</code> method
   * (the task has to be running). A common usecase is to calculate the amount of work in the
   * beginning showing in indeterminate mode and later switch to the progress with known steps
   *
   * @param workunits a definite number of complete units of work out of the total
   */
  public void switchToDeterminate(int workunits) {
    internal.toDeterminate(workunits, -1);
  }

  /**
   * Finish the task, remove the task's component from the progress bar UI. This method has to be
   * called after calling <code>start</code> method (the task has to be running).
   */
  public void finish() {
    internal.finish();
  }

  /**
   * Notify the user about completed workunits. This method has to be called after calling <code>
   * start</code> method and before calling <code>finish</code> method (the task has to be running).
   *
   * @param workunit a cumulative number of workunits completed so far
   */
  public void progress(int workunit) {
    progress(null, workunit);
  }

  /**
   * Notify the user about progress by showing message with details. This method has to be called
   * after calling <code>start</code> method and before calling <code>finish</code> method (the task
   * has to be running).
   *
   * @param message details about the status of the task
   */
  public void progress(String message) {
    progress(message, -1);
  }

  /**
   * Notify the user about completed workunits and show additional detailed message. This method has
   * to be called after calling <code>start</code> method and before calling <code>finish</code>
   * method (the task has to be running).
   *
   * @param message details about the status of the task
   * @param workunit a cumulative number of workunits completed so far
   */
  public void progress(String message, int workunit) {
    internal.progress(message, workunit);
  }

  public void setCancelContext(CancelContext cancelContext) {
    internal.setCancelContext(cancelContext);
  }

  public CancelContext getCancelContext() {
    return internal.getCancelContext();
  }
}

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

package org.gprofng.mpmt.ipc;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;

/** */
public class IPCContext {
  private static final int NON_DBE_CHANNEL = 0;
  private static final int DEF_DBE_CHANNEL = 1;
  private static final int UNIQUE_DBE_CHANNEL_START = 2;
  private static int currentUniqueDBEChannel = UNIQUE_DBE_CHANNEL_START;

  public enum Scope {
    SYSTEM,
    SESSION,
    NONE
  };

  public enum Request {
    SINGLE,
    GROUP
  };

  private static final IPCContext defaultDBEIPCContext =
      new IPCContext(null, Scope.NONE, false, false, Request.SINGLE, 1, null);
  private static final IPCContext defaultNONDBEIPCContext =
      new IPCContext(null, Scope.SYSTEM, false, false, Request.SINGLE, 0, null);

  private String taskName;
  private Scope scope;
  private boolean systemProgressBar;
  private boolean cancellable;
  private Request request;
  private int channel;
  private AnWindow anWindow;

  private int cancelChannel;
  private int cancelRequest;

  /**
   * Context for progress updates. Creates a context that has progress updates but task is not
   * cancellable.
   *
   * @param scope the scope of the command. WINDOW: progress update on anWindow only. SESSION:
   *     progress update on all windows.
   * @param anWindow the current anWindow (owner of progress status panel)
   */
  public IPCContext(Scope scope, AnWindow anWindow) {
    this(null, scope, true, false, Request.SINGLE, -1, anWindow);
  }

  /**
   * Context for progress updates and cancellable task. Creates a context that has progress updates
   * and task is cancellable.
   *
   * @param userActionName name of task used in cancel dialog
   * @param scope the scope of the command. WINDOW: progress update in current AnWindow only.
   *     SESSION: progress updates in all windows.
   * @param systemProgressBar set to true to enable system progress updates
   * @param cancellable set to true to make this task cancellable via the progress status
   * @param request whether to cancel a single request or a group of request (on a unique channel)
   * @param channel the channel to cancel in case of request==GROUP
   * @param anWindow the current anWindow (owner of progress status panel)
   */
  public IPCContext(
      String taskName,
      Scope scope,
      boolean systemProgressBar,
      boolean cancellable,
      Request request,
      int channel,
      AnWindow anWindow) {
    this.taskName = taskName;
    this.scope = scope;
    this.systemProgressBar = systemProgressBar;
    this.cancellable = cancellable;
    this.request = request;
    this.channel = channel;
    this.anWindow = anWindow;
  }

  /**
   * Context for a cancel request
   *
   * @param cancelChannel
   * @param cancelRequest
   */
  private IPCContext(int cancelChannel, int cancelRequest) {
    this.cancelChannel = cancelChannel;
    this.cancelRequest = cancelRequest;
  }

  public static IPCContext newDBEIPCContext(
      String userActionName,
      Scope scope,
      boolean systemProgressBar,
      boolean cancellable,
      Request request,
      AnWindow anWindow) {
    return new IPCContext(
        userActionName,
        scope,
        systemProgressBar,
        cancellable,
        request,
        request == Request.SINGLE ? DEF_DBE_CHANNEL : getUniqueDBEChannel(),
        anWindow);
  }

  public static IPCContext newNONDBEIPCContext(
      String userActionName, boolean systemProgressBar, boolean cancellable) {
    return new IPCContext(
        userActionName,
        Scope.SYSTEM,
        systemProgressBar,
        cancellable,
        Request.SINGLE,
        NON_DBE_CHANNEL,
        null);
  }

  public static IPCContext newCancelContext(int cancelChannel, int cancelRequest) {
    return new IPCContext(cancelChannel, cancelRequest);
  }

  public static IPCContext getDefaultDBEIPCContext() {
    return defaultDBEIPCContext;
  }

  public static IPCContext getDefaultNONDBEIPCContext() {
    return defaultNONDBEIPCContext;
  }

  /**
   * @return the taskName
   */
  public String getTaskName() {
    return taskName;
  }

  /**
   * @param taskName the taskName to set
   */
  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  /**
   * @return the scope
   */
  public Scope getScope() {
    return scope;
  }

  /**
   * @return the systemProgressBar
   */
  public boolean isSystemProgressBar() {
    return systemProgressBar;
  }

  /**
   * @return the cancellable
   */
  public boolean isCancellable() {
    return cancellable;
  }

  /**
   * @param cancellable the cancellable to set
   */
  public void setCancellable(boolean cancellable) {
    this.cancellable = cancellable;
  }

  /**
   * @return the request
   */
  public Request getRequest() {
    return request;
  }

  /**
   * @return the channel
   */
  public int getChannel() {
    return channel;
  }

  /**
   * @return the anWindow
   */
  public AnWindow getAnWindow() {
    return anWindow;
  }

  /**
   * @return the cancelChannel
   */
  public int getCancelChannel() {
    return cancelChannel;
  }

  /**
   * @return the cancelRequest
   */
  public int getCancelRequest() {
    return cancelRequest;
  }

  private static int getUniqueDBEChannel() {
    return currentUniqueDBEChannel++;
  }

  /*********/

  private static IPCContext currentContext = defaultDBEIPCContext;

  public static IPCContext newCurrentContext(
      String userActionName, Scope scope, boolean cancellable, AnWindow anWindow) {
    if (userActionName == null) {
      userActionName = AnLocale.getString("Unknown action....");
    }
    currentContext =
        newDBEIPCContext(
            userActionName,
            scope,
            true,
            cancellable,
            Request.GROUP,
            anWindow); // Default not cancellable
    //
    // System.out.print("\n--------------------------------------------------------newCurrentContext: " + currentContext.getChannel());
    return currentContext;
  }

  public static IPCContext getCurrentContext() {
    return currentContext;
  }
}

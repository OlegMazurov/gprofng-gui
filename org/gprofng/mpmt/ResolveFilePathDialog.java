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

import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.util.gui.AnDialog2;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.gui.ToolTipPopup;
import java.awt.Frame;
import javax.swing.JLabel;

public class ResolveFilePathDialog extends AnDialog2 {

  private ResolveFilePathPanel resolveFilePathPanel;
  private String unResolvedPath;
  private long obj_id;

  public ResolveFilePathDialog(Frame owner, String unResolvedPath, long obj_id) {
    super(owner, owner, AnLocale.getString("Resolve Source File"));
    this.unResolvedPath = unResolvedPath;
    this.obj_id = obj_id;
    setHelpTag(AnVariable.HELP_ResolveSourceFile);
    resolveFilePathPanel = new ResolveFilePathPanel(this, unResolvedPath);
    setCustomPanel(resolveFilePathPanel);
    //        getHelpButton().setVisible(false);
    //        getHelpButton().setEnabled(false);
  }

  @Override
  protected void setStatus(AnDialog2.Status status) {
    super.setStatus(status);
    if (status == AnDialog2.Status.OK) {
      String resolvedPath = resolveFilePathPanel.getResolvedPath();

      if (resolveFilePathPanel.archiveFiles()) {
        if (resolveFilePathPanel.resolveThisFileOnly()) {
          long[] ids = new long[1];
          String[] paths = new String[1];
          ids[0] = obj_id;
          paths[0] = resolvedPath;
          archive(ids, paths);
        } else {
          String unresolvedDir = AnUtility.dirname(unResolvedPath);
          String resolvedDir = AnUtility.dirname(resolvedPath);
          //                    System.out.println("unresolvedDir: " + unresolvedDir);
          //                    System.out.println("resolvedDir: " + resolvedDir);
          archiveAll(unresolvedDir, resolvedDir);
        }
      } else {
        //                if (resolveFilePathPanel.resolveThisFileOnly()) {
        //                    String[] oldPaths = new String[1];
        //                    String[] newPaths = new String[1];
        //                    oldPaths[0] = unResolvedPath;
        //                    newPaths[0] = resolvedPath;
        //                    setLocations(oldPaths, newPaths);
        //                } else {
        if (unResolvedPath.startsWith("/")) {
          // Use Path Map (native)
          int slash = 0;
          int unResolvedLength = unResolvedPath.length();
          int resolvedLength = resolvedPath.length();
          for (int i = 1; i < resolvedLength; i++) {
            if (resolvedPath.charAt(resolvedLength - i) == '/') {
              slash = i;
            }
            if (unResolvedPath.charAt(unResolvedLength - i)
                != resolvedPath.charAt(resolvedLength - i)) {
              break;
            }
          }
          String from = unResolvedPath.substring(0, unResolvedLength - slash);
          String to = resolvedPath.substring(0, resolvedLength - slash);
          addPathmap(from, to);
        } else {
          // Use Search Path (Java)
          String searchPath;
          if (resolvedPath.endsWith(unResolvedPath)) {
            searchPath =
                resolvedPath.substring(0, resolvedPath.length() - unResolvedPath.length() - 1);
          } else {
            searchPath = resolvedPath;
          }
          addSearchPath(searchPath);
        }
      }
      //            }
    }
  }

  private void addSearchPath(String searchPath) {
    //        System.out.println("Search Path: " + searchPath);
    AnWindow.getInstance().getSettings().getSearchPathSetting().add(this, searchPath);

    String ttText =
        "<html><b>"
            + searchPath
            + "</b> "
            + AnLocale.getString("added to Search Path settings<html>");
    JLabel label = new JLabel(ttText);
    ToolTipPopup toolTipPopup =
        new ToolTipPopup(
            AnWindow.getInstance().getViews().getCurrentViewDisplay(),
            label,
            ToolTipPopup.Location.CENTER,
            true);
    toolTipPopup.show(0, 4000);
  }

  private void addPathmap(final String from, final String to) {
    //        System.out.println("Pathmap: " + from + " -> " + to);
    AnWindow.getInstance().getSettings().getPathMapSetting().addPathMap(this, from, to);

    String ttText =
        "<html><b>"
            + from
            + "</b> -> <b>"
            + to
            + "</b> "
            + AnLocale.getString("added to Pathmap settings<html>");
    JLabel label = new JLabel(ttText);
    ToolTipPopup toolTipPopup =
        new ToolTipPopup(
            AnWindow.getInstance().getViews().getCurrentViewDisplay(),
            label,
            ToolTipPopup.Location.CENTER,
            true);
    toolTipPopup.show(0, 4000);
  }

  public void archiveAll(final String unresolvedDir, final String resolvedDir) {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            synchronized (AnVariable.mainFlowLock) {
              Object[] o = dbeResolvedWith_pathmap(unresolvedDir, resolvedDir); // IPC
              if (o == null) {
                return; // BUG?
              }
              String[] oldPaths = (String[]) o[0];
              String[] newPaths = (String[]) o[1];
              long[] ids = (long[]) o[2];
              archive(ids, newPaths);
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(this, AnChangeEvent.Type.SOURCE_FINDING_CHANGING));
              dbe_archive(ids, newPaths); // IPC
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(this, AnChangeEvent.Type.SOURCE_FINDING_CHANGED));
            }
          }
        },
        "Archive All thread");
  }

  public void archive(final long[] ids, final String[] paths) {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            synchronized (AnVariable.mainFlowLock) {
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(this, AnChangeEvent.Type.SOURCE_FINDING_CHANGING));
              dbe_archive(ids, paths); // IPC
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(this, AnChangeEvent.Type.SOURCE_FINDING_CHANGED));
            }
          }
        },
        "Archive thread");
  }

  public void setLocations(final String fnames[], final String locations[]) {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            synchronized (AnVariable.mainFlowLock) {
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(this, AnChangeEvent.Type.SOURCE_FINDING_CHANGING));
              dbeSetLocations(fnames, locations); // IPC
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(this, AnChangeEvent.Type.SOURCE_FINDING_CHANGED));
            }
          }
        },
        "set source locations thread");
  }

  protected void dbe_archive(final long ids[], final String locations[]) {
    AnWindow window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      window.IPC().send("dbe_archive");
      window.IPC().send(ids);
      window.IPC().send(locations);
      window.IPC().recvVoid();
    }
  }

  protected void dbeSetLocations(final String fnames[], final String locations[]) {
    AnWindow window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      window.IPC().send("dbeSetLocations");
      window.IPC().send(fnames);
      window.IPC().send(locations);
      window.IPC().recvVoid();
    }
  }

  protected Object[] dbeResolvedWith_setpath(final String path) {
    AnWindow window = AnWindow.getInstance();
    // obj[0] - list of old pathes
    // obj[1] - list of new pathes
    // obj[2] - list of ids
    synchronized (IPC.lock) {
      window.IPC().send("dbeResolvedWith_setpath");
      window.IPC().send(path);
      return (Object[]) window.IPC().recvObject();
    }
  }

  protected Object[] dbeResolvedWith_pathmap(final String old_prefix, final String new_prefix) {
    AnWindow window = AnWindow.getInstance();
    // obj[0] - list of old pathes
    // obj[1] - list of new pathes
    // obj[2] - list of ids
    synchronized (IPC.lock) {
      window.IPC().send("dbeResolvedWith_pathmap");
      window.IPC().send(old_prefix);
      window.IPC().send(new_prefix);
      return (Object[]) window.IPC().recvObject();
    }
  }
}

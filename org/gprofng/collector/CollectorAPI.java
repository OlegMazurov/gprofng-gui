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

package org.gprofng.collector;

/** This class provides access to the gprofng GUI collector API. */
public class CollectorAPI {

  private static boolean inited = false;

  static {
    init();
  }

  public static void init() {
    // Check if previously initialized
    if (inited) {
      return;
    }

    // Check if Java profiling is on
    if (!Boolean.getBoolean("collector.init")) {
      return;
    }

    try {
      System.loadLibrary("collector");
    } catch (UnsatisfiedLinkError e) {
      System.err.println("CollectorAPI: " + e);
      return;
    } catch (SecurityException e) {
      System.err.println("CollectorAPI: " + e);
      return;
    }
    inited = true;
  }

  private static native void _sample(String name);

  public static void sample(String name) {
    if (inited) {
      _sample(name);
    }
  }

  public static void sample() {
    sample(null);
  }

  private static native void _pause();

  public static void pause() {
    if (inited) {
      _pause();
    }
  }

  private static native void _resume();

  public static void resume() {
    if (inited) {
      _resume();
    }
  }

  private static native void _threadPause(Thread thread);

  public static void threadPause(Thread thread) {
    if (inited) {
      _threadPause(thread);
    }
  }

  public static void threadPause() {
    threadPause(null);
  }

  private static native void _threadResume(Thread thread);

  public static void threadResume(Thread thread) {
    if (inited) {
      _threadResume(thread);
    }
  }

  public static void threadResume() {
    threadResume(null);
  }

  private static native void _terminate();

  public static void terminate() {
    if (inited) {
      _terminate();
    }
  }
}

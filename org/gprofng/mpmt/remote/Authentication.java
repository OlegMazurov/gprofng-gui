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

package org.gprofng.mpmt.remote;

import org.gprofng.mpmt.persistence.UserPref;
import java.util.ArrayList;
import java.util.List;

public class Authentication {

  public enum Type {
    PASSWORD("Password", "password"),
    PUBLIC_KEY("Public key", "publickey"),
    KEYBOARD_INTERACTIVE("Keyboard-interactive", "keyboard-interactive");
    private String name;
    private String keyName;

    private Type(String name, String keyName) {
      this.name = name;
      this.keyName = keyName;
    }

    public String getName() {
      return name;
    }

    public String getKeyName() {
      return keyName;
    }

    protected static Type fromString(String s) {
      for (Type type : Type.values()) {
        if (type.toString().equals(s)) {
          return type;
        }
      }
      return null;
    }
  };

  private Type type;
  private boolean on;

  public Authentication(Type type, boolean on) {
    this.on = on;
    this.type = type;
  }

  public Authentication copy() {
    return new Authentication(type, on);
  }

  public Type getType() {
    return type;
  }

  public boolean isOn() {
    return on;
  }

  public void setOn(boolean on) {
    this.on = on;
  }

  /**
   * @return the default set of authentication methods
   */
  public static List<Authentication> getDefaultAuthentications() {
    List<Authentication> def = new ArrayList<Authentication>();
    def.add(new Authentication(Authentication.Type.PUBLIC_KEY, true));
    def.add(new Authentication(Authentication.Type.PASSWORD, true));
    //        def.add(new Authentication(Authentication.Type.KEYBOARD_INTERACTIVE, false)); // Don't
    // offer KEYBOARD_INTERACTIVE for now....
    return def;
  }

  /*
   * @return "Public key, Password"
   * Used in dialog. Uses 'user names' and shows only those that are on
   */
  public static String toString(List<Authentication> authentications) {
    StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (Authentication auth : authentications) {
      if (auth.isOn()) {
        if (first) {
          first = false;
        } else {
          buf.append(", ");
        }
        buf.append(auth.getType().getName());
      }
    }
    return buf.toString();
  }

  /**
   * @param authentications
   * @return "PUBLIC_KEY=true,PASSWORD=true,KEYBOARD_INTERACTIVE=false" Used in persistence
   */
  public static String toXMLString(List<Authentication> authentications) {
    StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (Authentication auth : authentications) {
      if (first) {
        first = false;
      } else {
        buf.append(",");
      }
      buf.append(auth.getType() + "=" + auth.isOn());
    }
    return buf.toString();
  }

  /**
   * @param xmlString
   * @return Parses output from toXMLString():
   *     "PUBLIC_KEY=true,PASSWORD=true,KEYBOARD_INTERACTIVE=false" Used in persistence.
   */
  public static List<Authentication> fromXMLString(String xmlString) {
    String[] auths = xmlString.split(",");
    List<Authentication> list = new ArrayList<Authentication>();
    for (String auth : auths) {
      String[] s = auth.split("=");
      Authentication.Type type = Type.fromString(s[0]);
      if (type
          == Authentication.Type
              .KEYBOARD_INTERACTIVE) { // Don't offer KEYBOARD_INTERACTIVE for now....
        continue;
      }
      if (s.length != 2) {
        // Abort
        return null;
      }
      boolean on = s[1].equals("true");
      if (type != null) {
        list.add(new Authentication(type, on));
      } else {
        // Abort, just return the default list...
        list = UserPref.getDefaultAuthentications();
      }
    }
    return list;
  }

  /**
   * @param authentications
   * @return "publickey,password", only the keys for methods selected. Used when connecting.
   */
  public static String toKeyString(List<Authentication> authentications) {
    StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (Authentication auth : authentications) {
      if (!auth.isOn()) {
        continue;
      }
      if (first) {
        first = false;
      } else {
        buf.append(",");
      }
      buf.append(auth.getType().getKeyName());
    }
    return buf.toString();
  }
}

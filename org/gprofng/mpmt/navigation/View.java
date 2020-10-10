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

package org.gprofng.mpmt.navigation;

import org.gprofng.mpmt.AnWindow;
import java.awt.Image;

public class View {

  public enum Type {
    STATIC_VIEW,
    VIEW,
    STANDARD,
    INDEX,
    MEMORY
  };

  private String name;
  private String displayName;
  private AnWindow anWindow;
  private String imagePath;
  private Image image = null;
  private Type type;
  private String shortDescr;
  private String longDescr;

  public View(
      String name,
      String displayName,
      String shortDescr,
      String longDescr,
      AnWindow anWindow,
      Type type,
      String imagePath) {
    this.name = name;
    this.displayName = displayName;
    this.anWindow = anWindow;
    this.imagePath = imagePath;
    this.type = type;
    this.shortDescr = shortDescr;
    this.longDescr = longDescr;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the type
   */
  public Type getType() {
    return type;
  }

  /**
   * @return the displayName
   */
  public String getDisplayName() {
    return displayName;
  }

  //    /**
  //     * @return the imagePath
  //     */
  //    public String getImagePath() {
  //        if (imagePath == null) {
  //            imagePath = generateIconPath();
  //        }
  //        return imagePath;
  //    }
  //    /**
  //     * @return the image
  //     */
  //    public Image getImage() {
  //        if (image == null) {
  //            URL url = this.getClass().getResource(getImagePath());
  //            if (url != null) {
  //                ImageIcon imageIcon = new ImageIcon(url);
  //                image = imageIcon.getImage();
  //            }
  //        }
  //        return image;
  //    }
  /**
   * @return the anWindow
   */
  public AnWindow getAnWindow() {
    return anWindow;
  }

  //    private static int iconNameIndex = 0;
  //    public static String generateIconPath() {
  //        if (iconNameIndex > 12) {
  //            iconNameIndex = 0;
  //        }
  //        return "/org/gprofng/mpmt/icons/" + "dummy" + iconNameIndex++ + ".png";
  //    }
  /**
   * @return the shortDescr
   */
  public String getShortDescr() {
    return shortDescr;
  }

  /**
   * @return the longDescr
   */
  public String getLongDescr() {
    if (longDescr == null) {
      return "<html><b>"
          + displayName
          + " "
          + "View"
          + " </b><br>"
          + displayName
          + " long"
          + " description......<br>..............................................<br>..............................................</html>";
    } else {
      return longDescr;
    }
  }
}

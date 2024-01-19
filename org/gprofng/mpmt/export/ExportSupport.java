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

package org.gprofng.mpmt.export;

import java.util.List;

public interface ExportSupport {

  public enum ExportFormat {
    TEXT("Text (.txt)"),
    HTML("HTML (.html)"),
    CSV("CSV (.csv)"),
    JPG("JPEG (.jpg)");
    private String name;

    private ExportFormat(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * @return list of supported export formats
   */
  public List<ExportFormat> getSupportedExportFormats();

  /**
   * Whether or not if this view supports limit
   *
   * @return true if limit is supported
   */
  public boolean exportLimitSupported();

  public String exportAsText(Integer limit, ExportSupport.ExportFormat format, Character delimiter);
}

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


package org.gprofng.mpmt.filter;

/**
 * @author tpreisle
 */
public class FilterClause {
  public enum Kind {
    STANDARD,
    CUSTOM,
    NO_FILTERS
  };

  private static int serialNumberCounter = 1;

  private String shortName;
  private String longName;
  private String clause;
  private boolean enabled;
  private Kind kind;
  private int serialNumber = 0;

  public FilterClause(String shortName, String longName, String clause, FilterClause.Kind kind) {
    this.shortName = shortName;
    this.longName = longName;
    this.clause = clause;
    this.enabled = true;
    this.kind = kind;
    if (kind != Kind.NO_FILTERS) {
      this.serialNumber = serialNumberCounter++;
    }
  }

  public static FilterClause getNoFiltersClause() {
    return new FilterClause(null, null, "1", Kind.NO_FILTERS);
  }

  /**
   * @return the kind
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * @return the displayName
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * @return the displayName
   */
  public String getShortNameWithSerialNumber() {
    return serialNumber + ": " + shortName;
  }

  public String getViewName() {
    String viewName = null;
    if (getShortName() != null) {
      int i = getShortName().indexOf(':');
      if (i > 0) {
        viewName = getShortName().substring(0, i);
      }
    }
    return viewName;
  }

  /**
   * @return the clause
   */
  public String getClause() {
    return clause;
  }

  public String getClause(int maxCharacters) {
    String shortClause = getClause();
    if (clause.length() > maxCharacters) {
      shortClause = shortClause.substring(0, maxCharacters) + "...";
    }
    return shortClause;
  }
  /**
   * @return the enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  public boolean isStandardFilter() {
    return getKind() == Kind.STANDARD;
  }

  public boolean isCustomFilter() {
    return getKind() == Kind.CUSTOM;
  }

  public boolean isNoFiltersFilter() {
    return getKind() == Kind.NO_FILTERS;
  }

  /**
   * @param enabled the enabled to set
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * @return the longName
   */
  public String getLongName() {
    return longName;
  }
}

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

package org.gprofng.mpmt.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * @author thp
 */
public class FilterParser {

  public enum Type {
    INT,
    DASH,
    COMMA
  };

  private String filter = null;
  private String filterId = null;

  public FilterParser(String filter, String filterId) {
    this.filter = filter;
    this.filterId = filterId;
  }

  public String go() {
    Stack<Elem> stack = new Stack<Elem>();
    Elem save = null;
    List<Elem> commas = new ArrayList<Elem>();

    if (filter == null) {
      return null;
    }

    if (filter.equals("all")) {
      return "";
    }

    StringTokenizer tokenizer = new StringTokenizer(filter, "-,", true);
    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken().trim();
      //            System.out.println("tok=" + tok);
      Elem elem;
      if (tok.equals("-")) {
        elem = new Elem(Type.DASH);
      } else if (tok.equals(",")) {
        elem = new Elem(Type.COMMA);
      } else {
        try {
          int val = new Integer(tok).intValue();
          elem = new Elem(val);
        } catch (NumberFormatException e) {
          return null;
        }
      }
      if (elem.getType() == Type.INT) {
        if (save != null) {
          stack.push(elem);
          stack.push(save);
          save = null;
        } else {
          stack.push(elem);
        }
      } else if (elem.getType() == Type.DASH) {
        if (save != null || stack.peek() == null) {
          return null; // Error
        }
        save = elem;
      } else if (elem.getType() == Type.COMMA) {
        if (save != null || stack.peek() == null) {
          return null; // Error
        }
        commas.add(elem);
        //                save = elem;
      }
    }
    for (Elem com : commas) {
      stack.push(com);
    }

    return generateFilter(stack);
  }

  private String generateFilter(Stack<Elem> stack) {
    if (stack.empty()) {
      return "";
    }
    Elem elem = stack.pop();
    if (elem.getType() == Type.INT) {
      return "(" + filterId + "==" + elem.getVal() + ")";
    } else if (elem.getType() == Type.DASH) {
      Elem elem1 = stack.pop();
      Elem elem2 = stack.pop();
      return "("
          + filterId
          + ">="
          + elem2.getVal()
          + " && "
          + filterId
          + "<="
          + elem1.getVal()
          + ")";
    } else if (elem.getType() == Type.COMMA) {
      String filter1 = generateFilter(stack);
      String filter2 = generateFilter(stack);
      return "(" + filter2 + " || " + filter1 + ")";
    } else {
      assert false;
    }
    return null; // Error
  }

  class Elem {

    private Type type;
    private int val;

    public Elem(int val) {
      this.type = Type.INT;
      this.val = val;
    }

    public Elem(Type type) {
      this.type = type;
      this.val = 0;
    }

    /**
     * @return the type
     */
    public Type getType() {
      return type;
    }

    /**
     * @return the val
     */
    public int getVal() {
      return val;
    }
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    String in;
    String out;

    test("34-38", "Samples");
    test("12,23", "Samples");
    test("1,2,3", "Samples");
    test("1,2-3", "Samples");
    test("34-38, 12,  23", "Samples");
  }

  private static void test(String filter, String filterId) {
    String res = new FilterParser(filter, filterId).go();
    System.out.println(filter + "========>" + res);
    System.out.println();
  }
}

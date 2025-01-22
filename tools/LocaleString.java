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

// stdin:	source files
// stdout:	properties file
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Vector;

public final class LocaleString {

    private final static MessageFormat mf1 = new MessageFormat("({0}){1}"); // NOI18N
    private final static MessageFormat mf2 = new MessageFormat("{0}){1}"); // NOI18N
    private final static MessageFormat mf3 = new MessageFormat("\''{0}\'',{1}"); // NOI18N
    private final static MessageFormat mf4 = new MessageFormat("\"{0}\",{1}"); // NOI18N
    private final static ParsePosition parsePos = new ParsePosition(0);
    private final static String key = "AnLocale.getString"; // NOI18N
    private final static int key_len = key.length();
    private final static String key1 = "getLocaleStr"; // NOI18N
    private final static int key1_len = key1.length();

    public static void main(String[] args) {
        final BufferedReader in;
        String line;
        Vector values, mnem_values;
        int index, index1;
        Elem strs;

        // Open stdin as BufferedReader
        in = new BufferedReader(new InputStreamReader(System.in));
        line = null;
        values = new Vector();
        mnem_values = new Vector();

        try {
            for (;;) {
                if ((line == null) && ((line = in.readLine()) == null)) {
                    break;
                }

                // Search keyword, the Locale get string method
                index = line.indexOf(key);
                index1 = line.indexOf(key1);
                if ((line.indexOf("getLocaleStr(String") > 0) || // NOI18N
                        (line.indexOf("AnLocale.getString(str") > 0) || // NOI18N
                        (line.indexOf("getLocaleStr(char") > 0) || // NOI18N
                        (line.indexOf("AnLocale.getString(c") > 0)) {   // NOI18N
                    line = null;
                    continue;
                }

                if ((index == -1) && (index1 == -1)) {
                    line = null;
                    continue;
                }
                // Parse for the string argument
                for (;;) {
                    if (index != -1) {
                        strs = parse(line.substring(index + key_len).trim());
                    } else {
                        strs = parse(line.substring(index1 + key1_len).trim());
                    }
                    if (strs != null) {
                        if (strs.isMnemonic()) {
                            mnem_values.addElement(strs);
                        } else {
                            values.addElement(strs);
                        }
                        line = strs.getLeftStr();
                        break;
                    }

                    line += in.readLine();
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        // Sort & Output result properties
        values = sort(values);
        mnem_values = sort(mnem_values);

        Elem elem;
        for (index = 0; index < values.size(); index++) {
            elem = (Elem) values.get(index);
            if (elem.getKey() != null) {
                System.out.println(setEscape(elem.getValue()) + "[" + elem.getKey() + "]" + "=" + // NOI18N
                        (elem.getValue().startsWith(" ") ? "\\" : "") + elem.getValue()); // NOI18N
            } else {
                System.out.println(setEscape(elem.getValue()) + "=" + // NOI18N
                        (elem.getValue().startsWith(" ") ? "\\" : "") + elem.getValue()); // NOI18N
            }
        }

        if (mnem_values.size() > 0) {
            System.out.println("\n# MNEMONICS"); // NOI18N
            System.out.println("# =================================================================================="); // NOI18N
            System.out.println("#"); // NOI18N

            for (index = 0; index < mnem_values.size(); index++) {
                elem = (Elem) mnem_values.get(index);
                System.out.println(elem.getKey() + "=" + // NOI18N
                        (elem.getValue().startsWith(" ") ? "\\" : "") + elem.getValue()); // NOI18N
            }
        }

    }
    private final static Object syncFormat = new Object();

    // Parser for getting the string argument
    private static Elem parse(String str) {
        MessageFormat mf;
        String ans, value, check;
        Object[] result;
        Elem elem;

        mf = mf1;
        ans = ""; // NOI18N

        for (;;) {
            parsePos.setIndex(0);
            result = mf.parse(str, parsePos);
            mf = mf2;
            if (result != null) {
                value = (String) result[0];
                str = (String) result[1];
                ans += value;
                check = value.trim();
                // If ends with ", then done
                if (check.endsWith("\"") && !check.endsWith("\\\"")) { // NOI18N
                    value = ans.trim();
                    parsePos.setIndex(0);
                    synchronized (syncFormat) { // begin critical section: format is not thread safe
                        result = mf3.parse(value, parsePos);
                        String key = null;
                        boolean is_mnemonic = false;
                        if (result != null) {
                            value = ((String) result[0]).trim();
                            key = ((String) result[1]).trim();
                            is_mnemonic = true;
                        } else {
                            result = mf4.parse(value, parsePos);
                            if (result != null) {
                                value = ((String) result[0]).trim();
                                key = ((String) result[1]).trim();
                            }
                        }
                        elem = new Elem(key, value, str, is_mnemonic);
                    } // end of critical section
                    return elem.removeQuotes();
                } else {
                    ans += ")"; // NOI18N
                }
            } else {
                return null;
            }
        }
    }

    // Sort the string list
    private static Vector sort(final Vector vec) {
        final int size;
        int index;
        final Vector result;

        // Convert results to Object[] & Sort
        size = vec.size();
        final Object[] strs = vec.toArray();
        Arrays.sort(strs);

        // Remove Repeated elements
        Elem elem = null;
        result = new Vector();

        for (index = 0; index < size; index++) {
            if (((Elem) strs[index]).equals(elem)) {
                continue;
            }
            elem = (Elem) strs[index];
            // Remove the "" from the both ends
            result.addElement(new Elem(elem));
        }
        return result;
    }

    // Add escape char '\' for ' ' & ':'
    private static String setEscape(final String str) {
        String esc;
        final int size;
        int index;
        char c;

        esc = ""; // NOI18N
        size = str.length();
        for (index = 0; index < size; index++) {
            c = str.charAt(index);
            if ((c == ' ') || (c == ':') || (c == '=')) // NOI18N
            {
                esc += '\\'; // NOI18N
            }
            esc += c;
        }

        return esc;
    }

    /* Class which contains a parsed properties value */
    private static final class Elem implements Comparable {

        private String key;   // optional bundle key
        private String value; // required properties value
        private String left;  // residual string
        private boolean is_mnemonic;  // element is mnemonic

        // Constructor
        public Elem(final String key, final String value, final String left, final boolean is_mnemonic) {
            this.key = key;
            this.value = value;
            this.left = left;
            this.is_mnemonic = is_mnemonic;
        }

        // Copy constructor
        public Elem(final Elem elem) {
            this.key = elem.key;
            this.value = elem.value;
            this.is_mnemonic = elem.is_mnemonic;
        }

        public String getLeftStr() {
            return left;
        }

        public String getKey() {
            return key;
        }

        public boolean isMnemonic() {
            return is_mnemonic;
        }

        public String getValue() {
            return value;
        }

        // Comparator implementation method
        public int compareTo(final Object obj) {
            final String obj_key = ((Elem) obj).key;
            final String obj_value = ((Elem) obj).value;
            final boolean obj_mnem = ((Elem) obj).is_mnemonic;

            if (key != null && obj_key != null) {
                if (is_mnemonic && obj_mnem) {
                    return key.compareTo(obj_key);
                } else {
                    return (value + key).compareTo(obj_value + obj_key);
                }
            } else {
                return value.compareTo(obj_value);
            }
        }

        public String toString() {
            return (key == null ? value : key) + "=" + value; // NOI18N
        }

        protected Elem removeQuotes() {
            if (key != null) {
                if (key.charAt(0) == '\"' || key.charAt(0) == '\'') { // NOI18N
                    key = key.substring(1, key.length() - 1);
                }
            }
            if (value != null) {
                if (value.charAt(0) == '\"' || value.charAt(0) == '\'') { // NOI18N
                    value = value.substring(1, value.length() - 1);
                }
            }
            return this;
        }

        public boolean equals(final Elem e) {
            if (e != null) {
                if (!this.is_mnemonic && !e.is_mnemonic) {
                    if (this.key != null && e.key != null) {
                        return this.compareTo(e) == 0;
                    }
                    if (this.key != null || e.key != null) {
                        return false;
                    }
                }
                return this.compareTo(e) == 0;
            }
            return false;
        }
    }
}

/* Copyright (C) 2026 Free Software Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses>. */

package org.mazurov.jdbe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application-specific metrics sampled periodically from the target process, read from a CSV
 * file dropped into the experiment directory and cropped to the experiment's time window.
 *
 * <p>CSV layout:
 * <pre>
 * filename:,&lt;path&gt;,                  first line, identifies the format
 * &lt;name&gt;:,&lt;description&gt;,           one line per metric
 * (blank line)
 * ,,&lt;group&gt;,&lt;group&gt;,...            group of each column
 * ,,&lt;name&gt;,&lt;name&gt;,...              column names; one column is named "time"
 * ,,&lt;value&gt;,&lt;value&gt;,...            data rows; "time" holds "yyyy-MM-dd HH:mm:ss UTC"
 * </pre>
 *
 * <p>Column types are not declared and are inferred from the data. Values are kept as doubles
 * (exact for integers up to 2^53); missing values are NaN. Columns without any value inside
 * the window are dropped.
 */
public final class AppMetrics {

    public enum Type {
        INT, DOUBLE, BOOL
    }

    /** Name of the column holding the sample time. */
    public static final String TIME_COLUMN = "time";

    /**
     * Stand-in for NaN on the wire: IPC sends doubles as locale-formatted text, which cannot
     * carry NaN. A real metric never has this value.
     */
    public static final double MISSING_WIRE = -1.0e18;

    /** Joins the metric names of a stacked chart in a chart property name. */
    public static final char STACK_SEPARATOR = '\u001f';

    private static final String HEADER_PREFIX = "filename:,";
    private static final long NANOSEC = 1_000_000_000L;

    private static final int SAW_INT = 1;
    private static final int SAW_DOUBLE = 2;
    private static final int SAW_BOOL = 4;

    private final String[] names;
    private final String[] descriptions;
    private final String[] groups;
    private final Type[] types;
    private final double[][] columns; // [metric][sample]
    private final long[] timesNs;     // sample times, relative to experiment start
    private final Map<String, Integer> indexByName = new HashMap<>();

    AppMetrics(String[] names, String[] descriptions, String[] groups, Type[] types,
               double[][] columns, long[] timesNs) {
        this.names = names;
        this.descriptions = descriptions;
        this.groups = groups;
        this.types = types;
        this.columns = columns;
        this.timesNs = timesNs;
        for (int i = 0; i < names.length; i++) {
            indexByName.put(names[i], i);
        }
    }

    public int metricCount() {
        return names.length;
    }

    public int sampleCount() {
        return timesNs.length;
    }

    public String name(int metric) {
        return names[metric];
    }

    public String description(int metric) {
        return descriptions[metric];
    }

    public String group(int metric) {
        return groups[metric];
    }

    public Type type(int metric) {
        return types[metric];
    }

    /** Index of the metric with this name, or -1. */
    public int indexOf(String name) {
        Integer i = indexByName.get(name);
        return i == null ? -1 : i;
    }

    /** Sample values (NaN = missing), one per sample; the array is shared, do not modify. */
    public double[] values(int metric) {
        return columns[metric];
    }

    /** Sample times in ns relative to experiment start; the array is shared, do not modify. */
    public long[] times() {
        return timesNs;
    }

    public double min(int metric) {
        double m = Double.NaN;
        for (double v : columns[metric]) {
            if (!Double.isNaN(v) && (Double.isNaN(m) || v < m)) {
                m = v;
            }
        }
        return m;
    }

    public double max(int metric) {
        double m = Double.NaN;
        for (double v : columns[metric]) {
            if (!Double.isNaN(v) && (Double.isNaN(m) || v > m)) {
                m = v;
            }
        }
        return m;
    }

    /**
     * Places the samples of a metric in the bins [start + i*delta, start + (i+1)*delta): a bin
     * holds the mean of the samples whose timestamp falls in it and is NaN if there are none.
     * Samples are therefore individual points in time: with bins shorter than the sampling
     * period there are gaps between them, with longer bins every bin has a value.
     *
     * @param start bin origin in ns relative to experiment start
     */
    public double[] binSamples(int metric, long start, long delta, int nbins) {
        return binSamples(metric, start, delta, nbins, null);
    }

    /**
     * As {@link #binSamples(int, long, long, int)} but only for the given samples (indices into
     * {@link #times()}), which is how the view's filters are honored.
     *
     * @param keep sample indices to use, or null for all
     */
    public double[] binSamples(int metric, long start, long delta, int nbins, int[] keep) {
        double[] out = new double[nbins];
        if (delta <= 0) {
            Arrays.fill(out, Double.NaN);
            return out;
        }
        int[] count = new int[nbins];
        double[] v = columns[metric];
        final int n = keep == null ? timesNs.length : keep.length;
        for (int k = 0; k < n; k++) {
            final int i = keep == null ? k : keep[k];
            if (Double.isNaN(v[i])) {
                continue;
            }
            long bin = Math.floorDiv(timesNs[i] - start, delta);
            if (bin >= 0 && bin < nbins) {
                out[(int) bin] += v[i];
                count[(int) bin]++;
            }
        }
        for (int b = 0; b < nbins; b++) {
            out[b] = count[b] == 0 ? Double.NaN : out[b] / count[b];
        }
        return out;
    }

    /**
     * Loads the first {@code *.csv} file in an experiment directory that is a metrics file.
     *
     * @return the metrics, or null if there is none or it has no samples in the window
     */
    public static AppMetrics findAndLoad(File experimentDir, long wallStartSec, long durationNs) {
        File[] files = experimentDir.listFiles((d, n) -> n.endsWith(".csv"));
        if (files == null) {
            return null;
        }
        Arrays.sort(files);
        for (File f : files) {
            try {
                AppMetrics m = load(f, wallStartSec, durationNs);
                if (m != null) {
                    return m;
                }
            } catch (IOException | RuntimeException e) {
                // not a usable metrics file, try the next one
            }
        }
        return null;
    }

    /**
     * Reads a metrics CSV, keeping only samples inside the experiment window.
     *
     * @param wallStartSec experiment start, UTC epoch seconds
     * @param durationNs   experiment duration
     * @return the metrics, or null if this is not a metrics file or no sample is in the window
     */
    public static AppMetrics load(File file, long wallStartSec, long durationNs) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line = r.readLine();
            if (line == null || !line.startsWith(HEADER_PREFIX)) {
                return null;
            }
            Map<String, String> descriptions = new HashMap<>();
            while ((line = r.readLine()) != null && !line.isEmpty()) {
                int i = line.indexOf(":,");
                if (i <= 0) {
                    return null;
                }
                String d = line.substring(i + 2);
                if (d.endsWith(",")) {
                    d = d.substring(0, d.length() - 1);
                }
                descriptions.put(line.substring(0, i), d);
            }
            String groupLine = r.readLine();
            String nameLine = r.readLine();
            if (line == null || groupLine == null || nameLine == null) {
                return null;
            }
            String[] groupRow = split(groupLine);
            String[] nameRow = split(nameLine);
            int timeCol = Arrays.asList(nameRow).indexOf(TIME_COLUMN);
            if (timeCol < 0) {
                return null;
            }
            int ncols = nameRow.length;

            long endSec = wallStartSec + (durationNs + NANOSEC - 1) / NANOSEC;
            List<double[]> rows = new ArrayList<>();
            long[] times = new long[64];
            int[] seen = new int[ncols];
            while ((line = r.readLine()) != null) {
                long sec = parseTime(cellAt(line, timeCol));
                if (sec == Long.MIN_VALUE || sec < wallStartSec) {
                    continue;
                }
                if (sec > endSec) {
                    break; // rows are in time order
                }
                String[] cells = split(line);
                double[] row = new double[ncols];
                for (int c = 0; c < ncols; c++) {
                    row[c] = c == timeCol || c >= cells.length ? Double.NaN : parseCell(cells[c], c, seen);
                }
                if (rows.size() == times.length) {
                    times = Arrays.copyOf(times, times.length * 2);
                }
                times[rows.size()] = (sec - wallStartSec) * NANOSEC;
                rows.add(row);
            }
            if (rows.isEmpty()) {
                return null;
            }

            List<Integer> kept = new ArrayList<>();
            for (int c = 0; c < ncols; c++) {
                if (c != timeCol && !nameRow[c].isEmpty() && seen[c] != 0) {
                    kept.add(c);
                }
            }
            int nm = kept.size();
            String[] names = new String[nm];
            String[] descs = new String[nm];
            String[] groups = new String[nm];
            Type[] types = new Type[nm];
            double[][] columns = new double[nm][rows.size()];
            for (int m = 0; m < nm; m++) {
                int c = kept.get(m);
                names[m] = nameRow[c];
                descs[m] = descriptions.getOrDefault(nameRow[c], "");
                groups[m] = c < groupRow.length ? groupRow[c] : "";
                types[m] = typeOf(seen[c]);
                for (int s = 0; s < rows.size(); s++) {
                    columns[m][s] = rows.get(s)[c];
                }
            }
            return new AppMetrics(names, descs, groups, types, columns, Arrays.copyOf(times, rows.size()));
        }
    }

    private static Type typeOf(int seen) {
        if ((seen & SAW_DOUBLE) != 0) {
            return Type.DOUBLE;
        }
        if ((seen & SAW_INT) != 0) {
            return Type.INT;
        }
        return Type.BOOL;
    }

    private static double parseCell(String s, int col, int[] seen) {
        if (s.isEmpty()) {
            return Double.NaN;
        }
        if (s.equals("true")) {
            seen[col] |= SAW_BOOL;
            return 1;
        }
        if (s.equals("false")) {
            seen[col] |= SAW_BOOL;
            return 0;
        }
        try {
            if (s.indexOf('.') < 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
                long l = Long.parseLong(s);
                if (l == Long.MIN_VALUE) {
                    return Double.NaN; // "not available" marker
                }
                seen[col] |= SAW_INT;
                return l;
            }
            double d = Double.parseDouble(s);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return Double.NaN;
            }
            seen[col] |= SAW_DOUBLE;
            return d;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Parses "yyyy-MM-dd HH:mm:ss UTC" to epoch seconds; Long.MIN_VALUE if malformed. */
    static long parseTime(String s) {
        if (s == null || s.length() < 19) {
            return Long.MIN_VALUE;
        }
        try {
            return LocalDateTime.of(
                    Integer.parseInt(s, 0, 4, 10), Integer.parseInt(s, 5, 7, 10), Integer.parseInt(s, 8, 10, 10),
                    Integer.parseInt(s, 11, 13, 10), Integer.parseInt(s, 14, 16, 10), Integer.parseInt(s, 17, 19, 10))
                    .toEpochSecond(ZoneOffset.UTC);
        } catch (RuntimeException e) {
            return Long.MIN_VALUE;
        }
    }

    /** The n-th comma separated cell of a line, or null; cheaper than split() for skipped rows. */
    private static String cellAt(String line, int n) {
        int from = 0;
        for (int i = 0; i < n; i++) {
            from = line.indexOf(',', from);
            if (from < 0) {
                return null;
            }
            from++;
        }
        int to = line.indexOf(',', from);
        return to < 0 ? line.substring(from) : line.substring(from, to);
    }

    /** Splits at every comma, keeping empty cells (including a trailing one). */
    static String[] split(String line) {
        List<String> cells = new ArrayList<>();
        int from = 0;
        while (true) {
            int to = line.indexOf(',', from);
            if (to < 0) {
                cells.add(line.substring(from));
                break;
            }
            cells.add(line.substring(from, to));
            from = to + 1;
        }
        return cells.toArray(new String[0]);
    }
}

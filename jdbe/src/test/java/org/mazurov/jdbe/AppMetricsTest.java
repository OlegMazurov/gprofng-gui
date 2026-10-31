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

package org.mazurov.jdbe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppMetricsTest {

    private static final long NS = 1_000_000_000L;
    // 2026-10-30 23:38:19 UTC
    private static final long START = 1793403499L;

    /** Metrics: a (int), b (double), flag (bool), none (always empty), gone (empty in window), plus time. */
    private static final String HEADER = String.join("\n",
            "filename:,data/stats/Test.csv,",
            "a:,counter a,",
            "b:,gauge b,",
            "flag:,a boolean,",
            "none:,never set,",
            "gone:,only set outside window,",
            "time:,sample time,",
            "",
            ",,g1,g1,g2,g2,g2,info",
            ",,a,b,flag,none,gone,time",
            "");

    private static String row(String time, String a, String b, String flag, String gone) {
        return ",," + a + "," + b + "," + flag + ",," + gone + "," + time + "\n";
    }

    private static final String T_BEFORE = "2026-10-30 23:38:16 UTC";
    private static final String T0 = "2026-10-30 23:38:19 UTC";
    private static final String T1 = "2026-10-30 23:38:22 UTC";
    private static final String T2 = "2026-10-30 23:38:25 UTC";
    private static final String T_END = "2026-10-30 23:38:29 UTC"; // 1 s past the 9.5 s window, rounded up
    private static final String T_AFTER = "2026-10-30 23:38:32 UTC";

    private Path write(Path dir, String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, content);
        return p;
    }

    private String csv() {
        return HEADER
                + row(T_BEFORE, "1", "0.5", "false", "7")
                + row(T0, "10", "1.5", "false", "")
                + row(T1, "-9223372036854775808", "2.5", "true", "")
                + row(T2, "30", "", "true", "")
                + row(T_END, "40", "4.5", "true", "")
                + row(T_AFTER, "50", "5.5", "true", "9");
    }

    @Test
    void cropsToExperimentWindow(@TempDir Path dir) throws IOException {
        AppMetrics m = AppMetrics.load(write(dir, "m.csv", csv()).toFile(), START, 9_500_000_000L);
        assertNotNull(m);
        // window is [START, START + 10 s]: T0, T1, T2 and T_END
        assertArrayEquals(new long[] {0, 3 * NS, 6 * NS, 10 * NS}, m.times());
    }

    @Test
    void dictionaryAndTypes(@TempDir Path dir) throws IOException {
        AppMetrics m = AppMetrics.load(write(dir, "m.csv", csv()).toFile(), START, 9_500_000_000L);
        assertEquals(3, m.metricCount()); // time, none and gone are not metrics
        assertEquals(0, m.indexOf("a"));
        assertEquals(-1, m.indexOf("time"));
        assertEquals(-1, m.indexOf("none"));
        assertEquals(-1, m.indexOf("gone"));
        assertEquals("counter a", m.description(m.indexOf("a")));
        assertEquals("g2", m.group(m.indexOf("flag")));
        assertEquals(AppMetrics.Type.INT, m.type(m.indexOf("a")));
        assertEquals(AppMetrics.Type.DOUBLE, m.type(m.indexOf("b")));
        assertEquals(AppMetrics.Type.BOOL, m.type(m.indexOf("flag")));
    }

    @Test
    void valuesMissingAndBooleans(@TempDir Path dir) throws IOException {
        AppMetrics m = AppMetrics.load(write(dir, "m.csv", csv()).toFile(), START, 9_500_000_000L);
        double[] a = m.values(m.indexOf("a"));
        assertEquals(10, a[0]);
        assertTrue(Double.isNaN(a[1]), "Long.MIN_VALUE is the 'not available' marker");
        assertEquals(30, a[2]);
        double[] b = m.values(m.indexOf("b"));
        assertEquals(1.5, b[0]);
        assertTrue(Double.isNaN(b[2]), "empty cell");
        assertArrayEquals(new double[] {0, 1, 1, 1}, m.values(m.indexOf("flag")));
        assertEquals(10, m.min(m.indexOf("a")));
        assertEquals(40, m.max(m.indexOf("a")));
    }

    @Test
    void noSamplesInWindow(@TempDir Path dir) throws IOException {
        assertNull(AppMetrics.load(write(dir, "m.csv", csv()).toFile(), START + 1000, NS));
    }

    @Test
    void notAMetricsFile(@TempDir Path dir) throws IOException {
        assertNull(AppMetrics.load(write(dir, "x.csv", "a,b,c\n1,2,3\n").toFile(), START, NS));
        assertNull(AppMetrics.load(write(dir, "empty.csv", "").toFile(), START, NS));
    }

    @Test
    void findAndLoadSkipsOtherCsvFiles(@TempDir Path dir) throws IOException {
        write(dir, "a-other.csv", "a,b,c\n1,2,3\n");
        write(dir, "z-metrics.csv", csv());
        write(dir, "readme.txt", "ignored");
        AppMetrics m = AppMetrics.findAndLoad(dir.toFile(), START, 9_500_000_000L);
        assertNotNull(m);
        assertEquals(3, m.metricCount());
        assertNull(AppMetrics.findAndLoad(new File(dir.toFile(), "missing"), START, NS));
    }

    private AppMetrics samples() {
        // samples at 0, 3 and 6 s with values 10, 20, NaN
        return new AppMetrics(new String[] {"m"}, new String[] {""}, new String[] {""},
                new AppMetrics.Type[] {AppMetrics.Type.DOUBLE},
                new double[][] {{10, 20, Double.NaN}}, new long[] {0, 3 * NS, 6 * NS});
    }

    @Test
    void binsShorterThanThePeriodLeaveGaps() {
        // 1 s bins from -1 s: each sample sits alone in its bin, the others are empty
        double[] v = samples().binSamples(0, -NS, NS, 9);
        assertTrue(Double.isNaN(v[0]));
        assertEquals(10, v[1]);
        assertTrue(Double.isNaN(v[2]) && Double.isNaN(v[3]));
        assertEquals(20, v[4]);
        assertTrue(Double.isNaN(v[5]) && Double.isNaN(v[6]));
        assertTrue(Double.isNaN(v[7]), "the 6 s sample is NaN, so its bin is empty");
        assertTrue(Double.isNaN(v[8]));
    }

    @Test
    void binsAsLongAsThePeriodHaveNoGaps() {
        double[] v = samples().binSamples(0, 0, 3 * NS, 2);
        assertArrayEquals(new double[] {10, 20}, v, 1e-9);
    }

    @Test
    void severalSamplesInABinAreAveraged() {
        assertEquals(15, samples().binSamples(0, 0, 6 * NS, 1)[0], 1e-9); // 10 and 20, NaN skipped
    }

    @Test
    void binBoundariesAreHalfOpen() {
        double[] v = samples().binSamples(0, 0, 3 * NS, 3);
        assertEquals(10, v[0]); // [0,3) holds the sample at 0 but not the one at 3 s
        assertEquals(20, v[1]); // [3,6) holds the one at 3 s
    }

    @Test
    void parsesTime() {
        assertEquals(START, AppMetrics.parseTime("2026-10-30 23:38:19 UTC"));
        assertEquals(Long.MIN_VALUE, AppMetrics.parseTime("garbage"));
        assertEquals(Long.MIN_VALUE, AppMetrics.parseTime(null));
    }
}

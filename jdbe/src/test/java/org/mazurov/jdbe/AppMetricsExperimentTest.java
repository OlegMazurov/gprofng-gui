package org.mazurov.jdbe;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Engine-level check of the application metrics against experiments/hashdex.er (skipped when
 * that experiment, which is test data and not in the repository history, is absent).
 */
@EnabledIf("hasExperiment")
class AppMetricsExperimentTest {

    private static final File EXPERIMENT = new File("../experiments/hashdex.er").getAbsoluteFile();
    private static final long NS = 1_000_000_000L;
    private static final int APPMETRICS = Enums.ProfData_type.DATA_APPMETRICS.getValue();
    private static final int PROP_EXPID = Enums.Prop_type.PROP_EXPID.ordinal();
    private static final int PROP_NONE = Enums.Prop_type.PROP_NONE.ordinal();

    static boolean hasExperiment() {
        return new File(EXPERIMENT, "log.xml").isFile();
    }

    @BeforeAll
    static void open() {
        new ERPrint(new String[] {"-IPC"});
        DbeApplication.getInstance().initApplication("", "", (percentage, text) -> { });
        DBE.dbeInitView(0, -1);
        assertNull(DBE.dbeSetExperimentsGroups(new String[][] {{EXPERIMENT.getPath()}}));
    }

    @AfterAll
    static void close() {
        DbeApplication.shutdown();
        DbeSession.shutdown();
    }

    private static Experiment exp() {
        return DbeSession.getInstance().get_exp(0);
    }

    @Test
    void croppedToTheExperiment() {
        AppMetrics m = exp().getAppMetrics();
        assertNotNull(m);
        assertEquals(1793403499L, exp().getWallStartSec());
        // CSV rows are 3 s apart from 23:37:34; the experiment runs 23:38:19 .. 23:42:52
        assertEquals(92, m.sampleCount());
        assertEquals(0, m.times()[0]);
        long duration = exp().getLastEvent() - exp().getStartTime();
        assertTrue(m.times()[m.sampleCount() - 1] <= duration + NS);
        assertTrue(m.metricCount() > 1000);
        assertEquals(-1, m.indexOf("time"));
    }

    @Test
    void dataKindIsAdvertised() {
        Object[] info = DBE.dbeGetDataDescriptorsV2(0);
        assertNotNull(info);
        int[] ids = (int[]) info[0];
        assertTrue(Arrays.stream(ids).anyMatch(id -> id == APPMETRICS), Arrays.toString(ids));
        assertEquals("PROFDATA_TYPE_APPMETRICS", ((String[]) info[1])[indexOf(ids, APPMETRICS)]);
    }

    private static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == v)
                return i;
        }
        return -1;
    }

    @Test
    void hasTimelineData() {
        boolean[] has = DBE.dbeHasTLData(0, new int[] {0}, new int[] {APPMETRICS},
                new int[] {PROP_EXPID}, new int[] {0}, new int[] {0});
        assertTrue(has[0]);
    }

    @Test
    void sameAnswersForTheEntityTheGuiUses() {
        // the GUI asks for rows without an entity (PROP_NONE), like heap size and GC events
        assertTrue(DBE.dbeHasTLData(0, new int[] {0}, new int[] {APPMETRICS},
                new int[] {PROP_NONE}, new int[] {0}, new int[] {0})[0]);
        long start = exp().getStartTime();
        String name = exp().getAppMetrics().name(0);
        Object[] viaNone = DBE.dbeGetTLData(0, 0, APPMETRICS, PROP_NONE, 0, 0, start, 3 * NS, 5, true,
                new String[] {name});
        Object[] viaExp = DBE.dbeGetTLData(0, 0, APPMETRICS, PROP_EXPID, 0, 0, start, 3 * NS, 5, true,
                new String[] {name});
        assertArrayEquals((double[]) ((Object[]) viaExp[1])[0], (double[]) ((Object[]) viaNone[1])[0]);
        assertNull(viaNone[0]); // no representative events, even when asked for
    }

    @Test
    void metricTree() {
        Object[] tree = DBE.dbeGetAppMetricTree(0);
        assertNotNull(tree);
        String[] names = (String[]) tree[0];
        assertEquals(exp().getAppMetrics().metricCount(), names.length);
        assertEquals(names.length, ((String[]) tree[1]).length);
        assertEquals(names.length, ((String[]) tree[2]).length);
        assertEquals(names.length, ((int[]) tree[3]).length);
        assertEquals(names.length, ((double[]) tree[4]).length);
        assertEquals(names.length, ((double[]) tree[5]).length);
        int i = Arrays.asList(names).indexOf("AddressBookService:updateNodeHdl");
        assertTrue(i >= 0);
        assertEquals("number of AddressBookService:updateNode handled", ((String[]) tree[1])[i]);
        assertEquals("app", ((String[]) tree[2])[i]);
    }

    @Test
    void binnedSeries() {
        AppMetrics m = exp().getAppMetrics();
        String name = "AddressBookService:updateNodeHdl";
        int idx = m.indexOf(name);
        double[] samples = m.values(idx);
        long start = exp().getStartTime();
        // 3 s bins starting at the first sample: each bin holds exactly one sample
        Object[] res = DBE.dbeGetTLData(0, 0, APPMETRICS, PROP_EXPID, 0, 0, start, 3 * NS, 5, false,
                new String[] {name, "no:such:metric"});
        assertNotNull(res);
        assertNull(res[0]);
        Object[] vals = (Object[]) res[1];
        assertEquals(2, vals.length);
        double[] series = (double[]) vals[0];
        for (int b = 0; b < 5; b++) {
            assertEquals(Double.isNaN(samples[b]) ? AppMetrics.MISSING_WIRE : samples[b], series[b], 1e-9);
        }
        double[] unknown = (double[]) vals[1];
        assertEquals(5, unknown.length);
        assertTrue(Arrays.stream(unknown).allMatch(v -> v == AppMetrics.MISSING_WIRE));
    }

    @Test
    void stackedChartGetsOneSeriesPerMember() {
        AppMetrics m = exp().getAppMetrics();
        String a = m.name(0);
        String b = m.name(1);
        String stack = a + AppMetrics.STACK_SEPARATOR + b + AppMetrics.STACK_SEPARATOR + "no:such:metric";
        Object[] res = DBE.dbeGetTLData(0, 0, APPMETRICS, PROP_NONE, 0, 0, exp().getStartTime(), 3 * NS, 4,
                false, new String[] {stack, a});
        Object[] vals = (Object[]) res[1];
        Object[] members = (Object[]) vals[0];
        assertEquals(3, members.length);
        assertArrayEquals((double[]) vals[1], (double[]) members[0]); // same as the plain chart of a
        assertEquals(4, ((double[]) members[1]).length);
        assertTrue(Arrays.stream((double[]) members[2]).allMatch(v -> v == AppMetrics.MISSING_WIRE));
    }

    @Test
    void timeFiltersHideTheSamplesOutsideTheRange() {
        AppMetrics m = exp().getAppMetrics();
        int idx = m.indexOf("AddressBookService:updateNodeHdl");
        long start = exp().getStartTime();
        // bins of one sampling period from the first sample; keep only samples 2 and 3 (6 s .. 9 s)
        String range = "TSTAMP_LO >= " + (6 * NS) + " && TSTAMP_LO <= " + (9 * NS);
        try {
            assertNull(DBE.dbeSetFilterStr(0, range));
            double[] series = (double[]) ((Object[]) ((Object[]) DBE.dbeGetTLData(0, 0, APPMETRICS, PROP_NONE,
                    0, 0, start, 3 * NS, 5, false, new String[] {m.name(idx)}))[1])[0];
            for (int b = 0; b < 5; b++) {
                boolean kept = b == 2 || b == 3;
                double expected = kept && !Double.isNaN(m.values(idx)[b]) ? m.values(idx)[b] : AppMetrics.MISSING_WIRE;
                assertEquals(expected, series[b], 1e-9, "bin " + b);
            }
            assertTrue(DBE.dbeHasTLData(0, new int[] {0}, new int[] {APPMETRICS},
                    new int[] {PROP_NONE}, new int[] {0}, new int[] {0})[0]);
            // a range with no sample leaves nothing to show
            assertNull(DBE.dbeSetFilterStr(0, "TSTAMP_LO >= " + (1000 * NS)));
            assertFalse(DBE.dbeHasTLData(0, new int[] {0}, new int[] {APPMETRICS},
                    new int[] {PROP_NONE}, new int[] {0}, new int[] {0})[0]);
        } finally {
            DBE.dbeSetFilterStr(0, null);
        }
        assertTrue(DBE.dbeHasTLData(0, new int[] {0}, new int[] {APPMETRICS},
                new int[] {PROP_NONE}, new int[] {0}, new int[] {0})[0]);
    }

    @Test
    void filtersOnOtherPropertiesLeaveTheSamplesAlone() {
        // metric samples have no thread, CPU or stack: such filters must not hide them
        for (String filter : new String[] {"THRID == 1", "THRID == 99999", "CPUID == 0", "LWPID == 1"}) {
            try {
                String err = DBE.dbeSetFilterStr(0, filter);
                boolean has = DBE.dbeHasTLData(0, new int[] {0}, new int[] {APPMETRICS},
                        new int[] {PROP_NONE}, new int[] {0}, new int[] {0})[0];
                if (err == null) {
                    assertTrue(has, filter);
                }
            } finally {
                DBE.dbeSetFilterStr(0, null);
            }
        }
    }

    @Test
    void sampleDetails() {
        AppMetrics m = exp().getAppMetrics();
        String a = m.name(0);
        Object[] res = DBE.dbeGetAppMetricValues(0, 0, PROP_NONE, 2, new String[] {a, "no:such:metric"});
        assertNotNull(res);
        assertEquals(exp().getStartTime() + m.times()[2], ((long[]) res[0])[0]);
        double[] values = (double[]) res[1];
        double expected = m.values(0)[2];
        assertEquals(Double.isNaN(expected) ? AppMetrics.MISSING_WIRE : expected, values[0], 1e-9);
        assertEquals(AppMetrics.MISSING_WIRE, values[1]);
        assertNull(DBE.dbeGetAppMetricValues(0, 0, PROP_NONE, m.sampleCount(), new String[] {a}));
        assertNull(DBE.dbeGetAppMetricValues(0, 0, PROP_NONE, -1, new String[] {a}));
    }

    @Test
    void sampleDetailsFollowTheFilter() {
        AppMetrics m = exp().getAppMetrics();
        try {
            assertNull(DBE.dbeSetFilterStr(0, "TSTAMP_LO >= " + (6 * NS) + " && TSTAMP_LO <= " + (9 * NS)));
            // position 0 of the filtered view is now the sample at 6 s
            Object[] res = DBE.dbeGetAppMetricValues(0, 0, PROP_NONE, 0, new String[] {m.name(0)});
            assertEquals(exp().getStartTime() + 6 * NS, ((long[]) res[0])[0]);
            assertNull(DBE.dbeGetAppMetricValues(0, 0, PROP_NONE, 2, new String[] {m.name(0)}));
        } finally {
            DBE.dbeSetFilterStr(0, null);
        }
    }

    @Test
    void binsOutsideTheSamplesAreMissing() {
        long start = exp().getStartTime();
        Object[] res = DBE.dbeGetTLData(0, 0, APPMETRICS, PROP_EXPID, 0, 0, start - 100 * NS, 10 * NS, 3, false,
                new String[] {exp().getAppMetrics().name(0)});
        double[] series = (double[]) ((Object[]) res[1])[0];
        assertTrue(Arrays.stream(series).allMatch(v -> v == AppMetrics.MISSING_WIRE));
    }
}

package org.example.triangular_number;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;

class TriangularSearchTest {
    @Test
    void fourWorkersProduceTheSampleRangesAndSortedMatches() {
        var ranges = TriangularSearch.partition(100, 4);
        assertEquals(List.of(new TriangularSearch.Range(1, 11), new TriangularSearch.Range(12, 31),
                new TriangularSearch.Range(32, 61), new TriangularSearch.Range(62, 100)), ranges);
        List<Integer> matches = new ArrayList<>();
        int totalChecked = 0;
        for (var range : ranges) {
            List<Integer> progress = new ArrayList<>();
            matches.addAll(TriangularSearch.scan(range, () -> false, progress::add));
            int previous = 0;
            for (int checked : progress) {
                assertTrue(checked > previous && checked <= range.size());
                previous = checked;
            }
            assertEquals(range.size(), previous);
            totalChecked += previous;
        }
        assertEquals(List.of(1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78, 91), matches);
        assertEquals(100, totalChecked);
        assertEquals(87, totalChecked - matches.size());
    }

    @Test
    void rangesCoverTheWholeIntervalAndDifferInSizeWhenPossible() {
        for (int limit : List.of(1, 4, 8, 63, 64, 65, 128, 10000, Integer.MAX_VALUE)) {
            for (int workers = 1; workers <= Math.min(64, limit); workers++) {
                var ranges = TriangularSearch.partition(limit, workers);
                assertEquals(workers, ranges.size());
                int end = 0;
                for (var range : ranges) {
                    assertEquals(end + 1, range.start());
                    assertTrue(range.size() >= 1);
                    end = range.end();
                }
                assertEquals(limit, end);
                if (workers > 1 && workers < limit) {
                    assertTrue(ranges.stream().map(TriangularSearch.Range::size).distinct().count() > 1);
                }
            }
        }
    }

    @Test
    void scansIncludeTriangularEndpointsAndHandleMaximumIntegers() {
        assertEquals(List.of(1), scan(1, 1));
        assertEquals(List.of(1, 3, 6, 10), scan(1, 10));
        assertEquals(List.of(10), scan(10, 10));
        assertEquals(List.of(), scan(11, 14));
        int largestMatch = 2_147_450_880;
        assertEquals(List.of(largestMatch), scan(largestMatch - 1, Integer.MAX_VALUE));
        assertEquals(List.of(), scan(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void validatesIntegerInputsAndBothLimitsOnThreadCount() {
        assertEquals(new TriangularSearch.Inputs(10000, 4), TriangularSearch.parseInputs(" 10000 ", "4"));
        assertEquals(new TriangularSearch.Inputs(Integer.MAX_VALUE, 64),
                TriangularSearch.parseInputs("2147483647", "64"));
        for (String invalid : List.of("", "x", "4.5", "-1", "0", "2147483648")) {
            assertThrows(IllegalArgumentException.class, () -> TriangularSearch.parseInputs(invalid, "1"));
            assertThrows(IllegalArgumentException.class, () -> TriangularSearch.parseInputs("100", invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> TriangularSearch.parseInputs("100", "65"));
        assertThrows(IllegalArgumentException.class, () -> TriangularSearch.parseInputs("3", "4"));
    }

    @Test
    void cancellationDiscardsResultsAndStopsProgress() {
        var range = new TriangularSearch.Range(1, 10000);
        List<Integer> progress = new ArrayList<>();
        assertThrows(CancellationException.class,
                () -> TriangularSearch.scan(range, () -> !progress.isEmpty(), progress::add));
        assertEquals(1, progress.size());
        assertTrue(progress.get(0) < range.size());
    }

    private List<Integer> scan(int start, int end) {
        return TriangularSearch.scan(new TriangularSearch.Range(start, end), () -> false, checked -> {});
    }
}

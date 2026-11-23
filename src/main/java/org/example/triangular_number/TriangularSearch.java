package org.example.triangular_number;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

final class TriangularSearch {
    private static final int MAX_THREADS = 64;

    private TriangularSearch() {}

    record Inputs(int upperLimit, int threadCount) {
        Inputs {
            if (upperLimit < 1) {
                throw new IllegalArgumentException("Upper limit must be a positive 32-bit integer.");
            }
            int maximum = Math.min(upperLimit, MAX_THREADS);
            if (threadCount < 1 || threadCount > maximum) {
                throw new IllegalArgumentException("Thread count must be between 1 and " + maximum + ".");
            }
        }
    }

    record Range(int start, int end) {
        Range {
            if (start < 1 || end < start) {
                throw new IllegalArgumentException("Invalid search range.");
            }
        }

        int size() {
            return end - start + 1;
        }
    }

    static Inputs parseInputs(String upperLimit, String threadCount) {
        return new Inputs(parseInteger(upperLimit, "Upper limit"), parseInteger(threadCount, "Thread count"));
    }

    private static int parseInteger(String text, String label) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a positive 32-bit integer.");
        }
    }

    static List<Range> partition(int upperLimit, int threadCount) {
        new Inputs(upperLimit, threadCount);
        long remaining = upperLimit - threadCount;
        long totalWeight = triangular(threadCount);
        int[] sizes = new int[threadCount];
        int previousEnd = 0;
        boolean uniform = true;
        for (int worker = 1; worker <= threadCount; worker++) {
            long weightedEnd = (remaining * triangular(worker) + totalWeight - 1) / totalWeight;
            int end = Math.toIntExact(worker + weightedEnd);
            sizes[worker - 1] = end - previousEnd;
            uniform &= sizes[worker - 1] == sizes[0];
            previousEnd = end;
        }
        if (uniform && threadCount > 1 && remaining > 0) {
            sizes[0]--;
            sizes[threadCount - 1]++;
        }

        List<Range> ranges = new ArrayList<>(threadCount);
        int end = 0;
        for (int size : sizes) {
            ranges.add(new Range(end + 1, end + size));
            end += size;
        }
        return ranges;
    }

    static List<Integer> scan(Range range, BooleanSupplier cancelled, IntConsumer progress) {
        List<Integer> matches = new ArrayList<>();
        long index = (long) ((Math.sqrt(8L * range.start() + 1) - 1) / 2);
        long nextMatch = triangular(index);
        if (nextMatch < range.start()) {
            nextMatch += ++index;
        }

        int reportInterval = Math.max(1, range.size() / 100);
        int batchSize = Math.min(4096, reportInterval);
        long nextReport = reportInterval;
        long value = range.start();
        while (value <= range.end()) {
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }
            long batchEnd = Math.min(value + batchSize - 1, range.end());
            for (; value <= batchEnd; value++) {
                if (value == nextMatch) {
                    matches.add((int) value);
                    nextMatch += ++index;
                }
            }
            int checked = (int) (value - range.start());
            if (checked >= nextReport || checked == range.size()) {
                progress.accept(checked);
                nextReport = (long) checked + reportInterval;
            }
        }
        return matches;
    }

    private static long triangular(long index) {
        return index * (index + 1) / 2;
    }
}

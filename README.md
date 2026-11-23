# Triangular Number

Triangular Number is a JavaFX desktop application that finds every positive triangular number up to an inclusive upper limit. The triangular number at position `k` is the sum of the integers from 1 through `k`, or `k(k + 1) / 2`.

The search defaults to an upper limit of 10,000 and four worker threads. The upper limit can be any positive 32-bit integer. The thread count ranges from 1 to the smaller of the upper limit and 64.

Each worker checks a separate, contiguous range. The progress pane shows each worker’s range, number count, progress, and state. A completed search lists the matches in ascending order and reports counts for triangular numbers, non-triangular numbers, and all values checked. Stopping a search clears its partial results.

## Example

With an upper limit of 100 and four threads, the matches are

```text
1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78, 91
```

Of the 100 integers checked, 13 are triangular and 87 are non-triangular.

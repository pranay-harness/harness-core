package io.harness.histogram;

/**
 * HistogramOptions define the number and size of buckets of a histogram.
 */
public interface HistogramOptions {
  /**
   * Returns the number of buckets in the histogram.
   */
  int getNumBuckets();

  /**
   Find the index of the bucket to which given value falls.
   If value is outside range, it returns closest bucket (first/last)
  */
  int findBucket(double value);

  /**
   * Returns start of bucket with a given index. If index is outside
   * [0..NumBuckets()-1] range, result is undefined.
   */
  double getBucketStart(int bucket);

  /**
   * Returns minimum weight for a bucket to be considered non-empty.
   */
  double getEpsilon();
}

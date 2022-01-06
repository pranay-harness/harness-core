/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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

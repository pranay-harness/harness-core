/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.histogram;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * HistogramOptions describing a histogram with exponentially growing bucket boundaries. The first bucket covers the
 * range [0..firstBucketSize). Bucket with index n has size equal to firstBucketSize * ratio^n.
 * It follows that the bucket with index n >= 1 starts at:
 *     firstBucketSize * (1 + ratio + ratio^2 + ... + ratio^(n-1)) =
 *     firstBucketSize * (ratio^n - 1) / (ratio - 1).
 * The last bucket start is larger or equal to maxValue.
 * Requires maxValue > 0, firstBucketSize > 0, ratio > 1, epsilon > 0.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExponentialHistogramOptions implements HistogramOptions {
  @Getter int numBuckets;
  double firstBucketSize;
  double ratio;
  @Getter double epsilon;

  public ExponentialHistogramOptions(double maxValue, double firstBucketSize, double ratio, double epsilon) {
    checkArgument(maxValue > 0.0 && firstBucketSize > 0.0 && epsilon > 0.0 && ratio > 1.0,
        "maxValue, firstBucketSize and epsilon must be > 0.0, ratio must be > 1.0");
    this.numBuckets = (int) Math.ceil(log(ratio, maxValue * (ratio - 1) / firstBucketSize + 1)) + 1;
    this.firstBucketSize = firstBucketSize;
    this.ratio = ratio;
    this.epsilon = epsilon;
  }

  // Returns the index of the bucket for given value. This is the inverse function to
  // GetBucketStart(), which yields the following formula for the bucket index:
  //    bucket(value) = floor(log(value/firstBucketSize*(ratio-1)+1) / log(ratio))
  @Override
  public int findBucket(double value) {
    if (value < this.firstBucketSize) {
      return 0;
    }
    int bucket = (int) log(this.ratio, value * (this.ratio - 1) / this.firstBucketSize + 1);
    if (bucket >= this.numBuckets) {
      return this.numBuckets - 1;
    }
    return bucket;
  }

  // Returns the start of the bucket with given index, according to the formula:
  //    bucketStart(bucket) = firstBucketSize * (ratio^bucket - 1) / (ratio - 1).
  @Override
  public double getBucketStart(int bucket) {
    checkElementIndex(bucket, this.numBuckets);
    if (bucket == 0) {
      return 0.0;
    }
    return this.firstBucketSize * (Math.pow(this.ratio, bucket) - 1) / (this.ratio - 1);
  }

  // Returns the logarithm of x to given base, so that: base^log(base, x) == x.
  private static double log(double base, double x) {
    return Math.log(x) / Math.log(base);
  }
}

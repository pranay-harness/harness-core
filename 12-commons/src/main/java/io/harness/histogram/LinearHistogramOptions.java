package io.harness.histogram;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * HistogramOptions describing a histogram with a given number of fixed-size buckets, with the first bucket start at
 * 0.0 and the last bucket start larger or equal to maxValue.
 * Requires maxValue > 0, bucketSize > 0, epsilon > 0.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LinearHistogramOptions implements HistogramOptions {
  @Getter int numBuckets;
  double bucketSize;
  @Getter double epsilon;

  public LinearHistogramOptions(double maxValue, double bucketSize, double epsilon) {
    checkArgument(maxValue > 0.0 && bucketSize > 0.0 && epsilon > 0.0, "maxValue, bucketSize & epsilon must be +ve");
    this.numBuckets = (int) Math.ceil((maxValue / bucketSize) + 1);
    this.bucketSize = bucketSize;
    this.epsilon = epsilon;
  }

  @Override
  public int findBucket(double value) {
    int bucket = (int) (value / this.bucketSize);
    if (bucket < 0) {
      return 0;
    }
    if (bucket >= this.numBuckets) {
      return this.numBuckets - 1;
    }
    return bucket;
  }

  @Override
  public double getBucketStart(int bucket) {
    checkElementIndex(bucket, this.numBuckets);
    return bucket * this.bucketSize;
  }
}

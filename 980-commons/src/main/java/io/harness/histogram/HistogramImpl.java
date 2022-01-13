/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.histogram;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Map;
import lombok.ToString;

/**
 * Simple bucket-based implementation of the Histogram interface. Each bucket holds the total weight of samples that
 * belong to it. getPercentile() returns the upper bound of the corresponding bucket. Resolution (bucket boundaries) of
 * the histogram depends on the options. There's no interpolation within buckets (i.e. one sample falls to exactly one
 * bucket). A bucket is considered empty if its weight is smaller than options.Epsilon().
 */
@ToString
public class HistogramImpl implements Histogram {
  static final int MAX_CHECKPOINT_WEIGHT = 10000;

  // Bucketing scheme
  private final HistogramOptions options;

  // Cumulative weight of samples in each bucket
  private final double[] bucketWeight;

  // Total cumulative weight of samples in all buckets
  private double totalWeight;

  // Index of first non-empty bucket if any, else last bucket index.
  private int minBucket;

  // Index of last non-empty bucket if any, else 0
  private int maxBucket;

  public HistogramImpl(HistogramOptions options) {
    this.options = options;
    this.bucketWeight = new double[options.getNumBuckets()];
    this.totalWeight = 0.0;
    this.minBucket = options.getNumBuckets() - 1;
    this.maxBucket = 0;
  }

  @Override
  public void addSample(double value, double weight, Instant time) {
    checkArgument(weight >= 0.0, "sample weight must be non-negative");
    int bucket = this.options.findBucket(value);
    this.bucketWeight[bucket] += weight;
    this.totalWeight += weight;
    if (bucket < this.minBucket && this.bucketWeight[bucket] >= this.options.getEpsilon()) {
      this.minBucket = bucket;
    }
    if (bucket > this.maxBucket && this.bucketWeight[bucket] >= this.options.getEpsilon()) {
      this.maxBucket = bucket;
    }
  }

  private double safeSubtract(double value, double sub, double epsilon) {
    value -= sub;
    if (value < epsilon) {
      return 0.0;
    }
    return value;
  }

  @Override
  public void subtractSample(double value, double weight, Instant time) {
    checkArgument(weight >= 0.0, "sample weight must be non-negative");
    int bucket = this.options.findBucket(value);
    double epsilon = this.options.getEpsilon();
    this.totalWeight = safeSubtract(this.totalWeight, weight, epsilon);
    this.bucketWeight[bucket] = safeSubtract(this.bucketWeight[bucket], weight, epsilon);
    updateMinAndMaxBucket();
  }

  @Override
  public void merge(Histogram other) {
    HistogramImpl o = (HistogramImpl) other;
    checkArgument(this.options.equals(o.options), "Can't merge histograms with different options");
    for (int bucket = o.minBucket; bucket <= o.maxBucket; bucket++) {
      this.bucketWeight[bucket] += o.bucketWeight[bucket];
    }
    this.totalWeight += o.totalWeight;
    this.minBucket = Math.min(this.minBucket, o.minBucket);
    this.maxBucket = Math.max(this.maxBucket, o.maxBucket);
  }

  @Override
  public double getPercentile(double percentile) {
    if (isEmpty()) {
      return 0.0;
    }
    double partialSum = 0.0;
    double threshold = percentile * this.totalWeight;
    int bucket;
    for (bucket = this.minBucket; bucket < this.maxBucket; bucket++) {
      partialSum += this.bucketWeight[bucket];
      if (partialSum >= threshold) {
        break;
      }
    }
    if (bucket < this.options.getNumBuckets() - 1) {
      // Return the end of the bucket.
      return this.options.getBucketStart(bucket + 1);
    }

    // Return the start of the last bucket (note that the last bucket
    // doesn't have an upper bound).
    return this.options.getBucketStart(bucket);
  }

  @Override
  public boolean isEmpty() {
    return this.bucketWeight[this.minBucket] < this.options.getEpsilon();
  }

  // Adjusts the value of minBucket & maxBucket after any operation that decreases weights.
  private void updateMinAndMaxBucket() {
    double epsilon = this.options.getEpsilon();
    int lastBucket = this.options.getNumBuckets() - 1;
    while (this.bucketWeight[this.minBucket] < epsilon && this.minBucket < lastBucket) {
      this.minBucket++;
    }
    while (this.bucketWeight[this.maxBucket]<epsilon&& this.maxBucket> 0) {
      this.maxBucket--;
    }
  }

  @Override
  public HistogramCheckpoint saveToCheckpoint() {
    ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();
    double max = 0;
    for (int bucket = this.minBucket; bucket <= this.maxBucket; bucket++) {
      if (this.bucketWeight[bucket] > max) {
        max = this.bucketWeight[bucket];
      }
    }
    if (max > 0) {
      double ratio = MAX_CHECKPOINT_WEIGHT / max;
      for (int bucket = this.minBucket; bucket <= this.maxBucket; bucket++) {
        int newWeight = (int) Math.round(this.bucketWeight[bucket] * ratio);
        if (newWeight > 0) {
          builder.put(bucket, newWeight);
        }
      }
    }
    return HistogramCheckpoint.builder().bucketWeights(builder.build()).totalWeight(this.totalWeight).build();
  }

  @Override
  public void loadFromCheckPoint(HistogramCheckpoint checkpoint) {
    checkNotNull(checkpoint, "Cannot load from empty checkpoint");
    checkArgument(checkpoint.getTotalWeight() >= 0.0, "Cannot load checkpoint with negative weight %s",
        checkpoint.getTotalWeight());
    long sum = 0;
    for (Map.Entry<Integer, Integer> bucketAndWeight : checkpoint.getBucketWeights().entrySet()) {
      int bucket = bucketAndWeight.getKey();
      int weight = bucketAndWeight.getValue();
      sum += weight;
      checkElementIndex(bucket, this.options.getNumBuckets(), "Checkpoint has invalid bucket index");
    }
    if (sum == 0) {
      return;
    }
    double ratio = checkpoint.getTotalWeight() / sum;
    for (Map.Entry<Integer, Integer> bucketAndWeight : checkpoint.getBucketWeights().entrySet()) {
      int bucket = bucketAndWeight.getKey();
      int weight = bucketAndWeight.getValue();
      if (bucket < this.minBucket) {
        this.minBucket = bucket;
      }
      if (bucket > this.maxBucket) {
        this.maxBucket = bucket;
      }
      this.bucketWeight[bucket] += weight * ratio;
    }
    this.totalWeight += checkpoint.getTotalWeight();
  }

  // Multiplies all weights by a given factor. Does not affect percentiles.
  void scale(double factor) {
    checkArgument(factor >= 0.0, "Scale factor %s must be non-negative");
    for (int bucket = this.minBucket; bucket <= this.maxBucket; bucket++) {
      this.bucketWeight[bucket] *= factor;
    }
    this.totalWeight *= factor;
    // Some buckets might become empty (weight < epsilon), so adjust min and max buckets.
    updateMinAndMaxBucket();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HistogramImpl other = (HistogramImpl) o;
    if (this.minBucket != other.minBucket || this.maxBucket != other.maxBucket || !this.options.equals(other.options)) {
      return false;
    }
    for (int bucket = this.minBucket; bucket <= this.maxBucket; bucket++) {
      double diff = this.bucketWeight[bucket] - other.bucketWeight[bucket];
      if (diff > 1e-15 || diff < -1e-15) {
        return false;
      }
    }
    return true;
  }
}

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.histogram;

import java.time.Instant;

/**
 * Histogram represents an approximate distribution of some variable
 */
public interface Histogram {
  /**
   * Add a sample with a given value and weight
   */
  void addSample(double value, double weight, Instant time);

  /**
   * Remove a sample with a given value and weight. Note that the total
   * weight of samples with a given value cannot be negative.
   */
  void subtractSample(double value, double weight, Instant time);

  /*
   Add all samples from another histogram. Requires the histograms to be
   of the exact same type.
  */
  void merge(Histogram other);

  /**
   * Returns an approximation of the given percentile of the distribution.
   * Note: the argument passed to Percentile() is a number between
   * 0 and 1. For example 0.5 corresponds to the median and 0.9 to the
   * 90th percentile.
   * If the histogram is empty, Percentile() returns 0.0.
   */
  double getPercentile(double percentile);

  /**
   * Returns true if the histogram is empty.
   */
  boolean isEmpty();

  /**
   * SaveToChekpoint returns a representation of the histogram as a
   * HistogramCheckpoint. During conversion buckets with small weights
   * can be omitted.
   */
  HistogramCheckpoint saveToCheckpoint();

  /**
   * LoadFromCheckpoint loads data from the checkpoint into the histogram
   * by appending samples.
   */
  void loadFromCheckPoint(HistogramCheckpoint histogramCheckPoint);
}

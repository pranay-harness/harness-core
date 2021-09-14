/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

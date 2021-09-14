/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.metrics;

/**
 * Possible types of thresholds that can be applied to a metric.
 * Created by mike@ on 6/19/17.
 */
public enum ThresholdComparisonType {
  /**
   * A threshold that is divided by the previous build's value to yield a ratio.
   */
  RATIO,
  /**
   * A threshold that is subtracted from the previous build's value to yield a delta.
   */
  DELTA,
  /**
   * A threshold that represents an absolute value to compare against rather than something in the previous build.
   */
  ABSOLUTE
}

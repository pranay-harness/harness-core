/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

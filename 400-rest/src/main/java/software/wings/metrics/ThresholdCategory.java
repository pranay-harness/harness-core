/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.metrics;

public enum ThresholdCategory {
  /**
   * Default threshold values. There are defined in code
   */
  DEFAULT,
  /**
   * Supervised threshold values. These are calculated from the last 6 months data
   */
  SUPERVISED,
  /**
   * User defined thershold values
   */
  USER_DEFINED
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

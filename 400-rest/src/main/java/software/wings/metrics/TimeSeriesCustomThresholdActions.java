/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.metrics;

public enum TimeSeriesCustomThresholdActions {
  FAIL_IMMEDIATELY,
  FAIL_AFTER_OCCURRENCES,
  FAIL_AFTER_CONSECUTIVE_OCCURRENCES;
}

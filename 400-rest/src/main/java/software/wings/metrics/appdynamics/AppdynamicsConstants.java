/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.metrics.appdynamics;

/**
 * Created by mike@ on 6/14/17.
 */
public interface AppdynamicsConstants {
  String RESPONSE_TIME_95 = "95th Percentile Response Time (ms)";
  String ERRORS_PER_MINUTE = "Errors per Minute";
  String STALL_COUNT = "Stall Count";
  String NUMBER_OF_SLOW_CALLS = "Number of Slow Calls";
  String CALLS_PER_MINUTE = "Calls per Minute";
  String AVG_RESPONSE_TIME = "Average Response Time (ms)";

  String ERROR_DISPLAY_METRIC_NAME = "Error Percentage";
  String STALL_COUNT_DISPLAY_METRIC_NAME = "Stall Count Percentage";
}

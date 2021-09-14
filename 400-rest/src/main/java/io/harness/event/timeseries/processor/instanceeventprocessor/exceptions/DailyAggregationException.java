/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.timeseries.processor.instanceeventprocessor.exceptions;

public class DailyAggregationException extends InstanceAggregationException {
  public DailyAggregationException(String message, Throwable cause) {
    super(message, cause);
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.timeseries.processor.instanceeventprocessor.exceptions;

public class HourAggregationException extends InstanceAggregationException {
  public HourAggregationException(String message, Throwable cause) {
    super(message, cause);
  }
}

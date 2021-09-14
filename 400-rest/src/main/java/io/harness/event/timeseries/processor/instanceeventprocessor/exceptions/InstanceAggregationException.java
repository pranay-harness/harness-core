/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.timeseries.processor.instanceeventprocessor.exceptions;

import io.harness.exception.WingsException;

public class InstanceAggregationException extends WingsException {
  public InstanceAggregationException(String message, Throwable cause) {
    super(message, cause);
  }
}

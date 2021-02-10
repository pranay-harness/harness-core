package io.harness.event.timeseries.processor.instanceeventprocessor.exceptions;

import io.harness.exception.WingsException;

public class InstanceAggregationException extends WingsException {
  public InstanceAggregationException(String message, Throwable cause) {
    super(message, cause);
  }
}

package io.harness.event.timeseries.processor.instanceeventprocessor.exceptions;

import io.harness.event.timeseries.processor.instanceeventprocessor.instanceaggregator.DailyAggregator;
import io.harness.event.timeseries.processor.instanceeventprocessor.instanceaggregator.HourlyAggregator;
import io.harness.event.timeseries.processor.instanceeventprocessor.instanceaggregator.InstanceAggregator;
import io.harness.exception.WingsException;

public class InstanceAggregationExceptionHandler {
  public static WingsException getException(InstanceAggregator instanceAggregator, String errorLog, Throwable ex) {
    if (instanceAggregator instanceof HourlyAggregator) {
      return new HourAggregationException(errorLog, ex);
    }
    if (instanceAggregator instanceof DailyAggregator) {
      return new DailyAggregationException(errorLog, ex);
    }
    return new InstanceAggregationException(errorLog, ex);
  }
}

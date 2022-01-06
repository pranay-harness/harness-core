/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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

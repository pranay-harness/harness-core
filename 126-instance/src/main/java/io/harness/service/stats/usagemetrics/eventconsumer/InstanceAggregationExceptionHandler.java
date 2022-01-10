/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.stats.usagemetrics.eventconsumer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.timeseries.processor.instanceeventprocessor.exceptions.DailyAggregationException;
import io.harness.event.timeseries.processor.instanceeventprocessor.exceptions.HourAggregationException;
import io.harness.event.timeseries.processor.instanceeventprocessor.exceptions.InstanceAggregationException;
import io.harness.exception.WingsException;
import io.harness.service.stats.usagemetrics.eventconsumer.instanceaggregator.DailyAggregator;
import io.harness.service.stats.usagemetrics.eventconsumer.instanceaggregator.HourlyAggregator;
import io.harness.service.stats.usagemetrics.eventconsumer.instanceaggregator.InstanceAggregator;

@OwnedBy(HarnessTeam.DX)
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

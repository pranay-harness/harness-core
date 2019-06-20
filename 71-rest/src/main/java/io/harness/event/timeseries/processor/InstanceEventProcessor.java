package io.harness.event.timeseries.processor;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

@Singleton
@Slf4j
public class InstanceEventProcessor implements EventProcessor {
  @Override
  public void processEvent(TimeSeriesEventInfo eventInfo) {
    logger.info(eventInfo.toString());
  }
}

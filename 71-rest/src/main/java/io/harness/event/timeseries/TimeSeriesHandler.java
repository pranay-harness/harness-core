package io.harness.event.timeseries;

import static io.harness.event.model.EventType.DEPLOYMENT_EVENT;
import static io.harness.event.model.EventType.DEPLOYMENT_VERIFIED;
import static io.harness.event.model.EventType.INSTANCE_EVENT;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.timeseries.processor.DeploymentEventProcessor;
import io.harness.event.timeseries.processor.InstanceEventProcessor;
import io.harness.event.timeseries.processor.VerificationEventProcessor;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

@Singleton
@Slf4j
public class TimeSeriesHandler implements EventHandler {
  @Inject DeploymentEventProcessor deploymentEventProcessor;
  @Inject InstanceEventProcessor instanceEventProcessor;
  @Inject VerificationEventProcessor verificationEventProcessor;

  @Inject
  public TimeSeriesHandler(EventListener eventListener) {
    registerForEvents(eventListener);
  }

  private void registerForEvents(EventListener eventListener) {
    eventListener.registerEventHandler(this, Sets.newHashSet(DEPLOYMENT_EVENT, INSTANCE_EVENT, DEPLOYMENT_VERIFIED));
  }

  @Override
  public void handleEvent(Event event) {
    switch (event.getEventType()) {
      case INSTANCE_EVENT:
        instanceEventProcessor.processEvent((TimeSeriesBatchEventInfo) event.getEventData().getEventInfo());
        break;
      case DEPLOYMENT_EVENT:
        deploymentEventProcessor.processEvent((TimeSeriesEventInfo) event.getEventData().getEventInfo());
        break;
      case DEPLOYMENT_VERIFIED:
        verificationEventProcessor.processEvent(event.getEventData().getProperties());
        break;
      default:
        logger.error("Invalid event typ e, dropping event : [{}]", event);
    }
  }
}

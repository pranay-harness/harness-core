package io.harness.event.timeseries;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.timeseries.processor.DeploymentEventProcessor;
import io.harness.event.timeseries.processor.ServiceGuardSetupEventProcessor;
import io.harness.event.timeseries.processor.VerificationEventProcessor;
import io.harness.event.timeseries.processor.instanceeventprocessor.InstanceEventProcessor;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

import java.util.Arrays;

import static io.harness.event.model.EventType.DEPLOYMENT_EVENT;
import static io.harness.event.model.EventType.DEPLOYMENT_VERIFIED;
import static io.harness.event.model.EventType.INSTANCE_EVENT;
import static io.harness.event.model.EventType.SERVICE_GUARD_SETUP;

@Singleton
@Slf4j
public class TimeSeriesHandler implements EventHandler {
  @Inject private DeploymentEventProcessor deploymentEventProcessor;
  @Inject private InstanceEventProcessor instanceEventProcessor;
  @Inject private VerificationEventProcessor verificationEventProcessor;
  @Inject private ServiceGuardSetupEventProcessor serviceGuardSetupEventProcessor;

  @Inject
  public TimeSeriesHandler(EventListener eventListener) {
    registerForEvents(eventListener);
  }

  private void registerForEvents(EventListener eventListener) {
    eventListener.registerEventHandler(
        this, Sets.newHashSet(DEPLOYMENT_EVENT, INSTANCE_EVENT, DEPLOYMENT_VERIFIED, SERVICE_GUARD_SETUP));
  }

  @Override
  public void handleEvent(Event event) {
    switch (event.getEventType()) {
      case INSTANCE_EVENT:
        try (AutoLogContext ignore = new TimeseriesLogContext(AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
          TimeSeriesBatchEventInfo eventInfo = (TimeSeriesBatchEventInfo) event.getEventData().getEventInfo();
          instanceEventProcessor.processEvent((TimeSeriesBatchEventInfo) event.getEventData().getEventInfo());
        } catch (Exception ex) {
          log.error(
              "Failed to process Event : [{}] , error : [{}]", event.toString(), Arrays.toString(ex.getStackTrace()));
        }
        break;
      case DEPLOYMENT_EVENT:
        try {
          deploymentEventProcessor.processEvent((TimeSeriesEventInfo) event.getEventData().getEventInfo());
        } catch (Exception ex) {
          log.error(
              "Failed to process Event : [{}] , error : [{}]", event.toString(), Arrays.toString(ex.getStackTrace()));
        }
        break;
      case DEPLOYMENT_VERIFIED:
        verificationEventProcessor.processEvent(event.getEventData().getProperties());
        break;
      case SERVICE_GUARD_SETUP:
        serviceGuardSetupEventProcessor.processEvent(event.getEventData().getProperties());
        break;
      default:
        log.error("Invalid event typ e, dropping event : [{}]", event);
    }
  }
}

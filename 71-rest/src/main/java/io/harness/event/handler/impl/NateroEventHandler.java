package io.harness.event.handler.impl;

import static io.harness.event.model.EventConstants.ACCOUNT_ID;
import static io.harness.event.model.EventConstants.INSTANCE_COUNT_TYPE;
import static io.harness.event.model.EventConstants.SETUP_DATA_TYPE;
import static io.harness.event.model.EventConstants.WORKFLOW_EXECUTION_STATUS;
import static io.harness.event.model.EventConstants.WORKFLOW_TYPE;
import static io.harness.event.model.EventType.DEPLOYMENT_DURATION;
import static io.harness.event.model.EventType.DEPLOYMENT_METADATA;
import static io.harness.event.model.EventType.INSTANCE_COUNT;
import static io.harness.event.model.EventType.SETUP_DATA;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hazelcast.util.Preconditions;
import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventType;
import io.harness.event.natero.NateroMetricsPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Created by Pranjal on 05/27/2019
 */
@Singleton
public class NateroEventHandler implements EventHandler {
  private static final Logger log = LoggerFactory.getLogger(NateroEventHandler.class);
  @Inject private NateroMetricsPublisher nateroMetricsPublisher;

  private enum NateroModules { Deployment, Setup }

  private static final Set<EventType> EVENT_TYPES =
      Sets.newHashSet(DEPLOYMENT_METADATA, DEPLOYMENT_DURATION, SETUP_DATA, INSTANCE_COUNT);

  @Inject
  public NateroEventHandler(EventListener eventListener) {
    registerEventHandlers(eventListener);
  }

  private void registerEventHandlers(EventListener eventListener) {
    eventListener.registerEventHandler(this, EVENT_TYPES);
  }

  @Override
  public void handleEvent(Event event) {
    Preconditions.checkTrue(EVENT_TYPES.contains(event.getEventType()), "Unknown event type " + event.getEventType());

    Map<String, String> properties = event.getEventData().getProperties();
    String accountId = properties.get(ACCOUNT_ID);
    if (nateroMetricsPublisher.isNATERO_ENABLED()) {
      Preconditions.checkNotNull(accountId);
      switch (event.getEventType()) {
        case DEPLOYMENT_METADATA:
          nateroMetricsPublisher.publishNateroFeatureMetric(accountId, NateroModules.Deployment.name(),
              event.getEventType().name() + "_" + properties.get(WORKFLOW_EXECUTION_STATUS) + "_"
                  + properties.get(WORKFLOW_TYPE),
              1);
          break;
        case DEPLOYMENT_DURATION:
          nateroMetricsPublisher.publishNateroFeatureMetric(
              accountId, NateroModules.Deployment.name(), event.getEventType().name(), event.getEventData().getValue());
          break;
        case SETUP_DATA:
          nateroMetricsPublisher.publishNateroFeatureMetric(
              accountId, NateroModules.Setup.name(), properties.get(SETUP_DATA_TYPE), event.getEventData().getValue());
          break;
        case INSTANCE_COUNT:
          nateroMetricsPublisher.publishNateroFeatureMetric(accountId, NateroModules.Deployment.name(),
              "Instance_" + properties.get(INSTANCE_COUNT_TYPE), event.getEventData().getValue());
          break;

        default:
          log.error("Invalid event reported to NateroEventHandler");
      }
    }
  }
}

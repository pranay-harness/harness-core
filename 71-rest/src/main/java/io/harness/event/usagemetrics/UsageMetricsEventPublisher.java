package io.harness.event.usagemetrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Singleton
public class UsageMetricsEventPublisher {
  private static Logger logger = LoggerFactory.getLogger(UsageMetricsEventPublisher.class);
  @Inject EventPublisher eventPublisher;
  @Inject private ExecutorService executorService;

  /***
   *
   * @param status
   * @param manual
   * @param accountId
   * @param accountName
   * @return
   */
  public void publishDeploymentMetadataEvent(
      ExecutionStatus status, boolean manual, String accountId, String accountName) {
    Map properties = new HashMap<>();
    properties.put(EventConstants.ACCOUNTID, accountId);
    properties.put(EventConstants.ACCOUNTNAME, accountName);
    properties.put(EventConstants.WORKFLOW_EXECUTION_STATUS, status);
    properties.put(EventConstants.WORKFLOW_TYPE,
        manual ? EventConstants.MANUAL_WORKFLOW_TYPE : EventConstants.AUTOMATIC_WORKFLOW_TYPE);

    EventData eventData = EventData.builder().properties(properties).build();
    publishEvent(Event.builder().eventType(EventType.DEPLOYMENT_METADATA).eventData(eventData).build());
  }

  /**
   *
   * @param duration
   * @param accountId
   * @param accountName
   * @return
   */
  public void publishDeploymentDurationEvent(long duration, String accountId, String accountName) {
    Map properties = new HashMap();
    properties.put(EventConstants.ACCOUNTID, accountId);
    properties.put(EventConstants.ACCOUNTNAME, accountName);
    EventData eventData = EventData.builder().properties(properties).value(duration).build();
    publishEvent(Event.builder().eventType(EventType.DEPLOYMENT_DURATION).eventData(eventData).build());
  };

  /**
   *
   * @param accountId
   * @param accountName
   */
  public void publishUserLoginEvent(String accountId, String accountName) {
    Map properties = new HashMap();
    properties.put(EventConstants.ACCOUNTID, accountId);
    properties.put(EventConstants.ACCOUNTNAME, accountName);
    properties.put(EventConstants.USER_LOGGED_IN, Boolean.TRUE.toString());
    EventData eventData = EventData.builder().properties(properties).build();
    publishEvent(Event.builder().eventType(EventType.USERS_LOGGED_IN).eventData(eventData).build());
  };

  /**
   *
   * @param accountId
   * @param accountName
   */
  public void publishUserLogoutEvent(String accountId, String accountName) {
    Map properties = new HashMap();
    properties.put(EventConstants.ACCOUNTID, accountId);
    properties.put(EventConstants.ACCOUNTNAME, accountName);
    properties.put(EventConstants.USER_LOGGED_IN, Boolean.FALSE.toString());
    EventData eventData = EventData.builder().properties(properties).build();
    publishEvent(Event.builder().eventType(EventType.USERS_LOGGED_IN).eventData(eventData).build());
  }

  private void publishEvent(Event event) {
    executorService.submit(() -> {
      try {
        eventPublisher.publishEvent(event);
      } catch (Exception e) {
        logger.error("Failed to publish event:[{}]", event.getEventType(), e);
      }
    });
  }
}

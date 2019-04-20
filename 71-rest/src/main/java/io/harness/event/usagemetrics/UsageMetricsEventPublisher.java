package io.harness.event.usagemetrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Singleton
@Slf4j
public class UsageMetricsEventPublisher {
  @Inject EventPublisher eventPublisher;
  @Inject private ExecutorService executorService;

  /***
   *
   * @param status
   * @param manual
   * @param accountId
   * @param accountName
   * @param workflowId
   * @param workflowName
   * @param applicationId
   * @param applicationName
   * @return
   */
  public void publishDeploymentMetadataEvent(ExecutionStatus status, boolean manual, String accountId,
      String accountName, String workflowId, String workflowName, String applicationId, String applicationName) {
    Map properties = new HashMap<>();
    properties.put(EventConstants.ACCOUNT_ID, accountId);
    properties.put(EventConstants.ACCOUNT_NAME, accountName);
    properties.put(EventConstants.WORKFLOW_EXECUTION_STATUS, status);
    properties.put(EventConstants.WORKFLOW_TYPE,
        manual ? EventConstants.MANUAL_WORKFLOW_TYPE : EventConstants.AUTOMATIC_WORKFLOW_TYPE);
    properties.put(EventConstants.WORKFLOW_ID, workflowId);
    properties.put(EventConstants.WORKFLOW_NAME, workflowName);
    properties.put(EventConstants.APPLICATION_ID, applicationId);
    properties.put(EventConstants.APPLICATION_NAME, applicationName);

    EventData eventData = EventData.builder().properties(properties).build();
    publishEvent(Event.builder().eventType(EventType.DEPLOYMENT_METADATA).eventData(eventData).build());
  }

  /**
   *
   * @param duration
   * @param accountId
   * @param accountName
   * @param workflowId
   * @param workflowName
   * @param applicationId
   * @param applicationName
   */
  public void publishDeploymentDurationEvent(long duration, String accountId, String accountName, String workflowId,
      String workflowName, String applicationId, String applicationName) {
    Map properties = new HashMap();
    properties.put(EventConstants.ACCOUNT_ID, accountId);
    properties.put(EventConstants.ACCOUNT_NAME, accountName);
    properties.put(EventConstants.WORKFLOW_ID, workflowId);
    properties.put(EventConstants.WORKFLOW_NAME, workflowName);
    properties.put(EventConstants.APPLICATION_ID, applicationId);
    properties.put(EventConstants.APPLICATION_NAME, applicationName);
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
    properties.put(EventConstants.ACCOUNT_ID, accountId);
    properties.put(EventConstants.ACCOUNT_NAME, accountName);
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
    properties.put(EventConstants.ACCOUNT_ID, accountId);
    properties.put(EventConstants.ACCOUNT_NAME, accountName);
    properties.put(EventConstants.USER_LOGGED_IN, Boolean.FALSE.toString());
    EventData eventData = EventData.builder().properties(properties).build();
    publishEvent(Event.builder().eventType(EventType.USERS_LOGGED_IN).eventData(eventData).build());
  }

  /**
   *
   * @param accountId
   * @param accountName
   * @param setupDataCount
   */
  public void publishSetupDataMetric(String accountId, String accountName, long setupDataCount, String setupDataType) {
    Map properties = new HashMap();
    properties.put(EventConstants.ACCOUNT_ID, accountId);
    properties.put(EventConstants.ACCOUNT_NAME, accountName);
    properties.put(EventConstants.SETUP_DATA_TYPE, setupDataType);
    EventData eventData = EventData.builder().properties(properties).value(setupDataCount).build();
    publishEvent(Event.builder().eventType(EventType.SETUP_DATA).eventData(eventData).build());
  }

  /**
   *
   * @param accountId
   * @param accountName
   * @param instanceCount
   * @param  instanceCountyType
   */
  public void publishInstanceMetric(
      String accountId, String accountName, double instanceCount, String instanceCountyType) {
    Map properties = new HashMap();
    properties.put(EventConstants.ACCOUNT_ID, accountId);
    properties.put(EventConstants.ACCOUNT_NAME, accountName);
    properties.put(EventConstants.INSTANCE_COUNT_TYPE, instanceCountyType);
    EventData eventData = EventData.builder().properties(properties).value(instanceCount).build();
    publishEvent(Event.builder().eventType(EventType.INSTANCE_COUNT).eventData(eventData).build());
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

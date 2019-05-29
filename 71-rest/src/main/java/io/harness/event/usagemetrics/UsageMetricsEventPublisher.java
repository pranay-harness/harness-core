package io.harness.event.usagemetrics;

import static io.harness.event.model.EventConstants.ACCOUNT_CREATED_AT;
import static io.harness.event.model.EventConstants.ACCOUNT_ID;
import static io.harness.event.model.EventConstants.ACCOUNT_NAME;
import static io.harness.event.model.EventConstants.ACCOUNT_STATUS;
import static io.harness.event.model.EventConstants.ACCOUNT_TYPE;
import static io.harness.event.model.EventConstants.APPLICATION_ID;
import static io.harness.event.model.EventConstants.APPLICATION_NAME;
import static io.harness.event.model.EventConstants.AUTOMATIC_WORKFLOW_TYPE;
import static io.harness.event.model.EventConstants.COMPANY_NAME;
import static io.harness.event.model.EventConstants.INSTANCE_COUNT_TYPE;
import static io.harness.event.model.EventConstants.MANUAL_WORKFLOW_TYPE;
import static io.harness.event.model.EventConstants.SETUP_DATA_TYPE;
import static io.harness.event.model.EventConstants.USER_LOGGED_IN;
import static io.harness.event.model.EventConstants.WORKFLOW_EXECUTION_STATUS;
import static io.harness.event.model.EventConstants.WORKFLOW_ID;
import static io.harness.event.model.EventConstants.WORKFLOW_NAME;
import static io.harness.event.model.EventConstants.WORKFLOW_TYPE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

@Singleton
@Slf4j
public class UsageMetricsEventPublisher {
  @Inject EventPublisher eventPublisher;
  @Inject private ExecutorService executorService;
  SimpleDateFormat sdf;

  public UsageMetricsEventPublisher() {
    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    sdf.setTimeZone(TimeZone.getTimeZone(TimeZone.getTimeZone("Etc/UTC").toZoneId()));
  }

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
    properties.put(ACCOUNT_ID, accountId);
    properties.put(ACCOUNT_NAME, accountName);
    properties.put(WORKFLOW_EXECUTION_STATUS, status);
    properties.put(WORKFLOW_TYPE, manual ? MANUAL_WORKFLOW_TYPE : AUTOMATIC_WORKFLOW_TYPE);
    properties.put(WORKFLOW_ID, workflowId);
    properties.put(WORKFLOW_NAME, workflowName);
    properties.put(APPLICATION_ID, applicationId);
    properties.put(APPLICATION_NAME, applicationName);

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
    properties.put(ACCOUNT_ID, accountId);
    properties.put(ACCOUNT_NAME, accountName);
    properties.put(WORKFLOW_ID, workflowId);
    properties.put(WORKFLOW_NAME, workflowName);
    properties.put(APPLICATION_ID, applicationId);
    properties.put(APPLICATION_NAME, applicationName);
    EventData eventData = EventData.builder().properties(properties).value(duration).build();
    publishEvent(Event.builder().eventType(EventType.DEPLOYMENT_DURATION).eventData(eventData).build());
  }

  /**
   *
   * @param accountId
   * @param accountName
   */
  public void publishUserLoginEvent(String accountId, String accountName) {
    Map properties = new HashMap();
    properties.put(ACCOUNT_ID, accountId);
    properties.put(ACCOUNT_NAME, accountName);
    properties.put(USER_LOGGED_IN, Boolean.TRUE.toString());
    EventData eventData = EventData.builder().properties(properties).build();
    publishEvent(Event.builder().eventType(EventType.USERS_LOGGED_IN).eventData(eventData).build());
  }

  /**
   *
   * @param accountId
   * @param accountName
   */
  public void publishUserLogoutEvent(String accountId, String accountName) {
    Map properties = new HashMap();
    properties.put(ACCOUNT_ID, accountId);
    properties.put(ACCOUNT_NAME, accountName);
    properties.put(USER_LOGGED_IN, Boolean.FALSE.toString());
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
    properties.put(ACCOUNT_ID, accountId);
    properties.put(ACCOUNT_NAME, accountName);
    properties.put(SETUP_DATA_TYPE, setupDataType);
    EventData eventData = EventData.builder().properties(properties).value(setupDataCount).build();
    publishEvent(Event.builder().eventType(EventType.SETUP_DATA).eventData(eventData).build());
  }

  public void publishAccountMetadataMetric(Account account) {
    Map properties = new HashMap();
    properties.put(ACCOUNT_ID, account.getUuid());
    properties.put(ACCOUNT_NAME, account.getAccountName());
    properties.put(COMPANY_NAME, account.getCompanyName());
    properties.put(ACCOUNT_TYPE, account.getLicenseInfo().getAccountType());
    properties.put(ACCOUNT_STATUS, account.getLicenseInfo().getAccountStatus());
    properties.put(ACCOUNT_CREATED_AT, sdf.format(new Date(account.getCreatedAt())));
    EventData eventData =
        EventData.builder().properties(properties).value(account.getLicenseInfo().getLicenseUnits()).build();
    publishEvent(Event.builder().eventType(EventType.LICENSE_UNITS).eventData(eventData).build());
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
    properties.put(ACCOUNT_ID, accountId);
    properties.put(ACCOUNT_NAME, accountName);
    properties.put(INSTANCE_COUNT_TYPE, instanceCountyType);
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

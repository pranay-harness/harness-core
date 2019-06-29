package io.harness.event.usagemetrics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
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
import static java.util.stream.Collectors.groupingBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Lists;
import io.harness.beans.ExecutionStatus;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.publisher.EventPublisher;
import io.harness.event.timeseries.processor.EventProcessor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo.DataPoint;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

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

  public void publishDeploymentTimeSeriesEvent(String accountId, WorkflowExecution workflowExecution) {
    Map<String, String> stringData = new HashMap<>();
    Map<String, List<String>> listData = new HashMap<>();
    Map<String, Long> longData = new HashMap<>();
    stringData.put(EventProcessor.EXECUTIONID, workflowExecution.getUuid());
    stringData.put(EventProcessor.STATUS, workflowExecution.getStatus().name());
    if (workflowExecution.getPipelineExecution() != null) {
      stringData.put(EventProcessor.PIPELINE, workflowExecution.getPipelineExecution().getPipelineId());
      /**
       * TODO Get data for workflows in an execution
       */
    } else {
      listData.put(EventProcessor.WORKFLOW_LIST, Lists.newArrayList(workflowExecution.getWorkflowId()));
    }
    /**
     * TODO CLOUDPROVIDER
     */
    stringData.put(EventProcessor.APPID, workflowExecution.getAppId());
    longData.put(EventProcessor.STARTTIME, workflowExecution.getStartTs());
    longData.put(EventProcessor.ENDTIME, workflowExecution.getEndTs());

    if (!Lists.isNullOrEmpty(workflowExecution.getArtifacts())) {
      listData.put(EventProcessor.ARTIFACT_LIST,
          workflowExecution.getArtifacts().stream().map(Artifact::getBuildNo).collect(Collectors.toList()));
    }
    if (!Lists.isNullOrEmpty(workflowExecution.getServiceIds())) {
      listData.put(EventProcessor.SERVICE_LIST, workflowExecution.getServiceIds());
    }
    if (!Lists.isNullOrEmpty(workflowExecution.getEnvIds())) {
      listData.put(EventProcessor.ENV_LIST, workflowExecution.getEnvIds());
    }
    if (workflowExecution.getDeploymentTriggerId() != null) {
      stringData.put(EventProcessor.TRIGGER_ID, workflowExecution.getDeploymentTriggerId());
    }
    if (workflowExecution.getTriggeredBy() != null) {
      stringData.put(EventProcessor.TRIGGERED_BY, workflowExecution.getTriggeredBy().getUuid());
    }
    stringData.put(EventProcessor.ACCOUNTID, accountId);
    longData.put(EventProcessor.DURATION, workflowExecution.getEndTs() - workflowExecution.getStartTs());

    TimeSeriesEventInfo eventInfo = TimeSeriesEventInfo.builder()
                                        .accountId(accountId)
                                        .stringData(stringData)
                                        .listData(listData)
                                        .longData(longData)
                                        .build();
    EventData eventData = EventData.builder().eventInfo(eventInfo).build();
    publishEvent(Event.builder().eventType(EventType.DEPLOYMENT_EVENT).eventData(eventData).build());
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

  public void publishInstanceTimeSeries(String accountId, long timestamp, List<Instance> instances) {
    if (isEmpty(instances)) {
      return;
    }

    List<TimeSeriesBatchEventInfo.DataPoint> dataPointList = new ArrayList<>();

    // key - infraMappingId, value - Set<Instance>
    Map<String, List<Instance>> infraMappingInstancesMap =
        instances.stream().collect(groupingBy(Instance::getInfraMappingId));

    infraMappingInstancesMap.values().forEach(instanceList -> {
      if (isEmpty(instanceList)) {
        return;
      }

      int size = instanceList.size();
      Instance instance = instanceList.get(0);
      Map<String, Object> data = new HashMap<>();
      data.put(EventProcessor.ACCOUNTID, instance.getAccountId());
      data.put(EventProcessor.APPID, instance.getAppId());
      data.put(EventProcessor.SERVICEID, instance.getServiceId());
      data.put(EventProcessor.ENVID, instance.getEnvId());
      data.put(EventProcessor.INFRAMAPPINGID, instance.getInfraMappingId());
      data.put(EventProcessor.COMPUTEPROVIDERID, instance.getComputeProviderId());
      data.put(EventProcessor.INSTANCETYPE, instance.getInstanceType().name());
      data.put(EventProcessor.ARTIFACTID, instance.getLastArtifactId());
      data.put(EventProcessor.INSTANCECOUNT, size);

      DataPoint dataPoint = DataPoint.builder().data(data).build();
      dataPointList.add(dataPoint);
    });

    TimeSeriesBatchEventInfo eventInfo = TimeSeriesBatchEventInfo.builder()
                                             .accountId(accountId)
                                             .timestamp(timestamp)
                                             .dataPointList(dataPointList)
                                             .build();
    EventData eventData = EventData.builder().eventInfo(eventInfo).build();
    publishEvent(Event.builder().eventType(EventType.INSTANCE_EVENT).eventData(eventData).build());
  }
}

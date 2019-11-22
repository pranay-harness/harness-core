package io.harness.event.usagemetrics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.groupingBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Lists;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.publisher.EventPublisher;
import io.harness.event.timeseries.processor.EventProcessor;
import io.harness.queue.QueuePublisher;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentTimeSeriesEvent;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo.DataPoint;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;
import software.wings.service.intfc.WorkflowExecutionService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private QueuePublisher<DeploymentTimeSeriesEvent> deploymentTimeSeriesEventQueue;
  SimpleDateFormat sdf;

  public UsageMetricsEventPublisher() {
    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    sdf.setTimeZone(TimeZone.getTimeZone(TimeZone.getTimeZone("Etc/UTC").toZoneId()));
  }

  public void publishDeploymentTimeSeriesEvent(String accountId, WorkflowExecution workflowExecution) {
    DeploymentTimeSeriesEvent event = constructDeploymentTimeSeriesEvent(accountId, workflowExecution);
    executorService.submit(() -> {
      try {
        deploymentTimeSeriesEventQueue.send(event);
      } catch (Exception e) {
        logger.error("Failed to publish deployment time series event:[{}]", event.getId(), e);
      }
    });
  }

  public DeploymentTimeSeriesEvent constructDeploymentTimeSeriesEvent(
      String accountId, WorkflowExecution workflowExecution) {
    logger.info("Reporting execution");
    Map<String, String> stringData = new HashMap<>();
    Map<String, List<String>> listData = new HashMap<>();
    Map<String, Long> longData = new HashMap<>();
    Map<String, Integer> integerData = new HashMap<>();
    stringData.put(EventProcessor.EXECUTIONID, workflowExecution.getUuid());
    stringData.put(EventProcessor.STATUS, workflowExecution.getStatus().name());
    if (workflowExecution.getPipelineExecution() != null) {
      stringData.put(EventProcessor.PIPELINE, workflowExecution.getPipelineExecution().getPipelineId());
    } else {
      listData.put(EventProcessor.WORKFLOW_LIST, Lists.newArrayList(workflowExecution.getWorkflowId()));
    }

    stringData.put(EventProcessor.APPID, workflowExecution.getAppId());
    longData.put(EventProcessor.STARTTIME, workflowExecution.getStartTs());
    longData.put(EventProcessor.ENDTIME, workflowExecution.getEndTs());

    if (!Lists.isNullOrEmpty(workflowExecution.getCloudProviderIds())) {
      listData.put(EventProcessor.CLOUD_PROVIDER_LIST, workflowExecution.getCloudProviderIds());
    }

    if (!Lists.isNullOrEmpty(workflowExecution.getEnvironments())) {
      listData.put(EventProcessor.ENVTYPES,
          new ArrayList<>(workflowExecution.getEnvironments()
                              .stream()
                              .map(envSummary -> envSummary.getEnvironmentType().name())
                              .collect(Collectors.toSet())));
    }

    if (workflowExecution.getPipelineExecutionId() != null) {
      stringData.put(EventProcessor.PARENT_EXECUTION, workflowExecution.getPipelineExecutionId());
    }

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
    longData.put(EventProcessor.DURATION, workflowExecution.getDuration());
    longData.put(EventProcessor.ROLLBACK_DURATION, workflowExecution.getRollbackDuration());
    int instancesDeployed = workflowExecutionService.getInstancesDeployedFromExecution(workflowExecution);
    integerData.put(EventProcessor.INSTANCES_DEPLOYED, instancesDeployed);

    TimeSeriesEventInfo eventInfo = TimeSeriesEventInfo.builder()
                                        .accountId(accountId)
                                        .stringData(stringData)
                                        .listData(listData)
                                        .longData(longData)
                                        .integerData(integerData)
                                        .build();
    if (isEmpty(eventInfo.getListData())) {
      logger.info("TimeSeriesEventInfo has listData empty eventInfo=[{}]", eventInfo);
    }
    return DeploymentTimeSeriesEvent.builder().timeSeriesEventInfo(eventInfo).build();
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
      data.put(EventProcessor.CLOUDPROVIDERID, instance.getComputeProviderId());
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

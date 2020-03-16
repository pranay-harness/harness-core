package software.wings.delegatetasks.cv;

import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.HARNESS_HEARTBEAT_METRIC_NAME;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.MetricElement;
import software.wings.service.impl.analysis.MetricsDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
@Slf4j
public class MetricsDataCollectionTask<T extends MetricsDataCollectionInfo> extends AbstractDataCollectionTask<T> {
  private MetricsDataCollector<T> metricsDataCollector;
  @Inject private MetricDataStoreService metricStoreService;

  public MetricsDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected void collectAndSaveData(MetricsDataCollectionInfo dataCollectionInfo) throws DataCollectionException {
    this.metricsDataCollector = (MetricsDataCollector<T>) getDataCollector();
    final List<NewRelicMetricDataRecord> newRelicMetrics = new ArrayList<>();
    final List<Callable<List<MetricElement>>> callables = new ArrayList<>();
    if (dataCollectionInfo.isShouldSendHeartbeat()) {
      addHeartbeats(dataCollectionInfo, newRelicMetrics);
    }
    if (dataCollectionInfo.getHosts().isEmpty()) {
      callables.add(() -> metricsDataCollector.fetchMetrics());
    } else {
      Iterables.partition(dataCollectionInfo.getHosts(), metricsDataCollector.getHostBatchSize())
          .forEach(batch -> callables.add(() -> metricsDataCollector.fetchMetrics(batch)));
    }
    List<Optional<List<MetricElement>>> results = executeParallel(callables);
    results.forEach(result -> {
      if (result.isPresent()) {
        newRelicMetrics.addAll(
            result.get()
                .stream()
                .map(metricElement -> mapToNewRelicMetricDataRecord(dataCollectionInfo, metricElement))
                .collect(Collectors.toList()));
      }
    });
    logger.info("Saving " + newRelicMetrics.size() + " metrics");
    save(dataCollectionInfo, newRelicMetrics);
  }

  private NewRelicMetricDataRecord mapToNewRelicMetricDataRecord(
      DataCollectionInfoV2 dataCollectionInfo, MetricElement metricElement) {
    int dataCollectionMinute = (int) TimeUnit.MILLISECONDS.toMinutes(metricElement.getTimestamp());
    if (dataCollectionInfo.getDataCollectionStartTime() != null) {
      dataCollectionMinute = (int) (dataCollectionMinute
          - TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getDataCollectionStartTime().toEpochMilli()));
    }
    return NewRelicMetricDataRecord.builder()
        .name(metricElement.getName())
        .appId(dataCollectionInfo.getApplicationId())
        .workflowId(dataCollectionInfo.getWorkflowId())
        .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
        .serviceId(dataCollectionInfo.getServiceId())
        .stateExecutionId(dataCollectionInfo.getStateExecutionId())
        .stateType(dataCollectionInfo.getStateType())
        .timeStamp(metricElement.getTimestamp())
        .cvConfigId(dataCollectionInfo.getCvConfigId())
        .host(metricElement.getHost())
        .values(metricElement.getValues())
        .groupName(metricElement.getGroupName())
        .tag(metricElement.getTag())
        .dataCollectionMinute(dataCollectionMinute)
        .build();
  }

  private void save(MetricsDataCollectionInfo dataCollectionInfo, List<NewRelicMetricDataRecord> records)
      throws DataCollectionException {
    boolean response = metricStoreService.saveNewRelicMetrics(dataCollectionInfo.getAccountId(),
        dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), getTaskId(), records);
    if (!response) {
      throw new DataCollectionException("Unable to save metrics elements. Manager API returned false");
    }
  }

  protected void addHeartbeats(
      MetricsDataCollectionInfo metricsDataCollectionInfo, List<NewRelicMetricDataRecord> newRelicMetrics) {
    int heartbeatMin = (int) (TimeUnit.MILLISECONDS.toMinutes(metricsDataCollectionInfo.getEndTime().toEpochMilli()));
    Set<String> groups = new HashSet<>();
    for (Map.Entry<String, String> entry : metricsDataCollectionInfo.getHostsToGroupNameMap().entrySet()) {
      if (!groups.contains(entry.getValue())) {
        int dataCollectionMinute = heartbeatMin;
        logger.info("Heartbeat min: " + heartbeatMin);
        if (metricsDataCollectionInfo.getDataCollectionStartTime() != null) {
          // unfortunately there is a inconsistency between how heartbeat is created for workflow and how it's created
          // for service guard.
          dataCollectionMinute =
              (int) ((dataCollectionMinute
                         - TimeUnit.MILLISECONDS.toMinutes(
                               metricsDataCollectionInfo.getDataCollectionStartTime().toEpochMilli()))
                  - 1);
          logger.info("dataCollectionMinute min: " + dataCollectionMinute);
        }
        newRelicMetrics.add(NewRelicMetricDataRecord.builder()
                                .stateType(metricsDataCollectionInfo.getStateType())
                                .name(HARNESS_HEARTBEAT_METRIC_NAME)
                                .appId(metricsDataCollectionInfo.getApplicationId())
                                .workflowId(metricsDataCollectionInfo.getWorkflowId())
                                .workflowExecutionId(metricsDataCollectionInfo.getWorkflowExecutionId())
                                .serviceId(metricsDataCollectionInfo.getServiceId())
                                .stateExecutionId(metricsDataCollectionInfo.getStateExecutionId())
                                .dataCollectionMinute(dataCollectionMinute)
                                .timeStamp(TimeUnit.MINUTES.toMillis(heartbeatMin))
                                .level(ClusterLevel.H0)
                                .cvConfigId(metricsDataCollectionInfo.getCvConfigId())
                                .groupName(entry.getValue())
                                .build());
        groups.add(entry.getValue());
      }
    }
  }
}

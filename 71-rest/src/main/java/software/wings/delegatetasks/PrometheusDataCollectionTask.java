package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DURATION_TO_ASK_MINUTES;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.sm.states.AbstractAnalysisState.END_TIME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.HOST_NAME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.START_TIME_PLACE_HOLDER;

import com.google.common.base.Preconditions;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.TaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.service.impl.prometheus.PrometheusMetricDataResponse;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 2/6/18.
 */
@Slf4j
public class PrometheusDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private PrometheusDataCollectionInfo dataCollectionInfo;
  @Inject DelegateCVActivityLogService delegateCVActivityLogService;
  @Inject private PrometheusDelegateService prometheusDelegateService;
  @Inject private MetricDataStoreService metricStoreService;
  @Inject private DelegateLogService delegateLogService;

  public PrometheusDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    dataCollectionInfo = (PrometheusDataCollectionInfo) parameters;
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.PROMETHEUS)
        .build();
  }

  @Override
  protected StateType getStateType() {
    return StateType.PROMETHEUS;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected boolean is24X7Task() {
    return getTaskType().equals(TaskType.PROMETHEUS_COLLECT_24_7_METRIC_DATA.name());
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new PrometheusMetricCollector(dataCollectionInfo, taskResult, is24X7Task());
  }

  private class PrometheusMetricCollector implements Runnable {
    private final PrometheusDataCollectionInfo dataCollectionInfo;
    private final DataCollectionTaskResult taskResult;
    private long collectionStartTime;
    private int dataCollectionMinute;
    private PrometheusConfig prometheusConfig;
    private boolean is247Task;

    private PrometheusMetricCollector(
        PrometheusDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult, boolean is247Task) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      prometheusConfig = dataCollectionInfo.getPrometheusConfig();
      this.is247Task = is247Task;
    }

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      int retry = 0;
      while (!completed.get() && retry < RETRIES) {
        try {
          logger.info("starting metric data collection for {} for minute {} is247Task {}", dataCollectionInfo,
              dataCollectionMinute, is247Task);
          TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricDataRecords = getMetricsData();

          List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(metricDataRecords);
          if (!saveMetrics(prometheusConfig.getAccountId(), dataCollectionInfo.getApplicationId(),
                  dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
            logger.error("Error saving metrics to the database. DatacollectionMin: {} StateexecutionId: {}",
                dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
          } else {
            logger.info("Sent {} Prometheus metric records to the server for minute {}", recordsToSave.size(),
                dataCollectionMinute);
          }
          dataCollectionMinute++;
          collectionStartTime += TimeUnit.MINUTES.toMillis(1);
          if (dataCollectionMinute >= dataCollectionInfo.getCollectionTime() || is247Task) {
            // We are done with all data collection, so setting task status to success and quitting.
            logger.info(
                "Completed Prometheus collection task. So setting task status to success and quitting. StateExecutionId {}",
                dataCollectionInfo.getStateExecutionId());
            completed.set(true);
            taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
          }
          break;
        } catch (Throwable ex) {
          if (!(ex instanceof Exception) || ++retry >= RETRIES) {
            logger.error("error fetching metrics for {} for minute {}", dataCollectionInfo.getStateExecutionId(),
                dataCollectionMinute, ex);
            taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
            completed.set(true);
            break;
          } else {
            if (retry == 1) {
              taskResult.setErrorMessage(ExceptionUtils.getMessage(ex));
            }
            logger.warn("for {} error fetching Prometheus metrics for minute {} . retrying in {}s",
                dataCollectionInfo.getStateExecutionId(), dataCollectionMinute, RETRY_SLEEP, ex);
            sleep(RETRY_SLEEP);
          }
        }
      }

      if (completed.get()) {
        logger.info("Shutting down Prometheus data collection");
        shutDownCollection();
        return;
      }
    }

    public TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricsData() throws IOException {
      final TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricDataResponses = TreeBasedTable.create();

      List<Callable<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> callables = new ArrayList<>();
      long startTime = TimeUnit.MILLISECONDS.toSeconds(collectionStartTime);
      long endTime = startTime + TimeUnit.MINUTES.toSeconds(1);
      List<Optional<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> results;
      logger.info("fetching Prometheus metrics for {} Analysis Type {} for min {}",
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getTimeSeriesMlAnalysisType(),
          dataCollectionMinute);
      switch (dataCollectionInfo.getTimeSeriesMlAnalysisType()) {
        case COMPARATIVE:
          for (String host : dataCollectionInfo.getHosts().keySet()) {
            callables.add(() -> getMetricDataRecords(host, startTime, endTime));
          }
          break;
        case PREDICTIVE:
          final int periodToCollect = is247Task
              ? dataCollectionInfo.getCollectionTime()
              : (dataCollectionMinute == 0) ? PREDECTIVE_HISTORY_MINUTES + DURATION_TO_ASK_MINUTES
                                            : DURATION_TO_ASK_MINUTES;
          if (is247Task) {
            callables.add(
                () -> getMetricDataRecords(null, startTime, startTime + TimeUnit.MINUTES.toSeconds(periodToCollect)));
          } else {
            callables.add(
                () -> getMetricDataRecords(null, endTime - TimeUnit.MINUTES.toSeconds(periodToCollect), endTime));
          }
          break;
        default:
          throw new IllegalStateException("Invalid type " + dataCollectionInfo.getTimeSeriesMlAnalysisType());
      }
      results = executeParallel(callables);
      logger.info("done fetching Prometheus metrics for {} Analysis Type {} for min {}",
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getTimeSeriesMlAnalysisType(),
          dataCollectionMinute);
      results.forEach(result -> {
        if (result.isPresent()) {
          metricDataResponses.putAll(result.get());
        }
      });
      return metricDataResponses;
    }

    private TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricDataRecords(
        String host, long startTime, long endTime) {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> rv = TreeBasedTable.create();
      dataCollectionInfo.getTimeSeriesToCollect().forEach(timeSeries -> {
        String url = timeSeries.getUrl();
        Preconditions.checkState(url.contains(START_TIME_PLACE_HOLDER));
        Preconditions.checkState(url.contains(END_TIME_PLACE_HOLDER));

        url = url.replace(START_TIME_PLACE_HOLDER, String.valueOf(startTime));
        url = url.replace(END_TIME_PLACE_HOLDER, String.valueOf(endTime));

        if (!is247Task) {
          Preconditions.checkState(url.contains(HOST_NAME_PLACE_HOLDER));
          url = url.replace(HOST_NAME_PLACE_HOLDER, host);
        }
        ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
        try {
          PrometheusMetricDataResponse response =
              prometheusDelegateService.fetchMetricData(prometheusConfig, url, apiCallLog);
          TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricRecords =
              response.getMetricRecords(timeSeries.getTxnName(), timeSeries.getMetricName(),
                  dataCollectionInfo.getApplicationId(), dataCollectionInfo.getWorkflowId(),
                  dataCollectionInfo.getWorkflowExecutionId(), dataCollectionInfo.getStateExecutionId(),
                  dataCollectionInfo.getServiceId(), host, dataCollectionInfo.getHosts().get(host),
                  dataCollectionInfo.getStartTime(), dataCollectionInfo.getCvConfigId(), is247Task, url,
                  delegateCVActivityLogService.getLogger(getAccountId(), dataCollectionInfo.getCvConfigId(),
                      dataCollectionInfo.getDataCollectionMinute(), dataCollectionInfo.getStateExecutionId()));
          metricRecords.cellSet().forEach(cell -> {
            if (rv.contains(cell.getRowKey(), cell.getColumnKey())) {
              NewRelicMetricDataRecord metricDataRecord = rv.get(cell.getRowKey(), cell.getColumnKey());
              metricDataRecord.getValues().putAll(cell.getValue().getValues());
              metricDataRecord.getDeeplinkMetadata().putAll(cell.getValue().getDeeplinkMetadata());
            } else {
              rv.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
            }
          });
        } catch (IOException e) {
          apiCallLog.addFieldToResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), FieldType.TEXT);
          delegateLogService.save(prometheusConfig.getAccountId(), apiCallLog);
          throw new WingsException("Exception occured while fetching metrics from Prometheus.", e);
        }
      });
      rv.put(HARNESS_HEARTBEAT_METRIC_NAME, 0L,
          NewRelicMetricDataRecord.builder()
              .stateType(getStateType())
              .name(HARNESS_HEARTBEAT_METRIC_NAME)
              .appId(dataCollectionInfo.getApplicationId())
              .workflowId(dataCollectionInfo.getWorkflowId())
              .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
              .serviceId(dataCollectionInfo.getServiceId())
              .cvConfigId(dataCollectionInfo.getCvConfigId())
              .stateExecutionId(dataCollectionInfo.getStateExecutionId())
              .dataCollectionMinute(fetchCollectionMinute())
              .timeStamp(collectionStartTime)
              .groupName(dataCollectionInfo.getHosts().get(host))
              .level(ClusterLevel.H0)
              .build());
      return rv;
    }

    private int fetchCollectionMinute() {
      boolean isPredictiveAnalysis =
          dataCollectionInfo.getTimeSeriesMlAnalysisType() == TimeSeriesMlAnalysisType.PREDICTIVE;
      int collectionMinute;
      if (is247Task) {
        collectionMinute = (int) TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getStartTime())
            + dataCollectionInfo.getCollectionTime();
      } else if (isPredictiveAnalysis) {
        collectionMinute = dataCollectionMinute + PREDECTIVE_HISTORY_MINUTES + DURATION_TO_ASK_MINUTES;
      } else {
        collectionMinute = dataCollectionMinute;
      }
      return collectionMinute;
    }
  }
}

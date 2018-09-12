package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.sm.states.AbstractAnalysisState.END_TIME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.HOST_NAME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.START_TIME_PLACE_HOLDER;

import com.google.common.base.Preconditions;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.service.impl.prometheus.PrometheusMetricDataResponse;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

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
public class PrometheusDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(PrometheusDataCollectionTask.class);
  private static final int DURATION_TO_ASK_MINUTES = 5;
  private static final int CANARY_DAYS_TO_COLLECT = 7;
  private PrometheusDataCollectionInfo dataCollectionInfo;

  @Inject private PrometheusDelegateService prometheusDelegateService;
  @Inject private MetricDataStoreService metricStoreService;

  public PrometheusDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (PrometheusDataCollectionInfo) parameters[0];
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
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new PrometheusMetricCollector(dataCollectionInfo, taskResult);
  }

  private class PrometheusMetricCollector implements Runnable {
    private final PrometheusDataCollectionInfo dataCollectionInfo;
    private final DataCollectionTaskResult taskResult;
    private long collectionStartTime;
    private int dataCollectionMinute;

    private PrometheusMetricCollector(
        PrometheusDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
            TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricDataRecords = getMetricsData();

            List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(metricDataRecords);
            if (!saveMetrics(dataCollectionInfo.getPrometheusConfig().getAccountId(),
                    dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
              logger.error("Error saving metrics to the database. DatacollectionMin: {} StateexecutionId: {}",
                  dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
            } else {
              logger.info("Sent {} Prometheus metric records to the server for minute {}", recordsToSave.size(),
                  dataCollectionMinute);
            }
            dataCollectionMinute++;
            collectionStartTime += TimeUnit.MINUTES.toMillis(1);
            break;

          } catch (Exception ex) {
            if (++retry >= RETRIES) {
              taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
              completed.set(true);
              break;
            } else {
              if (retry == 1) {
                taskResult.setErrorMessage(Misc.getMessage(ex));
              }
              logger.warn("error fetching Prometheus metrics for minute " + dataCollectionMinute + ". retrying in "
                      + RETRY_SLEEP + "s",
                  ex);
              sleep(RETRY_SLEEP);
            }
          }
        }
      } catch (Exception e) {
        completed.set(true);
        taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage("error fetching Prometheus metrics for minute " + dataCollectionMinute);
        logger.error("error fetching Prometheus metrics for minute " + dataCollectionMinute, e);
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
      for (String host : dataCollectionInfo.getHosts().keySet()) {
        callables.add(() -> getMetricDataRecords(host));
      }

      logger.info("fetching Prometheus metrics for {} strategy {} for min {}", dataCollectionInfo.getStateExecutionId(),
          dataCollectionInfo.getAnalysisComparisonStrategy(), dataCollectionMinute);
      List<Optional<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> results = executeParrallel(callables);
      logger.info("done fetching Prometheus metrics for {} strategy {} for min {}",
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getAnalysisComparisonStrategy(),
          dataCollectionMinute);
      results.forEach(result -> {
        if (result.isPresent()) {
          metricDataResponses.putAll(result.get());
        }
      });
      return metricDataResponses;
    }

    private TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricDataRecords(String host) {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> rv = TreeBasedTable.create();
      dataCollectionInfo.getTimeSeriesToCollect().forEach(timeSeries -> {
        String url = timeSeries.getUrl();
        Preconditions.checkState(url.contains(START_TIME_PLACE_HOLDER));
        Preconditions.checkState(url.contains(END_TIME_PLACE_HOLDER));
        Preconditions.checkState(url.contains(HOST_NAME_PLACE_HOLDER));

        url = url.replace(HOST_NAME_PLACE_HOLDER, host);
        url = url.replace(START_TIME_PLACE_HOLDER, String.valueOf(collectionStartTime / TimeUnit.SECONDS.toMillis(1)));
        url = url.replace(
            END_TIME_PLACE_HOLDER, String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));

        try {
          ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
          PrometheusMetricDataResponse response =
              prometheusDelegateService.fetchMetricData(dataCollectionInfo.getPrometheusConfig(), url, apiCallLog);
          TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricRecords = response.getMetricRecords(
              timeSeries.getTxnName(), timeSeries.getMetricName(), dataCollectionInfo.getApplicationId(),
              dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
              dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getServiceId(), host,
              dataCollectionInfo.getHosts().get(host), dataCollectionInfo.getStartTime());
          metricRecords.cellSet().forEach(cell -> {
            if (rv.contains(cell.getRowKey(), cell.getColumnKey())) {
              NewRelicMetricDataRecord metricDataRecord = rv.get(cell.getRowKey(), cell.getColumnKey());
              metricDataRecord.getValues().putAll(cell.getValue().getValues());
            } else {
              rv.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
            }
          });
        } catch (IOException e) {
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
              .stateExecutionId(dataCollectionInfo.getStateExecutionId())
              .dataCollectionMinute(dataCollectionMinute)
              .timeStamp(collectionStartTime)
              .groupName(dataCollectionInfo.getHosts().get(host))
              .level(ClusterLevel.H0)
              .build());
      return rv;
    }

    private List<NewRelicMetricDataRecord> getAllMetricRecords(
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) {
      List<NewRelicMetricDataRecord> rv = new ArrayList<>();
      records.cellSet().forEach(cell -> rv.add(cell.getValue()));
      return rv;
    }
  }
}

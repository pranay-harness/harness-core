package software.wings.delegatetasks;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.newrelic.NewRelicApdex;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicErrors;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicMetricData;
import software.wings.service.impl.newrelic.NewRelicMetricData.NewRelicMetricSlice;
import software.wings.service.impl.newrelic.NewRelicMetricData.NewRelicMetricTimeSlice;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicWebTransactions;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.sm.StateType;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rsingh on 5/18/17.
 */
public class NewRelicDataCollectionTask extends AbstractDelegateDataCollectionTask {
  public static final String HARNESS_HEARTEAT_METRIC_NAME = "Harness heartbeat metric";
  private static final Logger logger = LoggerFactory.getLogger(NewRelicDataCollectionTask.class);
  private static final int DURATION_TO_ASK_MINUTES = 3;
  private static final int METRIC_DATA_QUERY_BATCH_SIZE = 50;
  private NewRelicDataCollectionInfo dataCollectionInfo;

  @Inject private NewRelicDelegateService newRelicDelegateService;

  @Inject private MetricDataStoreService metricStoreService;

  public NewRelicDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (NewRelicDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}" + dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.NEW_RELIC)
        .build();
  }

  @Override
  protected StateType getStateType() {
    return StateType.NEW_RELIC;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new NewRelicMetricCollector(dataCollectionInfo);
  }

  private class NewRelicMetricCollector implements Runnable {
    private final NewRelicDataCollectionInfo dataCollectionInfo;
    private final List<NewRelicApplicationInstance> instances;
    private long collectionStartTime;
    private final List<NewRelicMetric> metrics;
    private int dataCollectionMinute;

    private NewRelicMetricCollector(NewRelicDataCollectionInfo dataCollectionInfo) throws IOException {
      this.dataCollectionInfo = dataCollectionInfo;
      this.instances = newRelicDelegateService.getApplicationInstances(
          dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId());
      this.metrics = newRelicDelegateService.getMetricsNameToCollect(
          dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId());
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime())
          - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
    }

    @Override
    public void run() {
      try {
        final long endTime = collectionStartTime + TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
        for (NewRelicApplicationInstance node : instances) {
          TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

          // HeartBeat
          records.put(HARNESS_HEARTEAT_METRIC_NAME, 0l,
              NewRelicMetricDataRecord.builder()
                  .name(HARNESS_HEARTEAT_METRIC_NAME)
                  .applicationId(dataCollectionInfo.getApplicationId())
                  .workflowId(dataCollectionInfo.getWorkflowId())
                  .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                  .serviceId(dataCollectionInfo.getServiceId())
                  .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                  .dataCollectionMinute(dataCollectionMinute)
                  .host(node.getHost())
                  .level(ClusterLevel.H0)
                  .build());

          List<List<String>> metricBatches = batchMetricsToCollect();
          for (List<String> metricNames : metricBatches) {
            try {
              NewRelicMetricData metricData =
                  newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
                      dataCollectionInfo.getNewRelicAppId(), node.getId(), metricNames, collectionStartTime, endTime);

              for (NewRelicMetricSlice metric : metricData.getMetrics()) {
                for (NewRelicMetricTimeSlice timeSlice : metric.getTimeslices()) {
                  final NewRelicMetricDataRecord metricDataRecord = new NewRelicMetricDataRecord();
                  metricDataRecord.setName(metric.getName());
                  metricDataRecord.setApplicationId(dataCollectionInfo.getApplicationId());
                  metricDataRecord.setWorkflowId(dataCollectionInfo.getWorkflowId());
                  metricDataRecord.setWorkflowExecutionId(dataCollectionInfo.getWorkflowExecutionId());
                  metricDataRecord.setServiceId(dataCollectionInfo.getServiceId());
                  metricDataRecord.setStateExecutionId(dataCollectionInfo.getStateExecutionId());
                  metricDataRecord.setDataCollectionMinute(dataCollectionMinute);

                  // set from time to the timestamp
                  long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeSlice.getFrom()).toEpochSecond());
                  metricDataRecord.setTimeStamp(timeStamp);
                  metricDataRecord.setHost(node.getHost());

                  final String webTxnJson = JsonUtils.asJson(timeSlice.getValues());
                  NewRelicWebTransactions webTransactions =
                      JsonUtils.asObject(webTxnJson, NewRelicWebTransactions.class);
                  metricDataRecord.setThroughput(webTransactions.getThroughput());
                  metricDataRecord.setAverageResponseTime(webTransactions.getAverage_response_time());
                  records.put(metric.getName(), timeStamp, metricDataRecord);
                }
              }
            } catch (Exception e) {
              logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
            }

            // get error metrics
            try {
              NewRelicMetricData metricData = newRelicDelegateService.getMetricData(
                  dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
                  getErrorMetricNames(metricNames), collectionStartTime, endTime);
              for (NewRelicMetricSlice metric : metricData.getMetrics()) {
                for (NewRelicMetricTimeSlice timeslice : metric.getTimeslices()) {
                  long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeslice.getFrom()).toEpochSecond());
                  String metricName = metric.getName().replace("Errors/", "");

                  NewRelicMetricDataRecord metricDataRecord = records.get(metricName, timeStamp);
                  if (metricDataRecord != null) {
                    final String errorsJson = JsonUtils.asJson(timeslice.getValues());
                    NewRelicErrors errors = JsonUtils.asObject(errorsJson, NewRelicErrors.class);
                    metricDataRecord.setError(errors.getError_count());
                  }
                }
              }

            } catch (Exception e) {
              logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
            }

            // get apdex metrics
            try {
              NewRelicMetricData metricData = newRelicDelegateService.getMetricData(
                  dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
                  getApdexMetricNames(metricNames), collectionStartTime, endTime);
              for (NewRelicMetricSlice metric : metricData.getMetrics()) {
                for (NewRelicMetricTimeSlice timeslice : metric.getTimeslices()) {
                  long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeslice.getFrom()).toEpochSecond());
                  String metricName = metric.getName().replace("Apdex", "WebTransaction");

                  NewRelicMetricDataRecord metricDataRecord = records.get(metricName, timeStamp);
                  if (metricDataRecord != null) {
                    final String apdexJson = JsonUtils.asJson(timeslice.getValues());
                    NewRelicApdex apdex = JsonUtils.asObject(apdexJson, NewRelicApdex.class);
                    metricDataRecord.setApdexScore(apdex.getScore());
                  }
                }
              }

            } catch (Exception e) {
              logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
            }
            logger.debug(records.toString());
            metricStoreService.saveNewRelicMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(),
                dataCollectionInfo.getApplicationId(), getAllMetricRecords(records));
            logger.info("Sending " + records.cellSet().size() + " new relic metric records to the server for minute "
                + dataCollectionMinute);
            records.clear();
          }
        }
        dataCollectionMinute++;
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Exception e) {
        logger.error("error fetching new relic metrics for minute " + dataCollectionMinute, e);
      }

      if (completed.get()) {
        logger.info("Shutting down new relic data collection");
        shutDownCollection();
        return;
      }
    }

    private List<String> getApdexMetricNames(List<String> metricNames) {
      final List<String> rv = new ArrayList<>();
      for (String metricName : metricNames) {
        rv.add(metricName.replace("WebTransaction", "Apdex"));
      }

      return rv;
    }

    private List<String> getErrorMetricNames(List<String> metricNames) {
      final List<String> rv = new ArrayList<>();
      for (String metricName : metricNames) {
        rv.add("Errors/" + metricName);
      }

      return rv;
    }

    private List<List<String>> batchMetricsToCollect() {
      List<List<String>> rv = new ArrayList<>();

      List<String> batchedMetrics = new ArrayList<>();
      for (NewRelicMetric metric : metrics) {
        batchedMetrics.add(metric.getName());

        if (batchedMetrics.size() == METRIC_DATA_QUERY_BATCH_SIZE) {
          rv.add(batchedMetrics);
          batchedMetrics = new ArrayList<>();
        }
      }

      if (!batchedMetrics.isEmpty()) {
        rv.add(batchedMetrics);
      }

      return rv;
    }

    private List<NewRelicMetricDataRecord> getAllMetricRecords(
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) {
      List<NewRelicMetricDataRecord> rv = new ArrayList<>();
      for (Cell<String, Long, NewRelicMetricDataRecord> cell : records.cellSet()) {
        rv.add(cell.getValue());
      }

      return rv;
    }
  }
}

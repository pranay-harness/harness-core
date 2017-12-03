package software.wings.delegatetasks;

import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP_SECS;

import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
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
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rsingh on 5/18/17.
 */
public class NewRelicDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicDataCollectionTask.class);
  private static final int DURATION_TO_ASK_MINUTES = 3;
  private static final int METRIC_DATA_QUERY_BATCH_SIZE = 50;
  private NewRelicDataCollectionInfo dataCollectionInfo;

  @Inject private NewRelicDelegateService newRelicDelegateService;

  @Inject private MetricDataStoreService metricStoreService;

  public NewRelicDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
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
    return new NewRelicMetricCollector(dataCollectionInfo, taskResult);
  }

  private class NewRelicMetricCollector implements Runnable {
    private final NewRelicDataCollectionInfo dataCollectionInfo;
    private final List<NewRelicApplicationInstance> instances;
    private long collectionStartTime;
    private Collection<NewRelicMetric> metrics;
    private int dataCollectionMinute;
    private DataCollectionTaskResult taskResult;

    private NewRelicMetricCollector(NewRelicDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult)
        throws IOException {
      this.dataCollectionInfo = dataCollectionInfo;
      this.instances = newRelicDelegateService.getApplicationInstances(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId());
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime())
          - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      this.taskResult = taskResult;
    }

    private TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricData(
        NewRelicApplicationInstance node, Collection<String> metricNames, long endTime) throws Exception {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

      try {
        NewRelicMetricData metricData = newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
            dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
            metricNames, collectionStartTime, endTime);

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
            metricDataRecord.setStateType(getStateType());

            // set from time to the timestamp
            long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeSlice.getFrom()).toEpochSecond());
            metricDataRecord.setTimeStamp(timeStamp);
            metricDataRecord.setHost(node.getHost());

            final String webTxnJson = JsonUtils.asJson(timeSlice.getValues());
            NewRelicWebTransactions webTransactions = JsonUtils.asObject(webTxnJson, NewRelicWebTransactions.class);
            if (webTransactions.getCall_count() > 0) {
              metricDataRecord.setThroughput(webTransactions.getThroughput());
              metricDataRecord.setAverageResponseTime(webTransactions.getAverage_response_time());
              metricDataRecord.setCallCount(webTransactions.getCall_count());
              metricDataRecord.setRequestsPerMinute(webTransactions.getRequests_per_minute());
              records.put(metric.getName(), timeStamp, metricDataRecord);
            }
          }
        }
      } catch (Exception e) {
        logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
        throw(e);
      }

      // get error metrics
      try {
        NewRelicMetricData metricData = newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
            dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
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
        throw(e);
      }

      // get apdex metrics
      try {
        NewRelicMetricData metricData = newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
            dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
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
        throw(e);
      }
      logger.debug(records.toString());
      return records;
    }

    @Override
    public void run() {
      try {
        int retry = RETRIES;
        while (!completed.get() && retry > 0) {
          try {
            if (metrics == null || metrics.isEmpty() || dataCollectionMinute % DURATION_TO_ASK_MINUTES == 0) {
              metrics = newRelicDelegateService.getMetricsNameToCollect(dataCollectionInfo.getNewRelicConfig(),
                  dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId());
              metrics = getMetricsWithDataIn24Hrs();
            }
            final long endTime = collectionStartTime + TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);

            TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

            for (NewRelicApplicationInstance node : instances) {
              if (!dataCollectionInfo.getHosts().contains(node.getHost())) {
                logger.info("Skipping host {} for stateExecutionId {} ", node.getHost(),
                    dataCollectionInfo.getStateExecutionId());
                continue;
              }

              List<Collection<String>> metricBatches = batchMetricsToCollect();

              for (Collection<String> metricNames : metricBatches) {
                records.putAll(getMetricData(node, metricNames, endTime));
              }

              List<NewRelicMetricDataRecord> metricRecords = getAllMetricRecords(records);
              if (!saveMetrics(metricRecords)) {
                retry = 0;
                throw new RuntimeException("Cannot save new relic metric records. Server returned error");
              }
              logger.info("Sending " + records.cellSet().size() + " new relic metric records to the server for minute "
                  + dataCollectionMinute);
              records.clear();
            }

            // HeartBeat
            records.put(HARNESS_HEARTBEAT_METRIC_NAME, 0l,
                NewRelicMetricDataRecord.builder()
                    .stateType(getStateType())
                    .name(HARNESS_HEARTBEAT_METRIC_NAME)
                    .applicationId(dataCollectionInfo.getApplicationId())
                    .workflowId(dataCollectionInfo.getWorkflowId())
                    .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                    .serviceId(dataCollectionInfo.getServiceId())
                    .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                    .dataCollectionMinute(dataCollectionMinute)
                    .timeStamp(collectionStartTime)
                    .level(ClusterLevel.H0)
                    .build());
            logger.info("Sending heartbeat new relic metric record to the server for minute " + dataCollectionMinute);

            metricStoreService.saveNewRelicMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(),
                dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), getTaskId(),
                getAllMetricRecords(records));
            records.clear();

            dataCollectionMinute++;
            collectionStartTime += TimeUnit.MINUTES.toMillis(1);
            dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
            break;
          } catch (Exception ex) {
            if (retry == 0) {
              taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
              completed.set(true);
              break;
            } else {
              /*
               * Save the exception from the first attempt. This is usually
               * more meaningful to trouble shoot.
               */
              if (retry == RETRIES) {
                taskResult.setErrorMessage(ex.getMessage());
              }
              --retry;
              logger.warn("error fetching new relic metrics for minute " + dataCollectionMinute + ". retrying in "
                      + RETRY_SLEEP_SECS + "s",
                  ex);
              Thread.sleep(TimeUnit.SECONDS.toMillis(RETRY_SLEEP_SECS));
            }
          }
        }
      } catch (Exception e) {
        completed.set(true);
        taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage("error fetching new relic metrics for minute " + dataCollectionMinute);
        logger.error("error fetching new relic metrics for minute " + dataCollectionMinute, e);
      }

      if (completed.get()) {
        logger.info("Shutting down new relic data collection");
        shutDownCollection();
        return;
      }
    }

    private boolean saveMetrics(List<NewRelicMetricDataRecord> records) {
      int retrySave = 0;
      do {
        boolean response = metricStoreService.saveNewRelicMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(),
            dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), getTaskId(), records);
        if (response) {
          return true;
        }
        Misc.sleep(RETRY_SLEEP_SECS, TimeUnit.SECONDS);
      } while (++retrySave != RETRIES);
      return false;
    }

    private Collection<NewRelicMetric> getMetricsWithDataIn24Hrs() throws IOException {
      Map<String, NewRelicMetric> webTransactionMetrics = new HashMap<>();
      for (NewRelicMetric metric : metrics) {
        webTransactionMetrics.put(metric.getName(), metric);
      }
      List<Collection<String>> metricBatches = batchMetricsToCollect();
      final long currentTime = System.currentTimeMillis();
      for (Collection<String> metricNames : metricBatches) {
        Set<String> metricsWithNoData = Sets.newHashSet(metricNames);
        for (NewRelicApplicationInstance node : instances) {
          // find and remove metrics which have no data in last 24 hours
          NewRelicMetricData metricData = newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
              dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
              metricNames, currentTime - TimeUnit.DAYS.toMillis(1), currentTime);
          metricsWithNoData.removeAll(metricData.getMetrics_found());

          if (metricsWithNoData.isEmpty()) {
            break;
          }
        }
        for (String metricName : metricsWithNoData) {
          webTransactionMetrics.remove(metricName);
        }
      }
      return webTransactionMetrics.values();
    }

    private Collection<String> getApdexMetricNames(Collection<String> metricNames) {
      final Collection<String> rv = new ArrayList<>();
      for (String metricName : metricNames) {
        rv.add(metricName.replace("WebTransaction", "Apdex"));
      }

      return rv;
    }

    private Collection<String> getErrorMetricNames(Collection<String> metricNames) {
      final Collection<String> rv = new ArrayList<>();
      for (String metricName : metricNames) {
        rv.add("Errors/" + metricName);
      }

      return rv;
    }

    private List<Collection<String>> batchMetricsToCollect() {
      List<Collection<String>> rv = new ArrayList<>();

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
        NewRelicMetricDataRecord value = cell.getValue();
        value.setName(value.getName().replace("WebTransaction/", ""));
        rv.add(value);
      }

      return rv;
    }
  }
}

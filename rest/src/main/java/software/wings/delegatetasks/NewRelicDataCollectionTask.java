package software.wings.delegatetasks;

import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP_SECS;

import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

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
import software.wings.service.impl.newrelic.NewRelicMetricNames;
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

/**
 * Created by rsingh on 5/18/17.
 */
public class NewRelicDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicDataCollectionTask.class);
  private static final int INITIAL_DELAY_MINUTES = 0;
  private static final int PERIOD_MINS = 1;
  private static final int METRIC_DATA_QUERY_BATCH_SIZE = 50;
  private static final int APM_COLLECTION_BUFFER = 2;
  private static final int COLLECTION_PERIOD_MINS = 5;

  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private MetricDataStoreService metricStoreService;
  private NewRelicDataCollectionInfo dataCollectionInfo;
  private NewRelicMetricNameCollectionTask metricNameCollectionTask;

  public NewRelicDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
    metricNameCollectionTask = new NewRelicMetricNameCollectionTask(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (NewRelicDataCollectionInfo) parameters[0];
    metricNameCollectionTask.setService(newRelicDelegateService, metricStoreService);
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

  @Override
  protected int getInitialDelayMinutes() {
    return Math.min(dataCollectionInfo.getCollectionTime(), INITIAL_DELAY_MINUTES);
  }

  @Override
  protected int getPeriodMinutes() {
    return PERIOD_MINS;
  }

  private int getDurationToAskMinutes() {
    return COLLECTION_PERIOD_MINS - APM_COLLECTION_BUFFER;
  }

  private class NewRelicMetricCollector implements Runnable {
    private final NewRelicDataCollectionInfo dataCollectionInfo;
    private final List<NewRelicApplicationInstance> instances;
    private long windowStartTimeManager;
    private Collection<NewRelicMetric> metrics;
    private int dataCollectionMinute;
    private DataCollectionTaskResult taskResult;
    private long lastCollectionTime;
    private long analysisStartTimeDelegate;
    private long managerAnalysisStartTime;

    private NewRelicMetricCollector(NewRelicDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult)
        throws IOException {
      this.dataCollectionInfo = dataCollectionInfo;
      this.instances = newRelicDelegateService.getApplicationInstances(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId());
      this.managerAnalysisStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime());
      this.windowStartTimeManager = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime());
      this.analysisStartTimeDelegate = System.currentTimeMillis();
      this.lastCollectionTime = analysisStartTimeDelegate;
      this.dataCollectionMinute = 0;
      this.taskResult = taskResult;

      logger.info("NewRelic collector initialized : managerAnalysisStartTime - {}, windowStartTimeManager {}",
          managerAnalysisStartTime, windowStartTimeManager);
    }

    private TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricData(
        NewRelicApplicationInstance node, Collection<String> metricNames, long endTime) throws Exception {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();
      try {
        NewRelicMetricData metricData = newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
            dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
            metricNames, windowStartTimeManager, endTime);

        for (NewRelicMetricSlice metric : metricData.getMetrics()) {
          for (NewRelicMetricTimeSlice timeSlice : metric.getTimeslices()) {
            // set from time to the timestamp
            long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeSlice.getFrom()).toEpochSecond());
            if (timeStamp < managerAnalysisStartTime) {
              logger.debug("New relic sending us data in the past. request start time {}, received time {}",
                  managerAnalysisStartTime, timeStamp);
            }
            final NewRelicMetricDataRecord metricDataRecord = new NewRelicMetricDataRecord();
            metricDataRecord.setName(metric.getName());
            metricDataRecord.setApplicationId(dataCollectionInfo.getApplicationId());
            metricDataRecord.setWorkflowId(dataCollectionInfo.getWorkflowId());
            metricDataRecord.setWorkflowExecutionId(dataCollectionInfo.getWorkflowExecutionId());
            metricDataRecord.setServiceId(dataCollectionInfo.getServiceId());
            metricDataRecord.setStateExecutionId(dataCollectionInfo.getStateExecutionId());
            metricDataRecord.setStateType(getStateType());

            metricDataRecord.setTimeStamp(timeStamp);
            metricDataRecord.setHost(node.getHost());

            metricDataRecord.setDataCollectionMinute(
                (int) ((timeStamp - managerAnalysisStartTime) / TimeUnit.MINUTES.toMillis(1)));

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
        logger.info("Step 1: Get NewRelic records finished");

      } catch (Exception e) {
        logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
        throw(e);
      }

      // get error metrics
      try {
        NewRelicMetricData metricData = newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
            dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
            getErrorMetricNames(metricNames), windowStartTimeManager, endTime);
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

        logger.info("Step 2: Get NewRelic error records finished");

      } catch (Exception e) {
        logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
        throw(e);
      }

      // get apdex metrics
      try {
        NewRelicMetricData metricData = newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
            dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
            getApdexMetricNames(metricNames), windowStartTimeManager, endTime);
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

        logger.info("Step 3: Get NewRelic apdex records finished");

      } catch (Exception e) {
        logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
        throw(e);
      }
      logger.debug(records.toString());
      return records;
    }

    private void getMetricsSet() {
      if (metrics == null || metrics.isEmpty()) {
        logger.info("fetching new relic metric names from harness manager");
        NewRelicMetricNames newRelicMetricNames = metricStoreService.getNewRelicMetricNames(getAccountId(),
            String.valueOf(dataCollectionInfo.getNewRelicAppId()), dataCollectionInfo.getSettingAttributeId());
        if (newRelicMetricNames != null && newRelicMetricNames.getMetrics() != null
            && newRelicMetricNames.getMetrics().size() > 0) {
          logger.info("found new relic metric names {} from harness manager", newRelicMetricNames.getMetrics().size());
          metrics = newRelicMetricNames.getMetrics();
        } else {
          logger.info("fetching new relic metrics with data in the last 24 hours");
          metricNameCollectionTask.run(new Object[] {dataCollectionInfo});
          metrics = metricNameCollectionTask.getMetrics();
          logger.info("total available metrics for new relic " + metrics.size());
        }
      }
    }

    private int timeDeltaInMins(long t2Millis, long t1Millis) {
      return (int) ((t2Millis - t1Millis) / TimeUnit.MINUTES.toMillis(1));
    }

    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          logger.info("Running new relic data collection");
          try {
            getMetricsSet();

            if (metrics != null) {
              logger.info("Found total new relic metrics " + metrics.size());
            } else {
              logger.info("Found 0 total new relic metrics ");
              return;
            }

            int totalAnalysisTime = timeDeltaInMins(System.currentTimeMillis(), analysisStartTimeDelegate);
            int elapsedTime = timeDeltaInMins(System.currentTimeMillis(), lastCollectionTime);

            if (totalAnalysisTime < dataCollectionInfo.getCollectionTime() && elapsedTime < COLLECTION_PERIOD_MINS) {
              logger.info("elapsed time {} below collection threshold {} . skipping collection", elapsedTime,
                  COLLECTION_PERIOD_MINS);
              return;
            }

            lastCollectionTime = System.currentTimeMillis();

            final long windowEndTimeManager = windowStartTimeManager + TimeUnit.MINUTES.toMillis(totalAnalysisTime)
                - TimeUnit.MINUTES.toMillis(dataCollectionMinute) - TimeUnit.MINUTES.toMillis(APM_COLLECTION_BUFFER);
            final int collectionLength = timeDeltaInMins(windowEndTimeManager, windowStartTimeManager);
            int dataCollectionMinuteEnd = dataCollectionMinute + collectionLength - 1;

            TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

            List<Collection<String>> metricBatches = batchMetricsToCollect();
            logger.info("Found total new relic metric batches " + metricBatches.size());

            for (NewRelicApplicationInstance node : instances) {
              if (!dataCollectionInfo.getHosts().contains(node.getHost())) {
                logger.info("Skipping host {} for stateExecutionId {} ", node.getHost(),
                    dataCollectionInfo.getStateExecutionId());
                continue;
              }

              for (Collection<String> metricNames : metricBatches) {
                records.putAll(getMetricData(node, metricNames, windowEndTimeManager));
              }

              logger.info("Got NewRelic metric records {}", records.size());
              List<NewRelicMetricDataRecord> metricRecords = getAllMetricRecords(records);
              logger.info("Saving {} NewRelic metric records to harness manager", metricRecords.size());
              if (!saveMetrics(metricRecords)) {
                retry = RETRIES;
                taskResult.setErrorMessage("Cannot save new relic metric records to Harness. Server returned error");
                throw new RuntimeException("Cannot save new relic metric records to Harness. Server returned error");
              }
              logger.info("Sending " + records.cellSet().size() + " new relic metric records for " + node.getHost()
                  + " to the server for : from minute " + dataCollectionMinute + ", to minute "
                  + dataCollectionMinuteEnd);
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
                    .dataCollectionMinute(dataCollectionMinuteEnd)
                    .timeStamp(windowStartTimeManager)
                    .level(ClusterLevel.H0)
                    .build());
            logger.info(
                "Sending heartbeat new relic metric record to the server for minute " + dataCollectionMinuteEnd);

            metricStoreService.saveNewRelicMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(),
                dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), getTaskId(),
                getAllMetricRecords(records));
            records.clear();
            windowStartTimeManager = windowEndTimeManager;

            dataCollectionMinute = dataCollectionMinuteEnd + 1;
            // dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
            break;
          } catch (Exception ex) {
            if (++retry >= RETRIES) {
              taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
              completed.set(true);
              break;
            } else {
              /*
               * Save the exception from the first attempt. This is usually
               * more meaningful to trouble shoot.
               */
              if (retry == 1) {
                if (ex instanceof WingsException) {
                  if (((WingsException) ex).getParams().containsKey("reason")) {
                    taskResult.setErrorMessage((String) ((WingsException) ex).getParams().get("reason"));
                  } else {
                    taskResult.setErrorMessage(ex.getMessage());
                  }
                } else {
                  taskResult.setErrorMessage(ex.getMessage());
                };
              }
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

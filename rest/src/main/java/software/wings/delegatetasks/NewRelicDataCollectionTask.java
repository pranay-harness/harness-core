package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.APDEX_SCORE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.CALL_COUNT;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.ERROR;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.REQUSET_PER_MINUTE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.THROUGHPUT;

import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.time.Timestamp;
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
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 5/18/17.
 */
public class NewRelicDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicDataCollectionTask.class);
  private static final String ALL_WEB_TXN_NAME = "WebTransaction/all";
  private static final String ALL_ERRORS_TXN_NAME = "Errors/all";
  private static final String OVERALL_APDEX_TXN_NAME = "Apdex";
  private static final int INITIAL_DELAY_MINUTES = 0;
  private static final int PERIOD_MINS = 1;
  private static final int METRIC_DATA_QUERY_BATCH_SIZE = 50;
  private static final int APM_COLLECTION_BUFFER = 2;
  public static final int COLLECTION_PERIOD_MINS = 5;
  private static final int MIN_RPM = 1;

  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private MetricDataStoreService metricStoreService;
  private NewRelicDataCollectionInfo dataCollectionInfo;

  public NewRelicDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (NewRelicDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
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

  private class NewRelicMetricCollector implements Runnable {
    private final NewRelicDataCollectionInfo dataCollectionInfo;
    private long windowStartTimeManager;
    private int dataCollectionMinute;
    private DataCollectionTaskResult taskResult;
    private final Set<NewRelicMetric> allTxns;
    private long lastCollectionTime;
    private long analysisStartTimeDelegate;
    private long managerAnalysisStartTime;

    private NewRelicMetricCollector(NewRelicDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult)
        throws IOException {
      this.dataCollectionInfo = dataCollectionInfo;
      this.managerAnalysisStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.windowStartTimeManager = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.analysisStartTimeDelegate = System.currentTimeMillis();
      this.lastCollectionTime = analysisStartTimeDelegate;
      this.dataCollectionMinute = 0;
      this.taskResult = taskResult;
      this.allTxns = newRelicDelegateService.getTxnNameToCollect(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(),
          createApiCallLog(dataCollectionInfo.getStateExecutionId()));

      logger.info("NewRelic collector initialized : managerAnalysisStartTime - {}, windowStartTimeManager {}",
          managerAnalysisStartTime, windowStartTimeManager);
    }

    private TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricData(
        NewRelicApplicationInstance node, Set<String> metricNames, long endTime) throws Exception {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

      logger.info("Fetching for host {} for stateExecutionId {} for metrics {}", node.getHost(),
          dataCollectionInfo.getStateExecutionId(), metricNames);
      getWebTransactionMetrics(node, metricNames, endTime, records);
      getErrorMetrics(node, metricNames, endTime, records);
      getApdexMetrics(node, metricNames, endTime, records);
      logger.info("Fetching done for host {} for stateExecutionId {} for metrics {}", node.getHost(),
          dataCollectionInfo.getStateExecutionId(), metricNames);

      logger.debug(records.toString());
      return records;
    }

    private void getWebTransactionMetrics(NewRelicApplicationInstance node, Set<String> metricNames, long endTime,
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) throws IOException {
      int retry = 0;
      while (retry < RETRIES) {
        try {
          NewRelicMetricData metricData = newRelicDelegateService.getMetricDataApplicationInstance(
              dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getEncryptedDataDetails(),
              dataCollectionInfo.getNewRelicAppId(), node.getId(), metricNames, windowStartTimeManager, endTime,
              createApiCallLog(dataCollectionInfo.getStateExecutionId()));

          for (NewRelicMetricSlice metric : metricData.getMetrics()) {
            for (NewRelicMetricTimeSlice timeSlice : metric.getTimeslices()) {
              // set from time to the timestamp
              long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeSlice.getFrom()).toEpochSecond());
              if (timeStamp < managerAnalysisStartTime) {
                logger.debug("New relic sending us data in the past. request start time {}, received time {}",
                    managerAnalysisStartTime, timeStamp);
                continue;
              }
              final NewRelicMetricDataRecord metricDataRecord =
                  NewRelicMetricDataRecord.builder()
                      .name(metric.getName())
                      .appId(dataCollectionInfo.getApplicationId())
                      .workflowId(dataCollectionInfo.getWorkflowId())
                      .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                      .serviceId(dataCollectionInfo.getServiceId())
                      .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                      .stateType(getStateType())
                      .timeStamp(timeStamp)
                      .host(node.getHost())
                      .values(new HashMap<>())
                      .groupName(DEFAULT_GROUP_NAME)
                      .build();

              metricDataRecord.setDataCollectionMinute(
                  (int) ((timeStamp - managerAnalysisStartTime) / TimeUnit.MINUTES.toMillis(1)));

              final String webTxnJson = JsonUtils.asJson(timeSlice.getValues());
              NewRelicWebTransactions webTransactions = JsonUtils.asObject(webTxnJson, NewRelicWebTransactions.class);
              if (webTransactions.getCall_count() > 0) {
                metricDataRecord.getValues().put(THROUGHPUT, webTransactions.getThroughput());
                metricDataRecord.getValues().put(AVERAGE_RESPONSE_TIME, webTransactions.getAverage_response_time());
                metricDataRecord.getValues().put(CALL_COUNT, (double) webTransactions.getCall_count());
                metricDataRecord.getValues().put(REQUSET_PER_MINUTE, (double) webTransactions.getRequests_per_minute());
                records.put(metric.getName(), timeStamp, metricDataRecord);
              }
            }
          }
          break;
        } catch (Exception e) {
          logger.warn(
              "Error fetching metrics for node: " + node + ", retry: " + retry + ", metrics: " + metricNames, e);
          retry++;
          if (retry >= RETRIES) {
            throw new WingsException("Fetching for web transaction metrics failed. reason " + Misc.getMessage(e));
          }
        }
      }
    }

    private void getErrorMetrics(NewRelicApplicationInstance node, Set<String> metricNames, long endTime,
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) throws IOException {
      // get error metrics
      int retry = 0;
      while (retry < RETRIES) {
        try {
          NewRelicMetricData metricData = newRelicDelegateService.getMetricDataApplicationInstance(
              dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getEncryptedDataDetails(),
              dataCollectionInfo.getNewRelicAppId(), node.getId(), getErrorMetricNames(metricNames),
              windowStartTimeManager, endTime, createApiCallLog(dataCollectionInfo.getStateExecutionId()));
          for (NewRelicMetricSlice metric : metricData.getMetrics()) {
            for (NewRelicMetricTimeSlice timeslice : metric.getTimeslices()) {
              long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeslice.getFrom()).toEpochSecond());
              String metricName = metric.getName().equals(ALL_ERRORS_TXN_NAME)
                  ? metric.getName()
                  : metric.getName().replace("Errors/", "");

              NewRelicMetricDataRecord metricDataRecord = records.get(metricName, timeStamp);
              if (metricDataRecord != null) {
                final String errorsJson = JsonUtils.asJson(timeslice.getValues());
                NewRelicErrors errors = JsonUtils.asObject(errorsJson, NewRelicErrors.class);
                metricDataRecord.getValues().put(ERROR, (double) errors.getError_count());
              }
            }
          }

          break;
        } catch (Exception e) {
          logger.warn(
              "Error fetching metrics for node: " + node + ", retry: " + retry + ", metrics: " + metricNames, e);
          retry++;
          if (retry >= RETRIES) {
            throw new WingsException("Fetching for error metrics failed. reason " + Misc.getMessage(e));
          }
        }
      }
    }

    private void getApdexMetrics(NewRelicApplicationInstance node, Set<String> metricNames, long endTime,
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) throws IOException {
      // get apdex metrics
      int retry = 0;
      while (retry < RETRIES) {
        try {
          NewRelicMetricData metricData = newRelicDelegateService.getMetricDataApplicationInstance(
              dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getEncryptedDataDetails(),
              dataCollectionInfo.getNewRelicAppId(), node.getId(), getApdexMetricNames(metricNames),
              windowStartTimeManager, endTime, createApiCallLog(dataCollectionInfo.getStateExecutionId()));
          for (NewRelicMetricSlice metric : metricData.getMetrics()) {
            for (NewRelicMetricTimeSlice timeslice : metric.getTimeslices()) {
              long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeslice.getFrom()).toEpochSecond());
              String metricName = metric.getName().equals(OVERALL_APDEX_TXN_NAME)
                  ? metric.getName()
                  : metric.getName().replace("Apdex", "WebTransaction");

              NewRelicMetricDataRecord metricDataRecord = records.get(metricName, timeStamp);
              if (metricDataRecord != null) {
                final String apdexJson = JsonUtils.asJson(timeslice.getValues());
                NewRelicApdex apdex = JsonUtils.asObject(apdexJson, NewRelicApdex.class);
                metricDataRecord.getValues().put(APDEX_SCORE, apdex.getScore());
              }
            }
          }

          break;
        } catch (Exception e) {
          logger.warn(
              "Error fetching metrics for node: " + node + ", retry: " + retry + ", metrics: " + metricNames, e);
          retry++;
          if (retry >= RETRIES) {
            throw new WingsException("Fetching for apdex metrics failed. reason " + Misc.getMessage(e));
          }
        }
      }
    }

    private Set<NewRelicMetric> getTxnsToCollect() throws IOException {
      logger.info("Collecting txn names for {}", dataCollectionInfo);
      logger.info("all txns far {}", allTxns.size());
      Set<NewRelicMetric> newTxns = newRelicDelegateService.getTxnNameToCollect(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(),
          createApiCallLog(dataCollectionInfo.getStateExecutionId()));
      newTxns.removeAll(allTxns);
      logger.info("new txns {}", newTxns.size());
      Set<NewRelicMetric> txnsWithData = getTxnsWithDataInLastHour(allTxns);
      logger.info("txns with data {}", txnsWithData.size());
      txnsWithData.addAll(newTxns);
      logger.info("txns to collect {}", txnsWithData.size());
      return txnsWithData;
    }

    private int timeDeltaInMins(long t2Millis, long t1Millis) {
      return (int) ((t2Millis - t1Millis) / TimeUnit.MINUTES.toMillis(1));
    }

    @SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "REC_CATCH_EXCEPTION"})
    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          logger.info("Running new relic data collection");
          long startTime = System.currentTimeMillis();
          try {
            int totalAnalysisTime = timeDeltaInMins(System.currentTimeMillis(), analysisStartTimeDelegate);
            int elapsedTime = timeDeltaInMins(System.currentTimeMillis(), lastCollectionTime);

            if (totalAnalysisTime < dataCollectionInfo.getCollectionTime() && elapsedTime < COLLECTION_PERIOD_MINS) {
              logger.info("elapsed time {} below collection threshold {} . skipping collection", elapsedTime,
                  COLLECTION_PERIOD_MINS);
              return;
            }
            Set<NewRelicMetric> txnsToCollect = getTxnsToCollect();
            if (txnsToCollect != null) {
              logger.info("Found total new relic metrics " + txnsToCollect.size());
            } else {
              logger.info("Found 0 total new relic metrics ");
              return;
            }
            List<NewRelicApplicationInstance> instances = newRelicDelegateService.getApplicationInstances(
                dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getEncryptedDataDetails(),
                dataCollectionInfo.getNewRelicAppId(), createApiCallLog(dataCollectionInfo.getStateExecutionId()));
            logger.info("Got {} new relic nodes.", instances.size());

            lastCollectionTime = System.currentTimeMillis();

            final long windowEndTimeManager = windowStartTimeManager + TimeUnit.MINUTES.toMillis(totalAnalysisTime)
                - TimeUnit.MINUTES.toMillis(dataCollectionMinute) - TimeUnit.MINUTES.toMillis(APM_COLLECTION_BUFFER);
            final int collectionLength = timeDeltaInMins(windowEndTimeManager, windowStartTimeManager);
            int dataCollectionMinuteEnd = dataCollectionMinute + collectionLength - 1;

            List<Set<String>> metricBatches = batchMetricsToCollect(txnsToCollect);
            logger.info("Found total new relic metric batches " + metricBatches.size());

            List<Callable<Boolean>> callables = new ArrayList<>();
            for (NewRelicApplicationInstance node : instances) {
              if (!dataCollectionInfo.getHosts().contains(node.getHost())) {
                logger.info("Skipping host {} for stateExecutionId {} ", node.getHost(),
                    dataCollectionInfo.getStateExecutionId());
                continue;
              }

              logger.info("Going to collect for host {} for stateExecutionId {}, for metrics {}", node.getHost(),
                  dataCollectionInfo.getStateExecutionId(), metricBatches);
              callables.add(() -> fetchAndSaveMetricsForNode(node, metricBatches, windowEndTimeManager));
            }

            logger.info("submitting parallel tasks {}", callables.size());
            List<Optional<Boolean>> results = executeParrallel(callables);
            for (Optional<Boolean> result : results) {
              if (!result.isPresent() || !result.get()) {
                throw new WingsException("Cannot save new relic metric records. Server returned error");
              }
            }

            logger.info("done processing parallel tasks {}", callables.size());
            TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();
            // HeartBeat
            records.put(HARNESS_HEARTBEAT_METRIC_NAME, 0l,
                NewRelicMetricDataRecord.builder()
                    .stateType(getStateType())
                    .name(HARNESS_HEARTBEAT_METRIC_NAME)
                    .appId(dataCollectionInfo.getApplicationId())
                    .workflowId(dataCollectionInfo.getWorkflowId())
                    .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                    .serviceId(dataCollectionInfo.getServiceId())
                    .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                    .dataCollectionMinute(dataCollectionMinuteEnd)
                    .timeStamp(windowStartTimeManager)
                    .level(ClusterLevel.H0)
                    .groupName(DEFAULT_GROUP_NAME)
                    .build());
            logger.info(
                "Sending heartbeat new relic metric record to the server for minute " + dataCollectionMinuteEnd);

            boolean result = saveMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(),
                dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
                getAllMetricRecords(records));
            if (!result) {
              retry = RETRIES;
              throw new WingsException("Cannot save new relic metric records. Server returned error");
            }
            records.clear();
            windowStartTimeManager = windowEndTimeManager;

            dataCollectionMinute = dataCollectionMinuteEnd + 1;
            logger.info("Time take for data collection: " + (System.currentTimeMillis() - startTime));
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
                taskResult.setErrorMessage(Misc.getMessage(ex));
              }
              logger.warn("error fetching new relic metrics for minute " + dataCollectionMinute + ". retrying in "
                      + RETRY_SLEEP + "s",
                  ex);
              sleep(RETRY_SLEEP);
            }
          }
        }
      } catch (Exception e) {
        completed.set(true);
        taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage(
            "error fetching new relic metrics for minute " + dataCollectionMinute + " reason: " + Misc.getMessage(e));
        logger.error("error fetching new relic metrics for minute " + dataCollectionMinute, e);
      }

      if (completed.get()) {
        logger.info("Shutting down new relic data collection for {}", dataCollectionInfo);
        shutDownCollection();
        return;
      }
    }

    private boolean fetchAndSaveMetricsForNode(
        NewRelicApplicationInstance node, List<Set<String>> metricBatches, long endTime) throws Exception {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

      final long startTime = System.currentTimeMillis();
      for (Set<String> metricNames : metricBatches) {
        records.putAll(getMetricData(node, metricNames, endTime));
      }

      List<NewRelicMetricDataRecord> metricRecords = getAllMetricRecords(records);
      logger.info("Sending {} new relic metric records for node {} for minute {}. Time taken: {}",
          records.cellSet().size(), node.getHost(), dataCollectionMinute, System.currentTimeMillis() - startTime);
      return saveMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(), dataCollectionInfo.getApplicationId(),
          dataCollectionInfo.getStateExecutionId(), metricRecords);
    }

    private Set<String> getApdexMetricNames(Set<String> metricNames) {
      final Set<String> rv = new HashSet<>();
      for (String metricName : metricNames) {
        rv.add(metricName.replace("WebTransaction", "Apdex"));
      }

      // overall apdex
      metricNames.add(OVERALL_APDEX_TXN_NAME);
      return rv;
    }

    private Collection<String> getErrorMetricNames(Set<String> metricNames) {
      final Set<String> rv = new HashSet<>();
      for (String metricName : metricNames) {
        rv.add("Errors/" + metricName);
      }

      // overall errors
      metricNames.add(ALL_ERRORS_TXN_NAME);
      return rv;
    }

    private Set<NewRelicMetric> getTxnsWithDataInLastHour(Set<NewRelicMetric> metrics) throws IOException {
      Map<String, NewRelicMetric> webTransactionMetrics = new HashMap<>();
      for (NewRelicMetric metric : metrics) {
        webTransactionMetrics.put(metric.getName(), metric);
      }
      List<Set<String>> metricBatches = batchMetricsToCollect(metrics);
      List<Callable<Set<String>>> metricDataCallabels = new ArrayList<>();
      for (Set<String> metricNames : metricBatches) {
        metricDataCallabels.add(() -> getMetricsWithNoData(metricNames));
      }
      List<Optional<Set<String>>> results = executeParrallel(metricDataCallabels);
      results.forEach(result -> {
        if (result.isPresent()) {
          for (String metricName : result.get()) {
            webTransactionMetrics.remove(metricName);
          }
        }
      });
      return new HashSet<>(webTransactionMetrics.values());
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    private Set<String> getMetricsWithNoData(Set<String> metricNames) throws IOException {
      final long currentTime = System.currentTimeMillis();
      Set<String> metricsWithNoData = Sets.newHashSet(metricNames);
      NewRelicMetricData metricData = newRelicDelegateService.getMetricDataApplication(
          dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getEncryptedDataDetails(),
          dataCollectionInfo.getNewRelicAppId(), metricNames, currentTime - TimeUnit.HOURS.toMillis(1), currentTime,
          true, createApiCallLog(dataCollectionInfo.getStateExecutionId()));

      if (metricData == null) {
        throw new WingsException("Unable to get NewRelic metric data for metric name collection " + dataCollectionInfo);
      }

      metricsWithNoData.removeAll(metricData.getMetrics_found());

      NewRelicMetricData errorMetricData =
          newRelicDelegateService.getMetricDataApplication(dataCollectionInfo.getNewRelicConfig(),
              dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(),
              getErrorMetricNames(metricNames), currentTime - TimeUnit.HOURS.toMillis(1), currentTime, true,
              createApiCallLog(dataCollectionInfo.getStateExecutionId()));

      if (metricData == null) {
        throw new WingsException("Unable to get NewRelic metric data for metric name collection " + dataCollectionInfo);
      }
      metricsWithNoData.removeAll(metricData.getMetrics_found());

      for (NewRelicMetricData.NewRelicMetricSlice metric : metricData.getMetrics()) {
        for (NewRelicMetricData.NewRelicMetricTimeSlice timeSlice : metric.getTimeslices()) {
          final String webTxnJson = JsonUtils.asJson(timeSlice.getValues());
          NewRelicWebTransactions webTransactions = JsonUtils.asObject(webTxnJson, NewRelicWebTransactions.class);
          if (webTransactions.getRequests_per_minute() < MIN_RPM) {
            metricsWithNoData.add(metric.getName());
          }
        }
      }

      for (NewRelicMetricData.NewRelicMetricSlice metric : errorMetricData.getMetrics()) {
        for (NewRelicMetricData.NewRelicMetricTimeSlice timeSlice : metric.getTimeslices()) {
          final String webTxnJson = JsonUtils.asJson(timeSlice.getValues());
          NewRelicErrors webTransactions = JsonUtils.asObject(webTxnJson, NewRelicErrors.class);
          if (webTransactions.getError_count() > 0 || webTransactions.getErrors_per_minute() > 0) {
            metricsWithNoData.remove(metric.getName());
          }
        }
      }
      return metricsWithNoData;
    }

    private List<Set<String>> batchMetricsToCollect(Collection<NewRelicMetric> metrics) {
      List<Set<String>> rv = new ArrayList<>();

      Set<String> batchedMetrics = new HashSet<>();
      for (NewRelicMetric metric : metrics) {
        batchedMetrics.add(metric.getName());

        if (batchedMetrics.size() == METRIC_DATA_QUERY_BATCH_SIZE) {
          rv.add(batchedMetrics);
          batchedMetrics = new HashSet<>();
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
        value.setName(value.getName().equals("WebTransaction") ? ALL_WEB_TXN_NAME
                                                               : value.getName().replace("WebTransaction/", ""));
        rv.add(value);
      }

      return rv;
    }
  }
}

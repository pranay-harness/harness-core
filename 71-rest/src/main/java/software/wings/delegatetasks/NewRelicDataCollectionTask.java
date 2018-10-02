package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl.batchMetricsToCollect;
import static software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl.getApdexMetricNames;
import static software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl.getErrorMetricNames;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.APDEX_SCORE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.CALL_COUNT;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.ERROR;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.REQUSET_PER_MINUTE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.THROUGHPUT;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
  private static final int INITIAL_DELAY_MINUTES = 2;
  private static final int PERIOD_MINS = 1;

  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private MetricDataStoreService metricStoreService;
  private NewRelicDataCollectionInfo dataCollectionInfo;

  public NewRelicDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
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
    private long managerAnalysisStartTime;
    private TimeSeriesMlAnalysisType analysisType;

    private NewRelicMetricCollector(NewRelicDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult)
        throws IOException {
      this.dataCollectionInfo = dataCollectionInfo;
      this.managerAnalysisStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.windowStartTimeManager = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = 0;
      this.taskResult = taskResult;
      this.allTxns = newRelicDelegateService.getTxnNameToCollect(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(),
          createApiCallLog(dataCollectionInfo.getStateExecutionId()));
      this.analysisType = dataCollectionInfo.getTimeSeriesMlAnalysisType();

      logger.info("NewRelic collector initialized : managerAnalysisStartTime - {}, windowStartTimeManager {}",
          managerAnalysisStartTime, windowStartTimeManager);
    }

    private TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricData(
        NewRelicApplicationInstance node, Set<String> metricNames, long endTime) throws Exception {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

      logger.info("Fetching for host {} for stateExecutionId {} for metrics {}", node,
          dataCollectionInfo.getStateExecutionId(), metricNames);
      getWebTransactionMetrics(node, metricNames, endTime, records);
      getErrorMetrics(node, metricNames, endTime, records);
      getApdexMetrics(node, metricNames, endTime, records);
      logger.info("Fetching done for host {} for stateExecutionId {} for metrics {}", node,
          dataCollectionInfo.getStateExecutionId(), metricNames);

      logger.debug(records.toString());
      return records;
    }

    private int getCollectionMinute(long timeStamp) {
      long analysisStartTime = isPredictiveAnalysis()
          ? managerAnalysisStartTime - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES)
          : managerAnalysisStartTime;
      return (int) ((timeStamp - analysisStartTime) / TimeUnit.MINUTES.toMillis(1));
    }

    private long getStartTime() {
      long startTime = windowStartTimeManager;
      if (isPredictiveAnalysis() && dataCollectionMinute == 0) {
        startTime = windowStartTimeManager - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES);
      }
      return startTime;
    }

    private NewRelicMetricData getPredictiveOrComparativeMetricData(
        final Set<String> metricNames, long endTime, NewRelicApplicationInstance node) throws IOException {
      NewRelicMetricData metricData = null;
      if (isPredictiveAnalysis()) {
        metricData = newRelicDelegateService.getMetricDataApplication(dataCollectionInfo.getNewRelicConfig(),
            dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), metricNames,
            getStartTime(), endTime, true, createApiCallLog(dataCollectionInfo.getStateExecutionId()));
      } else {
        metricData = newRelicDelegateService.getMetricDataApplicationInstance(dataCollectionInfo.getNewRelicConfig(),
            dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
            metricNames, getStartTime(), endTime, createApiCallLog(dataCollectionInfo.getStateExecutionId()));
      }
      return metricData;
    }
    private void getWebTransactionMetrics(NewRelicApplicationInstance node, Set<String> metricNames, long endTime,
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) throws IOException {
      int retry = 0;
      while (retry < RETRIES) {
        try {
          logger.info("For datacollectionMinute {}, start and end times are {} and {}", dataCollectionMinute,
              windowStartTimeManager, endTime);
          NewRelicMetricData metricData = getPredictiveOrComparativeMetricData(metricNames, endTime, node);

          for (NewRelicMetricSlice metric : metricData.getMetrics()) {
            for (NewRelicMetricTimeSlice timeSlice : metric.getTimeslices()) {
              // set from time to the timestamp
              long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeSlice.getFrom()).toEpochSecond());

              String hostname = isPredictiveAnalysis() ? DEFAULT_GROUP_NAME : node.getHost();
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
                      .host(hostname)
                      .values(new HashMap<>())
                      .groupName(dataCollectionInfo.getHosts().get(node.getHost()))
                      .build();

              metricDataRecord.setDataCollectionMinute(getCollectionMinute(timeStamp));

              if (metricDataRecord.getDataCollectionMinute() < 0) {
                logger.info("New relic sending us data in the past. request start time {}, received time {}",
                    managerAnalysisStartTime, timeStamp);
                continue;
              }

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
          NewRelicMetricData metricData =
              getPredictiveOrComparativeMetricData(getErrorMetricNames(metricNames), endTime, node);

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
          Set<String> apdexMetricNames = getApdexMetricNames(metricNames);
          if (isEmpty(apdexMetricNames)) {
            return;
          }
          NewRelicMetricData metricData =
              getPredictiveOrComparativeMetricData(getApdexMetricNames(metricNames), endTime, node);
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
      logger.info("all txns so far {} for {}", allTxns.size(), dataCollectionInfo.getStateExecutionId());
      Set<NewRelicMetric> newTxns = newRelicDelegateService.getTxnNameToCollect(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(),
          createApiCallLog(dataCollectionInfo.getStateExecutionId()));
      newTxns.removeAll(allTxns);
      logger.info("new txns {} for {}", newTxns.size(), dataCollectionInfo.getStateExecutionId());
      Set<NewRelicMetric> txnsWithData = newRelicDelegateService.getTxnsWithDataInLastHour(allTxns,
          dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getEncryptedDataDetails(),
          dataCollectionInfo.getNewRelicAppId(), createApiCallLog(dataCollectionInfo.getStateExecutionId()));
      logger.info("txns with data {} for {}", txnsWithData.size(), dataCollectionInfo.getStateExecutionId());
      txnsWithData.addAll(newTxns);
      logger.info("txns to collect {} for {}", txnsWithData.size(), dataCollectionInfo.getStateExecutionId());
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
        long startTime = System.currentTimeMillis();
        final long windowEndTimeManager = windowStartTimeManager + TimeUnit.MINUTES.toMillis(PERIOD_MINS);
        int collectionLength = timeDeltaInMins(windowEndTimeManager, windowStartTimeManager);
        int dataCollectionMinuteEnd = dataCollectionMinute + collectionLength - 1;
        logger.info("Running new relic data collection for minute {}, state execution {}", dataCollectionMinuteEnd,
            dataCollectionInfo.getStateExecutionId());

        while (!completed.get() && retry < RETRIES) {
          try {
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

            List<Set<String>> metricBatches = batchMetricsToCollect(txnsToCollect);
            logger.info("Found total new relic metric batches " + metricBatches.size());

            List<Callable<Boolean>> callables = new ArrayList<>();
            if (isPredictiveAnalysis()) {
              logger.info(
                  "AnalysisType is Predictive. So we're collecting metrics by application instead of host/node");
              callables.add(() -> fetchAndSaveMetricsForNode(instances.get(0), metricBatches, windowEndTimeManager));
            } else {
              for (NewRelicApplicationInstance node : instances) {
                if (!dataCollectionInfo.getHosts().keySet().contains(node.getHost())) {
                  logger.info("Skipping host {} for stateExecutionId {} ", node.getHost(),
                      dataCollectionInfo.getStateExecutionId());
                  continue;
                }

                logger.info("Going to collect for host {} for stateExecutionId {}, for metrics {}", node.getHost(),
                    dataCollectionInfo.getStateExecutionId(), metricBatches);
                callables.add(() -> fetchAndSaveMetricsForNode(node, metricBatches, windowEndTimeManager));
              }
            }
            logger.info("submitting parallel tasks {}", callables.size());
            List<Optional<Boolean>> results = executeParrallel(callables);
            for (Optional<Boolean> result : results) {
              if (!result.isPresent() || !result.get()) {
                logger.error("Error saving metrics to the database. DatacollectionMin: {} StateexecutionId: {}",
                    dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
              }
            }

            if (!saveHeartBeats(dataCollectionMinuteEnd)) {
              logger.error("Error saving heartbeat to the database. DatacollectionMin: {} StateexecutionId: {}",
                  dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
            }

            logger.info("done processing parallel tasks {}", callables.size());
            windowStartTimeManager = windowEndTimeManager;

            dataCollectionMinute = dataCollectionMinuteEnd + 1;

            if (dataCollectionMinute >= dataCollectionInfo.getCollectionTime()) {
              // We are done with all data collection, so setting task status to success and quitting.
              logger.info("Completed NewRelic collection task. So setting task status to success and quitting");
              completed.set(true);
              taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
            }
            logger.info("Time take for data collection: " + (System.currentTimeMillis() - startTime));
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

    private boolean saveHeartBeats(int dataCollectionMinuteEnd) {
      Set<String> groups = new HashSet<>();
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();
      for (Map.Entry<String, String> entry : dataCollectionInfo.getHosts().entrySet()) {
        if (!groups.contains(entry.getValue())) {
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
                  .groupName(entry.getValue())
                  .build());
          logger.info("adding heartbeat new relic metric record for group {} for minute {}", entry.getValue(),
              dataCollectionMinuteEnd);
          groups.add(entry.getValue());
        }
      }
      List<NewRelicMetricDataRecord> metricRecords = getAllMetricRecords(records);
      logger.info(
          "Sending {} new relic heart  beat records for minute {}", records.cellSet().size(), dataCollectionMinuteEnd);
      return saveMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(), dataCollectionInfo.getApplicationId(),
          dataCollectionInfo.getStateExecutionId(), metricRecords);
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
          records.cellSet().size(), node, dataCollectionMinute, System.currentTimeMillis() - startTime);
      return saveMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(), dataCollectionInfo.getApplicationId(),
          dataCollectionInfo.getStateExecutionId(), metricRecords);
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
    private boolean isPredictiveAnalysis() {
      return analysisType.equals(TimeSeriesMlAnalysisType.PREDICTIVE);
    }
  }
}

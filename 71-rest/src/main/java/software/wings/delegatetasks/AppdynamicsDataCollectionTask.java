package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DURATION_TO_ASK_MINUTES;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.common.base.Preconditions;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataValue;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.appdynamics.AppdynamicsTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.regex.Pattern;

/**
 * Created by rsingh on 5/18/17.
 */
@Slf4j
public class AppdynamicsDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private AppdynamicsDataCollectionInfo dataCollectionInfo;

  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;

  @Inject private MetricDataStoreService metricStoreService;
  @Inject private EncryptionService encryptionService;

  private static final Set<String> REJECTED_METRICS_24X7 =
      new HashSet<>(Arrays.asList(AppdynamicsTimeSeries.NUMBER_OF_SLOW_CALLS.getMetricName(),
          AppdynamicsTimeSeries.RESPONSE_TIME_95.getMetricName()));
  private static final Set<String> REJECTED_METRICS_WORKFLOW =
      new HashSet<>(Arrays.asList(AppdynamicsTimeSeries.AVG_RESPONSE_TIME.getMetricName()));

  public AppdynamicsDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    dataCollectionInfo = (AppdynamicsDataCollectionInfo) parameters;
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.APP_DYNAMICS)
        .build();
  }

  @Override
  protected boolean is24X7Task() {
    return getTaskType().equals(TaskType.APPDYNAMICS_COLLECT_24_7_METRIC_DATA.name());
  }

  @Override
  protected StateType getStateType() {
    return StateType.APP_DYNAMICS;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) {
    return new AppdynamicsMetricCollector(dataCollectionInfo, taskResult,
        this.getTaskType().equals(TaskType.APPDYNAMICS_COLLECT_24_7_METRIC_DATA.name()));
  }

  private class AppdynamicsMetricCollector implements Runnable {
    private final AppdynamicsDataCollectionInfo dataCollectionInfo;
    private long collectionStartTime;
    private int dataCollectionMinute;
    private final DataCollectionTaskResult taskResult;
    private AppDynamicsConfig appDynamicsConfig;
    private boolean is247Task;
    private int maxDataCollectionMin24x7;

    private AppdynamicsMetricCollector(
        AppdynamicsDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult, boolean is247Task) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
          - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      this.taskResult = taskResult;
      appDynamicsConfig = dataCollectionInfo.getAppDynamicsConfig();
      encryptionService.decrypt(appDynamicsConfig, dataCollectionInfo.getEncryptedDataDetails());
      this.is247Task = is247Task;
    }

    private List<AppdynamicsMetricData> getMetricsData() throws IOException, CloneNotSupportedException {
      final List<EncryptedDataDetail> encryptionDetails = dataCollectionInfo.getEncryptedDataDetails();
      final long appId = dataCollectionInfo.getAppId();
      final long tierId = dataCollectionInfo.getTierId();
      final AppdynamicsTier tier = appdynamicsDelegateService.getAppdynamicsTier(appDynamicsConfig, appId, tierId,
          encryptionDetails, createApiCallLog(dataCollectionInfo.getStateExecutionId()));
      final List<AppdynamicsMetric> tierMetrics = appdynamicsDelegateService.getTierBTMetrics(appDynamicsConfig, appId,
          tierId, encryptionDetails, createApiCallLog(dataCollectionInfo.getStateExecutionId()));

      final List<AppdynamicsMetricData> metricsData = new ArrayList<>();
      List<Callable<List<AppdynamicsMetricData>>> callables = new ArrayList<>();
      long endTimeForCollection = System.currentTimeMillis();

      for (AppdynamicsMetric appdynamicsMetric : tierMetrics) {
        switch (dataCollectionInfo.getTimeSeriesMlAnalysisType()) {
          case COMPARATIVE:
            for (String hostName : dataCollectionInfo.getHosts().keySet()) {
              callables.add(()
                                -> appdynamicsDelegateService.getTierBTMetricData(appDynamicsConfig, appId,
                                    tier.getName(), appdynamicsMetric.getName(), hostName,
                                    endTimeForCollection - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES),
                                    endTimeForCollection, encryptionDetails,
                                    createApiCallLog(dataCollectionInfo.getStateExecutionId())));
            }
            break;
          case PREDICTIVE:
            if (is247Task) {
              long startTime = dataCollectionInfo.getStartTime();
              long endTime =
                  dataCollectionInfo.getStartTime() + TimeUnit.MINUTES.toMillis(dataCollectionInfo.getCollectionTime());
              callables.add(()
                                -> appdynamicsDelegateService.getTierBTMetricData(appDynamicsConfig, appId,
                                    tier.getName(), appdynamicsMetric.getName(), null, startTime, endTime,
                                    encryptionDetails, createApiCallLog(dataCollectionInfo.getStateExecutionId())));
            } else {
              callables.add(
                  ()
                      -> appdynamicsDelegateService.getTierBTMetricData(appDynamicsConfig, appId, tier.getName(),
                          appdynamicsMetric.getName(), null,
                          endTimeForCollection
                              - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES + DURATION_TO_ASK_MINUTES),
                          endTimeForCollection, encryptionDetails,
                          createApiCallLog(dataCollectionInfo.getStateExecutionId())));
            }
            break;

          default:
            throw new IllegalStateException("Invalid type " + dataCollectionInfo.getTimeSeriesMlAnalysisType());
        }
      }
      List<Optional<List<AppdynamicsMetricData>>> results = executeParrallel(callables);
      results.forEach(result -> {
        if (result.isPresent()) {
          metricsData.addAll(result.get());
        }
      });

      for (int i = metricsData.size() - 1; i >= 0; i--) {
        String metricName = metricsData.get(i).getMetricName();
        if (metricName.contains("|")) {
          metricName = metricName.substring(metricName.lastIndexOf('|') + 1);
        }
        if (!AppdynamicsTimeSeries.getMetricsToTrack().contains(metricName)) {
          metricsData.remove(i);
          if (!metricName.equals("METRIC DATA NOT FOUND")) {
            if (logger.isDebugEnabled()) {
              logger.debug("metric with unexpected name found: " + metricName);
            }
          }
        }
      }
      return metricsData;
    }

    private TreeBasedTable<String, Long, Map<String, NewRelicMetricDataRecord>> processMetricData(
        List<AppdynamicsMetricData> metricsData) {
      TreeBasedTable<String, Long, Map<String, NewRelicMetricDataRecord>> records = TreeBasedTable.create();
      /*
       * An AppDynamics metric path looks like:
       * "Business Transaction Performance|Business Transactions|test-tier|/todolist/|Individual Nodes|test-node|Number
       * of Slow Calls" Element 0 and 1 are constant Element 2 is the tier name Element 3 is the BT name Element 4 is
       * constant Element 5 is the node name Element 6 is the metric name
       */
      for (AppdynamicsMetricData metricData : metricsData) {
        String[] appdynamicsPathPieces = metricData.getMetricPath().split(Pattern.quote("|"));
        String tierName = parseAppdynamicsInternalName(appdynamicsPathPieces, 2);
        String nodeName = dataCollectionInfo.getTimeSeriesMlAnalysisType().equals(TimeSeriesMlAnalysisType.PREDICTIVE)
            ? tierName
            : appdynamicsPathPieces[5];
        if (dataCollectionInfo.getTimeSeriesMlAnalysisType().equals(TimeSeriesMlAnalysisType.COMPARATIVE)
            && !dataCollectionInfo.getHosts().keySet().contains(nodeName)) {
          logger.info("skipping: {}", nodeName);
          continue;
        }
        String btName = parseAppdynamicsInternalName(appdynamicsPathPieces, 3);
        String metricName =
            dataCollectionInfo.getTimeSeriesMlAnalysisType().equals(TimeSeriesMlAnalysisType.COMPARATIVE)
            ? parseAppdynamicsInternalName(appdynamicsPathPieces, 6)
            : parseAppdynamicsInternalName(appdynamicsPathPieces, 4);

        for (AppdynamicsMetricDataValue metricDataValue : metricData.getMetricValues()) {
          long metricTimeStamp = metricDataValue.getStartTimeInMillis();
          int dataCollectionMinForRecord = getCollectionMinute(metricTimeStamp, false);
          if (is247Task && dataCollectionMinForRecord > maxDataCollectionMin24x7) {
            maxDataCollectionMin24x7 = dataCollectionMinForRecord;
          }

          Map<String, NewRelicMetricDataRecord> hostVsRecordMap = records.get(btName, metricTimeStamp);
          if (hostVsRecordMap == null) {
            hostVsRecordMap = new HashMap<>();
            records.put(btName, metricTimeStamp, hostVsRecordMap);
          }

          NewRelicMetricDataRecord hostRecord = hostVsRecordMap.get(nodeName);
          if (hostRecord == null) {
            hostRecord = NewRelicMetricDataRecord.builder()
                             .name(btName)
                             .appId(dataCollectionInfo.getApplicationId())
                             .workflowId(dataCollectionInfo.getWorkflowId())
                             .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                             .serviceId(dataCollectionInfo.getServiceId())
                             .cvConfigId(dataCollectionInfo.getCvConfigId())
                             .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                             .dataCollectionMinute(dataCollectionMinForRecord)
                             .timeStamp(metricTimeStamp)
                             .host(nodeName)
                             .groupName(tierName)
                             .cvConfigId(dataCollectionInfo.getCvConfigId())
                             .stateType(getStateType())
                             .values(new HashMap<>())
                             .deeplinkMetadata(new HashMap<>())
                             .build();
            if (hostRecord.getTimeStamp() >= dataCollectionInfo.getStartTime()) {
              hostVsRecordMap.put(nodeName, hostRecord);
            } else {
              logger.info("Ignoring a record that was before the collectionStartTime.");
            }
          }
          Set<String> rejectedMetricsList = is247Task ? REJECTED_METRICS_24X7 : REJECTED_METRICS_WORKFLOW;
          if (!rejectedMetricsList.contains(metricName)) {
            hostRecord.getValues().put(AppdynamicsTimeSeries.getVariableName(metricName), metricDataValue.getValue());
            hostRecord.getDeeplinkMetadata().put(metricName, getDeeplinkString(metricData.getMetricId()));
          }
        }
      }
      return records;
    }

    private String getDeeplinkString(long metricId) {
      return dataCollectionInfo.getTierId() + "." + metricId;
    }

    private int getCollectionMinute(final long metricTimeStamp, boolean isHeartbeat) {
      boolean isPredictiveAnalysis =
          dataCollectionInfo.getTimeSeriesMlAnalysisType().equals(TimeSeriesMlAnalysisType.PREDICTIVE);
      long collectionStartTime = !isPredictiveAnalysis || is247Task
          ? dataCollectionInfo.getStartTime()
          : dataCollectionInfo.getStartTime() - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES);

      int collectionMinute;
      if (isHeartbeat) {
        if (is247Task) {
          collectionMinute = maxDataCollectionMin24x7 != 0
              ? maxDataCollectionMin24x7
              : (int) TimeUnit.MILLISECONDS.toMinutes(collectionStartTime) + dataCollectionInfo.getCollectionTime();
        } else if (isPredictiveAnalysis) {
          collectionMinute = dataCollectionMinute + PREDECTIVE_HISTORY_MINUTES + DURATION_TO_ASK_MINUTES;
        } else {
          collectionMinute = dataCollectionMinute;
        }
      } else {
        if (is247Task) {
          collectionMinute = (int) TimeUnit.MILLISECONDS.toMinutes(metricTimeStamp);
        } else {
          collectionMinute = (int) (TimeUnit.MILLISECONDS.toMinutes(metricTimeStamp - collectionStartTime));
        }
      }
      return collectionMinute;
    }

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      int retry = 0;
      while (!completed.get() && retry < RETRIES) {
        try {
          logger.info("starting metric data collection for {} for minute {}", dataCollectionInfo, dataCollectionMinute);
          AppdynamicsTier appdynamicsTier = appdynamicsDelegateService.getAppdynamicsTier(appDynamicsConfig,
              dataCollectionInfo.getAppId(), dataCollectionInfo.getTierId(),
              dataCollectionInfo.getEncryptedDataDetails(), createApiCallLog(dataCollectionInfo.getStateExecutionId()));
          Preconditions.checkNotNull(dataCollectionInfo, "No trier found for dataCollectionInfo");
          setHostIdsIfNecessary();
          List<AppdynamicsMetricData> metricsData = getMetricsData();
          logger.info(
              "Got {} metrics from appdynamics for {}", metricsData.size(), dataCollectionInfo.getStateExecutionId());
          TreeBasedTable<String, Long, Map<String, NewRelicMetricDataRecord>> records = TreeBasedTable.create();
          records.putAll(processMetricData(metricsData));

          // HeartBeat
          records.put(HARNESS_HEARTBEAT_METRIC_NAME, 0L, new HashMap<>());
          records.get(HARNESS_HEARTBEAT_METRIC_NAME, 0L)
              .put("heartbeatHost",
                  NewRelicMetricDataRecord.builder()
                      .stateType(getStateType())
                      .name(HARNESS_HEARTBEAT_METRIC_NAME)
                      .appId(dataCollectionInfo.getApplicationId())
                      .workflowId(dataCollectionInfo.getWorkflowId())
                      .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                      .serviceId(dataCollectionInfo.getServiceId())
                      .cvConfigId(dataCollectionInfo.getCvConfigId())
                      .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                      .dataCollectionMinute(getCollectionMinute(System.currentTimeMillis(), true))
                      .timeStamp(collectionStartTime)
                      .cvConfigId(dataCollectionInfo.getCvConfigId())
                      .level(ClusterLevel.H0)
                      .groupName(appdynamicsTier.getName())
                      .build());

          List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(records);
          if (!saveMetrics(appDynamicsConfig.getAccountId(), dataCollectionInfo.getApplicationId(),
                  dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
            logger.error("Error saving metrics to the database. DatacollectionMin: {} StateexecutionId: {}",
                dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
          } else {
            logger.info("Sent {} appdynamics metric records to the server for minute {} for state {}",
                recordsToSave.size(), dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
          }
          dataCollectionMinute++;
          collectionStartTime += TimeUnit.MINUTES.toMillis(1);
          dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
          if (dataCollectionInfo.getCollectionTime() <= 0 || is247Task) {
            // We are done with all data collection, so setting task status to success and quitting.
            logger.info("Completed AppDynamics collection task. So setting task status to success and quitting");
            completed.set(true);
            taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
          }
          break;

        } catch (Throwable ex) {
          if (!(ex instanceof Exception) || ++retry >= RETRIES) {
            logger.error("error fetching  metrics for {} for minute {}", dataCollectionInfo.getStateExecutionId(),
                dataCollectionMinute, ex);
            taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
            completed.set(true);
            break;
          } else {
            if (retry == 1) {
              taskResult.setErrorMessage(ExceptionUtils.getMessage(ex));
            }
            logger.info(
                "error fetching appdynamics metrics for minute {} for state {}. retrying in " + RETRY_SLEEP + "s",
                dataCollectionMinute, dataCollectionInfo.getStateExecutionId(), ex);
            sleep(RETRY_SLEEP);
          }
        }
      }

      if (completed.get()) {
        logger.info("Shutting down appdynamics data collection");
        shutDownCollection();
        return;
      }
    }

    private void setHostIdsIfNecessary() throws IOException {
      if (!dataCollectionInfo.isNodeIdsMapped() && isNotEmpty(dataCollectionInfo.getHosts())) {
        final Set<AppdynamicsNode> nodes = appdynamicsDelegateService.getNodes(appDynamicsConfig,
            dataCollectionInfo.getAppId(), dataCollectionInfo.getTierId(), dataCollectionInfo.getEncryptedDataDetails(),
            createApiCallLog(dataCollectionInfo.getStateExecutionId()));
        Map<String, String> hosts = new HashMap<>();
        dataCollectionInfo.getHosts().forEach((hostName, groupName) -> nodes.forEach(appdynamicsNode -> {
          if (appdynamicsNode.getName().toLowerCase().equals(hostName.toLowerCase())) {
            hosts.put(hostName, appdynamicsNode.getName());
            return;
          }
        }));

        dataCollectionInfo.getHosts().forEach((hostName, groupName) -> {
          if (!hosts.containsKey(hostName)) {
            logger.error("No corresponding node found in appdynamics state {} for node {}, nodes from appdynamics {}",
                dataCollectionInfo.getStateExecutionId(), hostName, nodes);
            throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
                .addParam("reason", "No corresponding node found in appdynamics for " + hostName);
          }
        });

        Map<String, String> finalHosts = new HashMap<>();
        dataCollectionInfo.getHosts().forEach((hostName, groupName) -> finalHosts.put(hosts.get(hostName), groupName));
        logger.info("for state {} final hosts are {}", dataCollectionInfo.getStateExecutionId(), finalHosts);
        dataCollectionInfo.setHosts(finalHosts);
        dataCollectionInfo.setNodeIdsMapped(true);
      }
    }

    private List<NewRelicMetricDataRecord> getAllMetricRecords(
        TreeBasedTable<String, Long, Map<String, NewRelicMetricDataRecord>> records) {
      List<NewRelicMetricDataRecord> rv = new ArrayList<>();
      for (Cell<String, Long, Map<String, NewRelicMetricDataRecord>> cell : records.cellSet()) {
        rv.addAll(cell.getValue().values());
      }

      return rv;
    }
  }

  private static String parseAppdynamicsInternalName(String[] appdynamicsPathPieces, int index) {
    String name = appdynamicsPathPieces[index];
    if (name == null) {
      return name;
    }

    // mongo doesn't like dots in the names
    return name.replaceAll("\\.", "-");
  }
}

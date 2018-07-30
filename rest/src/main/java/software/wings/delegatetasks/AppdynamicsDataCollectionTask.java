package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.common.base.Preconditions;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataValue;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.appdynamics.AppdynamicsTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Created by rsingh on 5/18/17.
 */
public class AppdynamicsDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsDataCollectionTask.class);
  public static final int DURATION_TO_ASK_MINUTES = 5;
  public static final int PREDECTIVE_HISTORY_MINUTES = 120;
  private AppdynamicsDataCollectionInfo dataCollectionInfo;

  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;

  @Inject private MetricDataStoreService metricStoreService;

  public AppdynamicsDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (AppdynamicsDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.APP_DYNAMICS)
        .build();
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
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new AppdynamicsMetricCollector(dataCollectionInfo, taskResult);
  }

  private class AppdynamicsMetricCollector implements Runnable {
    private final AppdynamicsDataCollectionInfo dataCollectionInfo;
    private long collectionStartTime;
    private int dataCollectionMinute;
    private final DataCollectionTaskResult taskResult;

    private AppdynamicsMetricCollector(
        AppdynamicsDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
          - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      this.taskResult = taskResult;
    }

    private List<AppdynamicsMetricData> getMetricsData() throws IOException, CloneNotSupportedException {
      final AppDynamicsConfig appDynamicsConfig = dataCollectionInfo.getAppDynamicsConfig();
      final List<EncryptedDataDetail> encryptionDetails = dataCollectionInfo.getEncryptedDataDetails();
      final long appId = dataCollectionInfo.getAppId();
      final long tierId = dataCollectionInfo.getTierId();
      final List<AppdynamicsMetric> tierMetrics = appdynamicsDelegateService.getTierBTMetrics(appDynamicsConfig, appId,
          tierId, encryptionDetails, createApiCallLog(dataCollectionInfo.getStateExecutionId()));

      final List<AppdynamicsMetricData> metricsData = new ArrayList<>();
      List<Callable<List<AppdynamicsMetricData>>> callables = new ArrayList<>();
      for (AppdynamicsMetric appdynamicsMetric : tierMetrics) {
        switch (dataCollectionInfo.getTimeSeriesMlAnalysisType()) {
          case COMPARATIVE:
            for (String hostName : dataCollectionInfo.getHosts().keySet()) {
              callables.add(()
                                -> appdynamicsDelegateService.getTierBTMetricData(appDynamicsConfig, appId, tierId,
                                    appdynamicsMetric.getName(), hostName, DURATION_TO_ASK_MINUTES, encryptionDetails,
                                    createApiCallLog(dataCollectionInfo.getStateExecutionId())));
            }
            break;
          case PREDICTIVE:
            callables.add(
                ()
                    -> appdynamicsDelegateService.getTierBTMetricData(appDynamicsConfig, appId, tierId,
                        appdynamicsMetric.getName(), null, PREDECTIVE_HISTORY_MINUTES + DURATION_TO_ASK_MINUTES,
                        encryptionDetails, createApiCallLog(dataCollectionInfo.getStateExecutionId())));
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
        List<AppdynamicsMetricData> metricsData)
        throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
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
                             .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                             .dataCollectionMinute(getCollectionMinute(metricTimeStamp))
                             .timeStamp(metricTimeStamp)
                             .host(nodeName)
                             .groupName(tierName)
                             .stateType(getStateType())
                             .values(new HashMap<>())
                             .build();
            hostVsRecordMap.put(nodeName, hostRecord);
          }

          hostRecord.getValues().put(AppdynamicsTimeSeries.getVariableName(metricName), metricDataValue.getValue());
        }
      }
      return records;
    }

    private int getCollectionMinute(long metricTimeStamp) {
      int collectionDuration =
          dataCollectionInfo.getTimeSeriesMlAnalysisType().equals(TimeSeriesMlAnalysisType.COMPARATIVE)
          ? DURATION_TO_ASK_MINUTES
          : PREDECTIVE_HISTORY_MINUTES + DURATION_TO_ASK_MINUTES;

      return (int) (TimeUnit.MILLISECONDS.toMinutes(metricTimeStamp - dataCollectionInfo.getStartTime())
          + collectionDuration);
    }

    @SuppressFBWarnings({"DMI_ARGUMENTS_WRONG_ORDER", "REC_CATCH_EXCEPTION"})
    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
            logger.info("starting metric data collection for {} for minute {}",
                dataCollectionInfo.getStateExecutionId(), dataCollectionMinute);
            AppdynamicsTier appdynamicsTier = appdynamicsDelegateService.getAppdynamicsTier(
                dataCollectionInfo.getAppDynamicsConfig(), dataCollectionInfo.getAppId(),
                dataCollectionInfo.getTierId(), dataCollectionInfo.getEncryptedDataDetails());
            Preconditions.checkNotNull("No trier found for {}", dataCollectionInfo);
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
                        .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                        .dataCollectionMinute(
                            dataCollectionInfo.getTimeSeriesMlAnalysisType().equals(TimeSeriesMlAnalysisType.PREDICTIVE)
                                ? dataCollectionMinute + PREDECTIVE_HISTORY_MINUTES + DURATION_TO_ASK_MINUTES
                                : dataCollectionMinute)
                        .timeStamp(collectionStartTime)
                        .level(ClusterLevel.H0)
                        .groupName(appdynamicsTier.getName())
                        .build());

            List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(records);
            if (!saveMetrics(dataCollectionInfo.getAppDynamicsConfig().getAccountId(),
                    dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
              logger.error("Error saving metrics to the database. DatacollectionMin: {} StateexecutionId: {}",
                  dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
            } else {
              logger.info("Sent {} appdynamics metric records to the server for minute {} for state {}",
                  recordsToSave.size(), dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
            }
            dataCollectionMinute++;
            collectionStartTime += TimeUnit.MINUTES.toMillis(1);
            dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
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
              logger.info(
                  "error fetching appdynamics metrics for minute {} for state {}. retrying in " + RETRY_SLEEP + "s",
                  dataCollectionMinute, dataCollectionInfo.getStateExecutionId(), ex);
              sleep(RETRY_SLEEP);
            }
          }
        }

      } catch (Exception e) {
        completed.set(true);
        taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage("error fetching appdynamics metrics for minute " + dataCollectionMinute);
        logger.error("error fetching appdynamics metrics for minute " + dataCollectionMinute, e);
      }

      if (completed.get()) {
        logger.info("Shutting down appdynamics data collection");
        shutDownCollection();
        return;
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

package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask;
import software.wings.exception.WingsException;
import software.wings.metrics.appdynamics.AppdynamicsConstants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.sm.StateType;
import software.wings.time.WingsTimeUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
  private static final int DURATION_TO_ASK_MINUTES = 5;
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
    logger.info("metric collection - dataCollectionInfo: {}" + dataCollectionInfo);
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
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime())
          - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      this.taskResult = taskResult;
    }

    private List<AppdynamicsMetricData> getMetricsData() throws IOException {
      final AppDynamicsConfig appDynamicsConfig = dataCollectionInfo.getAppDynamicsConfig();
      final List<EncryptedDataDetail> encryptionDetails = dataCollectionInfo.getEncryptedDataDetails();
      final long appId = dataCollectionInfo.getAppId();
      final long tierId = dataCollectionInfo.getTierId();
      final List<AppdynamicsMetric> tierMetrics =
          appdynamicsDelegateService.getTierBTMetrics(appDynamicsConfig, appId, tierId, encryptionDetails);

      final List<AppdynamicsMetricData> metricsData = new ArrayList<>();
      List<Callable<List<AppdynamicsMetricData>>> callables = new ArrayList<>();
      for (AppdynamicsMetric appdynamicsMetric : tierMetrics) {
        for (String hostName : dataCollectionInfo.getHosts()) {
          callables.add(()
                            -> appdynamicsDelegateService.getTierBTMetricData(appDynamicsConfig, appId, tierId,
                                appdynamicsMetric.getName(), hostName, DURATION_TO_ASK_MINUTES, encryptionDetails));
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
        if (!AppdynamicsConstants.METRICS_TO_TRACK.contains(metricName)) {
          metricsData.remove(i);
          if (!metricName.equals("METRIC DATA NOT FOUND")) {
            logger.debug("metric with unexpected name found: " + metricName);
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
        String nodeName = appdynamicsPathPieces[5];
        if (!dataCollectionInfo.getHosts().contains(nodeName)) {
          logger.info("skipping: {}", nodeName);
          continue;
        }
        String btName = parseAppdynamicsInternalName(appdynamicsPathPieces, 3);
        String metricName = parseAppdynamicsInternalName(appdynamicsPathPieces, 6);

        for (AppdynamicsMetricDataValue metricDataValue : metricData.getMetricValues()) {
          Map<String, NewRelicMetricDataRecord> hostVsRecordMap =
              records.get(btName, metricDataValue.getStartTimeInMillis());
          if (hostVsRecordMap == null) {
            hostVsRecordMap = new HashMap<>();
            records.put(btName, metricDataValue.getStartTimeInMillis(), hostVsRecordMap);
          }

          NewRelicMetricDataRecord hostRecord = hostVsRecordMap.get(nodeName);
          if (hostRecord == null) {
            hostRecord = new NewRelicMetricDataRecord();
            hostRecord.setName(btName);
            hostRecord.setApplicationId(dataCollectionInfo.getApplicationId());
            hostRecord.setWorkflowId(dataCollectionInfo.getWorkflowId());
            hostRecord.setWorkflowExecutionId(dataCollectionInfo.getWorkflowExecutionId());
            hostRecord.setServiceId(dataCollectionInfo.getServiceId());
            hostRecord.setStateExecutionId(dataCollectionInfo.getStateExecutionId());
            hostRecord.setDataCollectionMinute(dataCollectionMinute);
            hostRecord.setTimeStamp(metricDataValue.getStartTimeInMillis());
            hostRecord.setHost(nodeName);
            hostRecord.setStateType(getStateType());
            hostVsRecordMap.put(nodeName, hostRecord);
          }

          String variableName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(metricName);
          Method setValueMethod =
              NewRelicMetricDataRecord.class.getMethod("set" + WordUtils.capitalize(variableName), Double.TYPE);
          setValueMethod.invoke(hostRecord, metricDataValue.getValue());
        }
      }
      return records;
    }

    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
            List<AppdynamicsMetricData> metricsData = getMetricsData();
            TreeBasedTable<String, Long, Map<String, NewRelicMetricDataRecord>> records = TreeBasedTable.create();

            // HeartBeat
            records.put(HARNESS_HEARTBEAT_METRIC_NAME, 0l, new HashMap<>());
            records.get(HARNESS_HEARTBEAT_METRIC_NAME, 0l)
                .put("heartbeatHost",
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

            records.putAll(processMetricData(metricsData));
            List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(records);
            if (!saveMetrics(dataCollectionInfo.getAppDynamicsConfig().getAccountId(),
                    dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
              retry = RETRIES;
              taskResult.setErrorMessage(
                  "Cannot save new AppDynamics metric records to Harness. Server returned error");
              throw new RuntimeException(
                  "Cannot save new AppDynamics metric records to Harness. Server returned error");
            }
            logger.info("Sent {} appdynamics metric records to the server for minute {}", recordsToSave.size(),
                dataCollectionMinute);

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
                if (ex instanceof WingsException) {
                  if (((WingsException) ex).getParams().containsKey("reason")) {
                    taskResult.setErrorMessage((String) ((WingsException) ex).getParams().get("reason"));
                  } else {
                    taskResult.setErrorMessage(ex.getMessage());
                  }
                } else {
                  taskResult.setErrorMessage(ex.getMessage());
                }
              }
              logger.warn("error fetching appdynamics metrics for minute " + dataCollectionMinute + ". retrying in "
                      + RETRY_SLEEP + "s",
                  ex);
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

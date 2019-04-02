package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DURATION_TO_ASK_MINUTES;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.LOADBALANCER;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.VMINSTANCE;

import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.api.services.monitoring.v3.model.TimeSeries;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import io.harness.time.Timestamp;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.TaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.stackdriver.StackDriverDataCollectionInfo;
import software.wings.service.impl.stackdriver.StackDriverMetric;
import software.wings.service.impl.stackdriver.StackDriverNameSpace;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by Pranjal on 11/30/18.
 */
public class StackDriverDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(StackDriverDataCollectionTask.class);
  private StackDriverDataCollectionInfo dataCollectionInfo;

  @Inject private StackDriverDelegateService stackDriverDelegateService;
  @Inject private DelegateLogService delegateLogService;

  public StackDriverDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (StackDriverDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.STACK_DRIVER)
        .build();
  }

  @Override
  protected StateType getStateType() {
    return StateType.STACK_DRIVER;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected boolean is24X7Task() {
    return getTaskType().equals(TaskType.STACKDRIVER_COLLECT_24_7_METRIC_DATA.name());
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new StackDriverMetricCollector(dataCollectionInfo, taskResult, is24X7Task());
  }

  private class StackDriverMetricCollector implements Runnable {
    private final StackDriverDataCollectionInfo dataCollectionInfo;
    private final DataCollectionTaskResult taskResult;
    private long collectionStartTime;
    private int dataCollectionMinute;
    private boolean is247Task;

    private StackDriverMetricCollector(
        StackDriverDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult, boolean is247Task) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      this.is247Task = is247Task;
    }

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      encryptionService.decrypt(dataCollectionInfo.getGcpConfig(), dataCollectionInfo.getEncryptedDataDetails());
      int retry = 0;
      while (!completed.get() && retry < RETRIES) {
        try {
          logger.info("starting metric data collection for {} for minute {}", dataCollectionInfo, dataCollectionMinute);

          TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricDataRecords = getMetricsData();
          // HeartBeat
          metricDataRecords.put(HARNESS_HEARTBEAT_METRIC_NAME, 0L,
              NewRelicMetricDataRecord.builder()
                  .stateType(getStateType())
                  .name(HARNESS_HEARTBEAT_METRIC_NAME)
                  .appId(getAppId())
                  .workflowId(dataCollectionInfo.getWorkflowId())
                  .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                  .serviceId(dataCollectionInfo.getServiceId())
                  .cvConfigId(dataCollectionInfo.getCvConfigId())
                  .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                  .dataCollectionMinute(dataCollectionMinute)
                  .timeStamp(collectionStartTime)
                  .level(ClusterLevel.H0)
                  .groupName(DEFAULT_GROUP_NAME)
                  .build());

          List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(metricDataRecords);
          if (!saveMetrics(dataCollectionInfo.getGcpConfig().getAccountId(), dataCollectionInfo.getAppId(),
                  dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
            retry = RETRIES;
            taskResult.setErrorMessage("Cannot save new stack driver metric records to Harness. Server returned error");
            throw new RuntimeException("Cannot save new stack driver metric records to Harness. Server returned error");
          }
          logger.info("Sent {} stack driver metric records to the server for minute {}", recordsToSave.size(),
              dataCollectionMinute);

          dataCollectionMinute++;
          collectionStartTime += TimeUnit.MINUTES.toMillis(1);
          if (dataCollectionMinute >= dataCollectionInfo.getCollectionTime()) {
            // We are done with all data collection, so setting task status to success and quitting.
            logger.info(
                "Completed stack driver collection task. So setting task status to success and quitting. StateExecutionId {}",
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
            logger.warn("error fetching stack driver metrics for minute " + dataCollectionMinute + ". retrying in "
                    + RETRY_SLEEP + "s",
                ex);
            sleep(RETRY_SLEEP);
          }
        }
      }

      if (completed.get()) {
        logger.info("Shutting down stack driver data collection");
        shutDownCollection();
        return;
      }
    }

    public TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricDataRecords(StackDriverNameSpace nameSpace,
        StackDriverMetric metric, String hostName, String groupName, String filter, long startTime, long endTime,
        int dataCollectionMinute, ThirdPartyApiCallLog apiCallLog, boolean is247Task) throws IOException {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> rv = TreeBasedTable.create();

      String projectId = stackDriverDelegateService.getProjectId(dataCollectionInfo.getGcpConfig());
      Monitoring monitoring =
          stackDriverDelegateService.getMonitoringClient(dataCollectionInfo.getGcpConfig(), projectId);

      switch (dataCollectionInfo.getTimeSeriesMlAnalysisType()) {
        case COMPARATIVE:
          fetchMetrics(projectId, monitoring, nameSpace, metric, hostName, groupName, filter, startTime, endTime,
              getAppId(), dataCollectionInfo, rv, apiCallLog);
          break;
        case PREDICTIVE:
          final int periodToCollect = is247Task
              ? dataCollectionInfo.getCollectionTime()
              : (dataCollectionMinute == 0) ? PREDECTIVE_HISTORY_MINUTES + DURATION_TO_ASK_MINUTES
                                            : DURATION_TO_ASK_MINUTES;
          if (is247Task) {
            fetchMetrics(projectId, monitoring, nameSpace, metric, hostName, groupName, filter, startTime,
                startTime + TimeUnit.MINUTES.toMillis(periodToCollect), getAppId(), dataCollectionInfo, rv, apiCallLog);
          } else {
            fetchMetrics(projectId, monitoring, nameSpace, metric, hostName, groupName, filter,
                endTime - TimeUnit.MINUTES.toMillis(periodToCollect), endTime, getAppId(), dataCollectionInfo, rv,
                apiCallLog);
          }
          break;
        default:
          throw new WingsException("Invalid strategy " + dataCollectionInfo.getTimeSeriesMlAnalysisType());
      }
      return rv;
    }

    public void fetchMetrics(String projectId, Monitoring monitoring, StackDriverNameSpace nameSpace,
        StackDriverMetric metric, String hostName, String groupName, String filter, long startTime, long endTime,
        String appId, StackDriverDataCollectionInfo dataCollectionInfo,
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> rv, ThirdPartyApiCallLog apiCallLog) {
      String projectResource = "projects/" + projectId;

      apiCallLog.setTitle("Fetching metric data from project");
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());

      apiCallLog.addFieldToRequest(
          ThirdPartyApiCallField.builder().name("body").value(JsonUtils.asJson(filter)).type(FieldType.JSON).build());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                       .name("Start Time")
                                       .value(stackDriverDelegateService.getDateFormatTime(startTime))
                                       .type(FieldType.TIMESTAMP)
                                       .build());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                       .name("End Time")
                                       .value(stackDriverDelegateService.getDateFormatTime(endTime))
                                       .type(FieldType.TIMESTAMP)
                                       .build());

      ListTimeSeriesResponse response;
      try {
        response = monitoring.projects()
                       .timeSeries()
                       .list(projectResource)
                       .setFilter(filter)
                       .setIntervalStartTime(stackDriverDelegateService.getDateFormatTime(startTime))
                       .setIntervalEndTime(stackDriverDelegateService.getDateFormatTime(endTime))
                       .execute();
      } catch (Exception e) {
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
        delegateLogService.save(dataCollectionInfo.getGcpConfig().getAccountId(), apiCallLog);
        throw new WingsException(
            "Unsuccessful response while fetching data from StackDriver. Error message: " + e.getMessage());
      }
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_OK, response, FieldType.JSON);

      delegateLogService.save(dataCollectionInfo.getGcpConfig().getAccountId(), apiCallLog);

      List<TimeSeries> dataPoints = response.getTimeSeries();

      if (!isEmpty(dataPoints)) {
        dataPoints.forEach(datapoint -> datapoint.getPoints().forEach(point -> {
          long timeStamp = stackDriverDelegateService.getTimeStamp(point.getInterval().getEndTime());
          NewRelicMetricDataRecord newRelicMetricDataRecord =
              NewRelicMetricDataRecord.builder()
                  .stateType(StateType.STACK_DRIVER)
                  .appId(appId)
                  .name(Misc.replaceDotWithUnicode(metric.getMetricName()))
                  .workflowId(dataCollectionInfo.getWorkflowId())
                  .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                  .serviceId(dataCollectionInfo.getServiceId())
                  .cvConfigId(dataCollectionInfo.getCvConfigId())
                  .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                  .timeStamp(timeStamp)
                  .dataCollectionMinute(fetchCollectionMinute())
                  .host(hostName)
                  .groupName(groupName)
                  .values(new HashMap<>())
                  .build();
          newRelicMetricDataRecord.getValues().put(
              Misc.replaceDotWithUnicode(metric.getMetricName()), point.getValue().getDoubleValue());

          rv.put(nameSpace.name(), timeStamp, newRelicMetricDataRecord);
        }));
      }
    }

    public TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricsData() throws IOException {
      final TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricDataResponses = TreeBasedTable.create();
      List<Callable<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> callables = new ArrayList<>();

      long startTime = collectionStartTime;
      long endTime = Timestamp.minuteBoundary(System.currentTimeMillis());

      if (!isEmpty(dataCollectionInfo.getLoadBalancerMetrics())) {
        dataCollectionInfo.getLoadBalancerMetrics().forEach(
            (forwardRule, stackDriverMetrics) -> stackDriverMetrics.forEach(stackDriverMetric -> {
              callables.add(
                  ()
                      -> getMetricDataRecords(LOADBALANCER, stackDriverMetric, forwardRule, DEFAULT_GROUP_NAME,
                          stackDriverDelegateService.createFilter(
                              LOADBALANCER, stackDriverMetric.getMetricName(), null, forwardRule),
                          startTime, endTime, dataCollectionMinute,
                          createApiCallLog(dataCollectionInfo.getStateExecutionId()), is247Task));
            }));
      }

      if (!isEmpty(dataCollectionInfo.getVmInstanceMetrics())) {
        dataCollectionInfo.getHosts().forEach(
            (host, groupName)
                -> dataCollectionInfo.getVmInstanceMetrics().forEach(metric
                    -> callables.add(
                        ()
                            -> getMetricDataRecords(VMINSTANCE, metric, host, groupName,
                                stackDriverDelegateService.createFilter(VMINSTANCE, metric.getMetricName(), host, null),
                                startTime, endTime, dataCollectionMinute,
                                createApiCallLog(dataCollectionInfo.getStateExecutionId()), is247Task))));
      }

      logger.info("fetching stackdriver metrics for {} strategy {} for min {}",
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getTimeSeriesMlAnalysisType(),
          dataCollectionMinute);
      List<Optional<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> results = executeParrallel(callables);
      logger.info("done fetching stackdriver metrics for {} strategy {} for min {}",
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getTimeSeriesMlAnalysisType(),
          dataCollectionMinute);
      results.forEach(result -> {
        if (result.isPresent()) {
          TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = result.get();
          for (Cell<String, Long, NewRelicMetricDataRecord> cell : records.cellSet()) {
            NewRelicMetricDataRecord metricDataRecord = metricDataResponses.get(cell.getRowKey(), cell.getColumnKey());
            if (metricDataRecord != null) {
              metricDataRecord.getValues().putAll(cell.getValue().getValues());
            } else {
              metricDataResponses.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
            }
          }
        }
      });
      return metricDataResponses;
    }

    private int fetchCollectionMinute() {
      boolean isPredictiveAnalysis =
          dataCollectionInfo.getTimeSeriesMlAnalysisType().equals(TimeSeriesMlAnalysisType.PREDICTIVE);
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

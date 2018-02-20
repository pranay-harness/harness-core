package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.common.base.Preconditions;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DynaTraceConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataRequest;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataResponse;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.sm.StateType;
import software.wings.sm.states.DynatraceState;
import software.wings.time.WingsTimeUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.lang.reflect.Field;
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
public class DynaTraceDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(DynaTraceDataCollectionTask.class);
  private static final int DURATION_TO_ASK_MINUTES = 5;
  private static final int CANARY_DAYS_TO_COLLECT = 7;
  private DynaTraceDataCollectionInfo dataCollectionInfo;

  @Inject private DynaTraceDelegateService dynaTraceDelegateService;
  @Inject private MetricDataStoreService metricStoreService;

  public DynaTraceDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (DynaTraceDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}" + dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.DYNA_TRACE)
        .build();
  }

  @Override
  protected StateType getStateType() {
    return StateType.DYNA_TRACE;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new DynaTraceMetricCollector(dataCollectionInfo, taskResult);
  }

  private class DynaTraceMetricCollector implements Runnable {
    private final DynaTraceDataCollectionInfo dataCollectionInfo;
    private final DataCollectionTaskResult taskResult;
    private long collectionStartTime;
    private int dataCollectionMinute;

    private DynaTraceMetricCollector(
        DynaTraceDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime())
          - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
    }

    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
            List<DynaTraceMetricDataResponse> metricsData = getMetricsData();
            TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();
            // HeartBeat
            records.put(HARNESS_HEARTBEAT_METRIC_NAME, 0L,
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
            if (!saveMetrics(dataCollectionInfo.getDynaTraceConfig().getAccountId(),
                    dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
              retry = RETRIES;
              taskResult.setErrorMessage("Cannot save new Dynatrace metric records to Harness. Server returned error");
              throw new RuntimeException("Cannot save new Dynatrace metric records to Harness. Server returned error");
            }
            logger.info("Sent {} Dynatrace metric records to the server for minute {}", recordsToSave.size(),
                dataCollectionMinute);

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
              logger.warn("error fetching Dynatrace metrics for minute " + dataCollectionMinute + ". retrying in "
                      + RETRY_SLEEP + "s",
                  ex);
              sleep(RETRY_SLEEP);
            }
          }
        }
      } catch (Exception e) {
        completed.set(true);
        taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage("error fetching Dynatrace metrics for minute " + dataCollectionMinute);
        logger.error("error fetching Dynatrace metrics for minute " + dataCollectionMinute, e);
      }

      if (completed.get()) {
        logger.info("Shutting down Dynatrace data collection");
        shutDownCollection();
        return;
      }
    }

    public List<DynaTraceMetricDataResponse> getMetricsData() throws IOException {
      final DynaTraceConfig dynaTraceConfig = dataCollectionInfo.getDynaTraceConfig();
      final List<EncryptedDataDetail> encryptionDetails = dataCollectionInfo.getEncryptedDataDetails();
      final List<DynaTraceMetricDataResponse> metricDataResponses = new ArrayList<>();
      List<Callable<DynaTraceMetricDataResponse>> callables = new ArrayList<>();
      switch (dataCollectionInfo.getAnalysisComparisonStrategy()) {
        case COMPARE_WITH_PREVIOUS:
          for (DynaTraceTimeSeries timeSeries : dataCollectionInfo.getTimeSeriesDefinitions()) {
            callables.add(() -> {
              DynaTraceMetricDataRequest dataRequest = DynaTraceMetricDataRequest.builder()
                                                           .timeseriesId(timeSeries.getTimeseriesId())
                                                           .entities(dataCollectionInfo.getServiceMethods())
                                                           .aggregationType(timeSeries.getAggregationType())
                                                           .percentile(timeSeries.getPercentile())
                                                           .startTimestamp(collectionStartTime)
                                                           .endTimestamp(System.currentTimeMillis())
                                                           .build();

              DynaTraceMetricDataResponse metricDataResponse =
                  dynaTraceDelegateService.fetchMetricData(dynaTraceConfig, dataRequest, encryptionDetails);
              metricDataResponse.getResult().setHost(DynatraceState.TEST_HOST_NAME);
              return metricDataResponse;
            });
          }
          break;
        case COMPARE_WITH_CURRENT:
          final long startTime = collectionStartTime;
          final long endTime = System.currentTimeMillis();

          for (int i = 0; i <= CANARY_DAYS_TO_COLLECT; i++) {
            String hostName = i == 0 ? DynatraceState.TEST_HOST_NAME : DynatraceState.CONTROL_HOST_NAME;
            long startTimeStamp = startTime - TimeUnit.DAYS.toMillis(i);
            long endTimeStamp = endTime - TimeUnit.DAYS.toMillis(i);
            for (DynaTraceTimeSeries timeSeries : dataCollectionInfo.getTimeSeriesDefinitions()) {
              callables.add(() -> {
                DynaTraceMetricDataRequest dataRequest = DynaTraceMetricDataRequest.builder()
                                                             .timeseriesId(timeSeries.getTimeseriesId())
                                                             .entities(dataCollectionInfo.getServiceMethods())
                                                             .aggregationType(timeSeries.getAggregationType())
                                                             .percentile(timeSeries.getPercentile())
                                                             .startTimestamp(startTimeStamp)
                                                             .endTimestamp(endTimeStamp)
                                                             .build();

                DynaTraceMetricDataResponse metricDataResponse =
                    dynaTraceDelegateService.fetchMetricData(dynaTraceConfig, dataRequest, encryptionDetails);
                metricDataResponse.getResult().setHost(hostName);
                return metricDataResponse;
              });
            }
          }
          break;

        default:
          throw new WingsException("invalid strategy " + dataCollectionInfo.getAnalysisComparisonStrategy());
      }

      logger.info("fetching dynatrace metrics for {} strategy {} for min {}", dataCollectionInfo.getStateExecutionId(),
          dataCollectionInfo.getAnalysisComparisonStrategy(), dataCollectionMinute);
      List<Optional<DynaTraceMetricDataResponse>> results = executeParrallel(callables);
      logger.info("done fetching dynatrace metrics for {} strategy {} for min {}",
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getAnalysisComparisonStrategy(),
          dataCollectionMinute);
      results.forEach(result -> {
        if (result.isPresent()) {
          metricDataResponses.add(result.get());
        }
      });
      return metricDataResponses;
    }

    private TreeBasedTable<String, Long, NewRelicMetricDataRecord> processMetricData(
        List<DynaTraceMetricDataResponse> metricsData) {
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();
      metricsData.forEach(dataResponse -> {
        String timeSeriesId = dataResponse.getResult().getTimeseriesId();
        dataResponse.getResult().getEntities().forEach((serviceMethodName, serviceMethodDesc) -> {
          String btName = serviceMethodDesc + ":" + serviceMethodName;

          List<List<Double>> dataPoints = dataResponse.getResult().getDataPoints().get(serviceMethodName);

          dataPoints.forEach(dataPoint -> {
            Double timeStamp = dataPoint.get(0);
            Double value = dataPoint.get(1);

            if (value != null) {
              DynaTraceTimeSeries timeSeries = DynaTraceTimeSeries.getTimeSeries(timeSeriesId);
              Preconditions.checkNotNull(timeSeries, "could not find timeSeries " + timeSeriesId);

              NewRelicMetricDataRecord metricDataRecord = records.get(btName, timeStamp.longValue());
              if (metricDataRecord == null) {
                metricDataRecord = NewRelicMetricDataRecord.builder()
                                       .name(btName)
                                       .applicationId(dataCollectionInfo.getApplicationId())
                                       .workflowId(dataCollectionInfo.getWorkflowId())
                                       .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                                       .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                                       .serviceId(dataCollectionInfo.getServiceId())
                                       .dataCollectionMinute(dataCollectionMinute)
                                       .timeStamp(timeStamp.longValue())
                                       .stateType(StateType.DYNA_TRACE)
                                       .host(dataResponse.getResult().getHost())
                                       .build();
                records.put(btName, timeStamp.longValue(), metricDataRecord);
              }
              try {
                Field f = NewRelicMetricDataRecord.class.getDeclaredField(timeSeries.getSavedFieldName());
                f.setAccessible(true);
                f.set(metricDataRecord, value);
              } catch (NoSuchFieldException e) {
                throw new WingsException(e);
              } catch (IllegalAccessException e) {
                throw new WingsException(e);
              }
            }
          });
        });
      });
      return records;
    }

    private List<NewRelicMetricDataRecord> getAllMetricRecords(
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) {
      List<NewRelicMetricDataRecord> rv = new ArrayList<>();
      records.cellSet().forEach(cell -> rv.add(cell.getValue()));
      return rv;
    }
  }
}

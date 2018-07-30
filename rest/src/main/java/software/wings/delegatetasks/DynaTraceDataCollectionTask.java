package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.base.Preconditions;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.time.Timestamp;
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
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
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
    private final long collectionStartMinute;

    private DynaTraceMetricCollector(
        DynaTraceDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartMinute = Timestamp.currentMinuteBoundary();
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
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
                    .appId(dataCollectionInfo.getApplicationId())
                    .workflowId(dataCollectionInfo.getWorkflowId())
                    .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                    .serviceId(dataCollectionInfo.getServiceId())
                    .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                    .dataCollectionMinute(dataCollectionMinute)
                    .timeStamp(collectionStartTime)
                    .level(ClusterLevel.H0)
                    .groupName(DEFAULT_GROUP_NAME)
                    .build());

            records.putAll(processMetricData(metricsData));
            List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(records);
            if (!saveMetrics(dataCollectionInfo.getDynaTraceConfig().getAccountId(),
                    dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
              logger.error("Error saving metrics to the database. DatacollectionMin: {} StateexecutionId: {}",
                  dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
            } else {
              logger.info("Sent {} Dynatrace metric records to the server for minute {}", recordsToSave.size(),
                  dataCollectionMinute);
            }

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
                  taskResult.setErrorMessage(Misc.getMessage(ex));
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
              DynaTraceMetricDataRequest dataRequest =
                  DynaTraceMetricDataRequest.builder()
                      .timeseriesId(timeSeries.getTimeseriesId())
                      .entities(dataCollectionInfo.getServiceMethods())
                      .aggregationType(timeSeries.getAggregationType())
                      .percentile(timeSeries.getPercentile())
                      .startTimestamp(collectionStartTime)
                      .endTimestamp(collectionStartTime + TimeUnit.MINUTES.toMillis(2))
                      .build();

              DynaTraceMetricDataResponse metricDataResponse = dynaTraceDelegateService.fetchMetricData(dynaTraceConfig,
                  dataRequest, encryptionDetails, createApiCallLog(dataCollectionInfo.getStateExecutionId()));
              metricDataResponse.getResult().setHost(DynatraceState.TEST_HOST_NAME);
              return metricDataResponse;
            });
          }
          break;
        case COMPARE_WITH_CURRENT:
          final long startTime = collectionStartTime;
          final long endTime = collectionStartTime + TimeUnit.MINUTES.toMillis(2);

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
                    dynaTraceDelegateService.fetchMetricData(dynaTraceConfig, dataRequest, encryptionDetails,
                        createApiCallLog(dataCollectionInfo.getStateExecutionId()));
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
                metricDataRecord =
                    NewRelicMetricDataRecord.builder()
                        .name(btName)
                        .appId(dataCollectionInfo.getApplicationId())
                        .workflowId(dataCollectionInfo.getWorkflowId())
                        .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                        .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                        .serviceId(dataCollectionInfo.getServiceId())
                        .dataCollectionMinute(
                            (int) ((Timestamp.minuteBoundary(timeStamp.longValue()) - collectionStartMinute)
                                / TimeUnit.MINUTES.toMillis(1)))
                        .timeStamp(timeStamp.longValue())
                        .stateType(StateType.DYNA_TRACE)
                        .host(dataResponse.getResult().getHost())
                        .values(new HashMap<>())
                        .groupName(DEFAULT_GROUP_NAME)
                        .build();
                if (metricDataRecord.getTimeStamp() >= dataCollectionInfo.getStartTime()) {
                  records.put(btName, timeStamp.longValue(), metricDataRecord);
                } else {
                  logger.info("Metric record for stateExecutionId {} is before the startTime. Ignoring.",
                      dataCollectionInfo.getStateExecutionId());
                }
              }

              metricDataRecord.getValues().put(timeSeries.getSavedFieldName(), value);
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

package software.wings.delegatetasks;

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
import software.wings.service.impl.newrelic.NewRelicMetricData;
import software.wings.service.impl.newrelic.NewRelicMetricData.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicMetricData.NewRelicMetricTimeSlice;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricType;
import software.wings.service.impl.newrelic.NewRelicWebTransactions;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rsingh on 5/18/17.
 */
public class NewRelicDataCollectionTask extends AbstractDelegateRunnableTask<DataCollectionTaskResult> {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicDataCollectionTask.class);
  private static final int DURATION_TO_ASK_MINUTES = 5;
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);
  private ScheduledExecutorService collectionService;

  @Inject private NewRelicDelegateService newRelicDelegateService;

  @Inject private MetricDataStoreService metricStoreService;

  public NewRelicDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public DataCollectionTaskResult run(Object[] parameters) {
    final NewRelicDataCollectionInfo dataCollectionInfo = (NewRelicDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}" + dataCollectionInfo);
    try {
      collectionService = scheduleMetricDataCollection(dataCollectionInfo);
    } catch (IOException e) {
      throw new WingsException(e);
    }
    logger.info("going to collect appdynamics data for " + dataCollectionInfo);

    synchronized (lockObject) {
      while (!completed.get()) {
        try {
          lockObject.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    return DataCollectionTaskResult.builder().status(DataCollectionTaskStatus.SUCCESS).build();
  }

  private ScheduledExecutorService scheduleMetricDataCollection(NewRelicDataCollectionInfo dataCollectionInfo)
      throws IOException {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new NewRelicMetricCollector(dataCollectionInfo), 0, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private void shutDownCollection() {
    collectionService.shutdown();
    completed.set(true);
    synchronized (lockObject) {
      lockObject.notifyAll();
    }
  }

  private class NewRelicMetricCollector implements Runnable {
    private final NewRelicDataCollectionInfo dataCollectionInfo;
    private final List<NewRelicApplicationInstance> instances;
    private long collectionStartTime;
    private int logCollectionMinute = 0;

    private NewRelicMetricCollector(NewRelicDataCollectionInfo dataCollectionInfo) throws IOException {
      this.dataCollectionInfo = dataCollectionInfo;
      this.instances = newRelicDelegateService.getApplicationInstances(
          dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId());
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime())
          - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
    }

    @Override
    public void run() {
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        shutDownCollection();
        return;
      }
      try {
        final long endTime = collectionStartTime + TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
        for (NewRelicApplicationInstance node : instances) {
          List<NewRelicMetricDataRecord> records = new ArrayList<>();
          for (NewRelicMetricType metricType : NewRelicMetricType.values()) {
            try {
              NewRelicMetricData metricData = newRelicDelegateService.getMetricData(
                  dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
                  metricType.getMetricName(), metricType.getValuesToCollect(), collectionStartTime, endTime);

              for (NewRelicMetric metric : metricData.getMetrics()) {
                for (NewRelicMetricTimeSlice timeslice : metric.getTimeslices()) {
                  final NewRelicMetricDataRecord metricDataRecord = new NewRelicMetricDataRecord();
                  metricDataRecord.setMetricType(metricType);
                  metricDataRecord.setWorkflowId(dataCollectionInfo.getWorkflowId());
                  metricDataRecord.setWorkflowExecutionId(dataCollectionInfo.getWorkflowExecutionId());
                  metricDataRecord.setServiceId(dataCollectionInfo.getServiceId());
                  metricDataRecord.setStateExecutionId(dataCollectionInfo.getStateExecutionId());

                  // set from time to the timestamp
                  metricDataRecord.setTimeStamp(
                      TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeslice.getFrom()).toEpochSecond()));
                  metricDataRecord.setHost(node.getHost());

                  switch (metricType) {
                    case APDEX:
                      final String appdexJson = JsonUtils.asJson(timeslice.getValues());
                      metricDataRecord.setApdexValue(JsonUtils.asObject(appdexJson, NewRelicApdex.class));
                      break;
                    case ERRORS:
                      final String errorsJson = JsonUtils.asJson(timeslice.getValues());
                      metricDataRecord.setErrors(JsonUtils.asObject(errorsJson, NewRelicErrors.class));
                      break;
                    case WEB_TRANSACTION:
                      final String webTxnJson = JsonUtils.asJson(timeslice.getValues());
                      metricDataRecord.setWebTransactions(
                          JsonUtils.asObject(webTxnJson, NewRelicWebTransactions.class));
                      break;
                    default:
                      throw new IllegalStateException("invalid metricType: " + metricType);
                  }

                  records.add(metricDataRecord);
                }
              }
            } catch (Exception e) {
              logger.warn("Error fetching metrics for node: " + node + ", metric: " + metricType.getMetricName(), e);
            }
          }
          logger.debug(records.toString());
          metricStoreService.saveNewRelicMetrics(
              dataCollectionInfo.getNewRelicConfig().getAccountId(), dataCollectionInfo.getApplicationId(), records);
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Exception e) {
        logger.error("error fetching appdynamics metrics", e);
      }
    }
  }
}

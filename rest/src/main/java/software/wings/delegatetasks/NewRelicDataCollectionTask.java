package software.wings.delegatetasks;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

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
  private static final int METRIC_DATA_QUERY_BATCH_SIZE = 50;
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
    logger.info("going to collect new relic data for " + dataCollectionInfo);

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
    private final List<NewRelicMetric> metrics;
    private int logCollectionMinute = 0;

    private NewRelicMetricCollector(NewRelicDataCollectionInfo dataCollectionInfo) throws IOException {
      this.dataCollectionInfo = dataCollectionInfo;
      this.instances = newRelicDelegateService.getApplicationInstances(
          dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId());
      this.metrics = newRelicDelegateService.getMetricsNameToCollect(
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
          TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

          List<List<String>> metricBatches = batchMetricsToCollect();

          for (List<String> metricNames : metricBatches) {
            try {
              NewRelicMetricData metricData =
                  newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
                      dataCollectionInfo.getNewRelicAppId(), node.getId(), metricNames, collectionStartTime, endTime);

              for (NewRelicMetricSlice metric : metricData.getMetrics()) {
                for (NewRelicMetricTimeSlice timeslice : metric.getTimeslices()) {
                  final NewRelicMetricDataRecord metricDataRecord = new NewRelicMetricDataRecord();
                  metricDataRecord.setName(metric.getName());
                  metricDataRecord.setWorkflowId(dataCollectionInfo.getWorkflowId());
                  metricDataRecord.setWorkflowExecutionId(dataCollectionInfo.getWorkflowExecutionId());
                  metricDataRecord.setServiceId(dataCollectionInfo.getServiceId());
                  metricDataRecord.setStateExecutionId(dataCollectionInfo.getStateExecutionId());

                  // set from time to the timestamp
                  long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeslice.getFrom()).toEpochSecond());
                  metricDataRecord.setTimeStamp(timeStamp);
                  metricDataRecord.setHost(node.getHost());

                  final String webTxnJson = JsonUtils.asJson(timeslice.getValues());
                  metricDataRecord.setWebTransactions(JsonUtils.asObject(webTxnJson, NewRelicWebTransactions.class));

                  records.put(metric.getName(), timeStamp, metricDataRecord);
                }
              }
            } catch (Exception e) {
              logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
            }

            // get error metrics
            try {
              NewRelicMetricData metricData = newRelicDelegateService.getMetricData(
                  dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
                  getErrorMetricNames(metricNames), collectionStartTime, endTime);
              for (NewRelicMetricSlice metric : metricData.getMetrics()) {
                for (NewRelicMetricTimeSlice timeslice : metric.getTimeslices()) {
                  long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeslice.getFrom()).toEpochSecond());
                  String metricName = metric.getName().replace("Errors/", "");

                  NewRelicMetricDataRecord metricDataRecord = records.get(metricName, timeStamp);
                  if (metricDataRecord != null) {
                    final String errorsJson = JsonUtils.asJson(timeslice.getValues());
                    metricDataRecord.setErrors(JsonUtils.asObject(errorsJson, NewRelicErrors.class));
                  }
                }
              }

            } catch (Exception e) {
              logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
            }

            // get apdex metrics
            try {
              NewRelicMetricData metricData = newRelicDelegateService.getMetricData(
                  dataCollectionInfo.getNewRelicConfig(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
                  getApdexMetricNames(metricNames), collectionStartTime, endTime);
              for (NewRelicMetricSlice metric : metricData.getMetrics()) {
                for (NewRelicMetricTimeSlice timeslice : metric.getTimeslices()) {
                  long timeStamp = TimeUnit.SECONDS.toMillis(OffsetDateTime.parse(timeslice.getFrom()).toEpochSecond());
                  String metricName = metric.getName().replace("Apdex", "WebTransaction");

                  NewRelicMetricDataRecord metricDataRecord = records.get(metricName, timeStamp);
                  if (metricDataRecord != null) {
                    final String apdexJson = JsonUtils.asJson(timeslice.getValues());
                    metricDataRecord.setApdexValue(JsonUtils.asObject(apdexJson, NewRelicApdex.class));
                  }
                }
              }

            } catch (Exception e) {
              logger.warn("Error fetching metrics for node: " + node + ", metrics: " + metricNames, e);
            }
            logger.debug(records.toString());
            metricStoreService.saveNewRelicMetrics(dataCollectionInfo.getNewRelicConfig().getAccountId(),
                dataCollectionInfo.getApplicationId(), getAllMetricRecords(records));
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Exception e) {
        logger.error("error fetching new relic metrics", e);
      }
    }

    private List<String> getApdexMetricNames(List<String> metricNames) {
      final List<String> rv = new ArrayList<>();
      for (String metricName : metricNames) {
        rv.add(metricName.replace("WebTransaction", "Apdex"));
      }

      return rv;
    }

    private List<String> getErrorMetricNames(List<String> metricNames) {
      final List<String> rv = new ArrayList<>();
      for (String metricName : metricNames) {
        rv.add("Errors/" + metricName);
      }

      return rv;
    }

    private List<List<String>> batchMetricsToCollect() {
      List<List<String>> rv = new ArrayList<>();

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
        rv.add(cell.getValue());
      }

      return rv;
    }
  }
}

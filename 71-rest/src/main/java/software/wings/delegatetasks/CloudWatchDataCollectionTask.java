package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchDataCollectionInfo;
import software.wings.service.impl.cloudwatch.CloudWatchDelegateServiceImpl;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 5/18/17.
 */
public class CloudWatchDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(CloudWatchDataCollectionTask.class);
  private static final int DURATION_TO_ASK_MINUTES = 15;
  private static final int CANARY_DAYS_TO_COLLECT = 7;
  private CloudWatchDataCollectionInfo dataCollectionInfo;

  @Inject private MetricDataStoreService metricStoreService;
  @Inject private CloudWatchDelegateServiceImpl cloudWatchDelegateService;

  public CloudWatchDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (CloudWatchDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.CLOUD_WATCH)
        .build();
  }

  @Override
  protected StateType getStateType() {
    return StateType.CLOUD_WATCH;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new CloudWatchMetricCollector(dataCollectionInfo, taskResult);
  }

  private class CloudWatchMetricCollector implements Runnable {
    private final CloudWatchDataCollectionInfo dataCollectionInfo;
    private final DataCollectionTaskResult taskResult;
    private long collectionStartTime;
    private int dataCollectionMinute;

    private CloudWatchMetricCollector(
        CloudWatchDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
          - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
    }

    @Override
    public void run() {
      encryptionService.decrypt(dataCollectionInfo.getAwsConfig(), dataCollectionInfo.getEncryptedDataDetails());
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
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
                    .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                    .dataCollectionMinute(dataCollectionMinute)
                    .timeStamp(collectionStartTime)
                    .level(ClusterLevel.H0)
                    .groupName(DEFAULT_GROUP_NAME)
                    .build());

            List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(metricDataRecords);
            if (!saveMetrics(dataCollectionInfo.getAwsConfig().getAccountId(), dataCollectionInfo.getApplicationId(),
                    dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
              retry = RETRIES;
              taskResult.setErrorMessage(
                  "Cannot save new cloud watch metric records to Harness. Server returned error");
              throw new RuntimeException(
                  "Cannot save new cloud watch metric records to Harness. Server returned error");
            }
            logger.info("Sent {} cloud watch metric records to the server for minute {}", recordsToSave.size(),
                dataCollectionMinute);

            dataCollectionMinute++;
            collectionStartTime += TimeUnit.MINUTES.toMillis(1);
            if (dataCollectionMinute >= dataCollectionInfo.getCollectionTime()) {
              // We are done with all data collection, so setting task status to success and quitting.
              logger.info(
                  "Completed CloudWatch collection task. So setting task status to success and quitting. StateExecutionId {}",
                  dataCollectionInfo.getStateExecutionId());
              completed.set(true);
              taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
            }
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
              logger.warn("error fetching cloud watch metrics for minute " + dataCollectionMinute + ". retrying in "
                      + RETRY_SLEEP + "s",
                  ex);
              sleep(RETRY_SLEEP);
            }
          }
        }
      } catch (RuntimeException e) {
        completed.set(true);
        taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage("error fetching cloud watch metrics for minute " + dataCollectionMinute);
        logger.error("error fetching cloud watch metrics for minute " + dataCollectionMinute, e);
      }

      if (completed.get()) {
        logger.info("Shutting down cloud watch data collection");
        shutDownCollection();
        return;
      }
    }

    public TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricsData() throws IOException {
      final TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricDataResponses = TreeBasedTable.create();
      List<Callable<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> callables = new ArrayList<>();
      AmazonCloudWatchClient cloudWatchClient =
          (AmazonCloudWatchClient) AmazonCloudWatchClientBuilder.standard()
              .withRegion(dataCollectionInfo.getRegion())
              .withCredentials(new AWSStaticCredentialsProvider(
                  new BasicAWSCredentials(dataCollectionInfo.getAwsConfig().getAccessKey(),
                      String.valueOf(dataCollectionInfo.getAwsConfig().getSecretKey()))))
              .build();

      dataCollectionInfo.getLoadBalancerMetrics().forEach(
          (loadBalancerName, cloudWatchMetrics) -> cloudWatchMetrics.forEach(cloudWatchMetric -> {
            callables.add(
                ()
                    -> cloudWatchDelegateService.getMetricDataRecords(AwsNameSpace.ELB, cloudWatchClient,
                        cloudWatchMetric, loadBalancerName, DEFAULT_GROUP_NAME, dataCollectionInfo, getAppId(),
                        System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES),
                        System.currentTimeMillis(), createApiCallLog(dataCollectionInfo.getStateExecutionId())));
          }));

      dataCollectionInfo.getHosts().forEach(
          (host, groupName)
              -> dataCollectionInfo.getEc2Metrics().forEach(cloudWatchMetric
                  -> callables.add(
                      ()
                          -> cloudWatchDelegateService.getMetricDataRecords(AwsNameSpace.EC2, cloudWatchClient,
                              cloudWatchMetric, host, groupName, dataCollectionInfo, getAppId(),
                              System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES),
                              System.currentTimeMillis(),
                              createApiCallLog(dataCollectionInfo.getStateExecutionId())))));

      logger.info("fetching cloud watch metrics for {} strategy {} for min {}",
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getAnalysisComparisonStrategy(),
          dataCollectionMinute);
      List<Optional<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> results = executeParrallel(callables);
      logger.info("done fetching cloud watch metrics for {} strategy {} for min {}",
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getAnalysisComparisonStrategy(),
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

    private List<NewRelicMetricDataRecord> getAllMetricRecords(
        TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) {
      List<NewRelicMetricDataRecord> rv = new ArrayList<>();
      records.cellSet().forEach(cell -> rv.add(cell.getValue()));
      return rv;
    }
  }
}

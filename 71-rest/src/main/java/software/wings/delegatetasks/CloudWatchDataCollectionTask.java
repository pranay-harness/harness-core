package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DURATION_TO_ASK_MINUTES;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.beans.TaskType;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.aws.delegate.AwsLambdaHelperServiceDelegateImpl;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchDataCollectionInfo;
import software.wings.service.impl.cloudwatch.CloudWatchDelegateServiceImpl;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 5/18/17.
 */
@Slf4j
public class CloudWatchDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private CloudWatchDataCollectionInfo dataCollectionInfo;

  @Inject private MetricDataStoreService metricStoreService;
  @Inject private CloudWatchDelegateServiceImpl cloudWatchDelegateService;
  @Inject private AwsLambdaHelperServiceDelegateImpl awsLambdaHelperServiceDelegate;

  public CloudWatchDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    dataCollectionInfo = (CloudWatchDataCollectionInfo) parameters;
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
  protected boolean is24X7Task() {
    return getTaskType().equals(TaskType.CLOUD_WATCH_COLLECT_24_7_METRIC_DATA.name());
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new CloudWatchMetricCollector(dataCollectionInfo, taskResult, is24X7Task());
  }

  private class CloudWatchMetricCollector implements Runnable {
    private final CloudWatchDataCollectionInfo dataCollectionInfo;
    private final DataCollectionTaskResult taskResult;
    boolean is247Task;
    private long collectionStartTime;
    private int dataCollectionMinute;
    private Map<String, Long> hostStartTimeMap;

    private CloudWatchMetricCollector(
        CloudWatchDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult, boolean is247Task) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      this.is247Task = is247Task;
      this.hostStartTimeMap = new HashMap<>();
    }

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      encryptionService.decrypt(dataCollectionInfo.getAwsConfig(), dataCollectionInfo.getEncryptedDataDetails());
      int retry = 0;
      while (!completed.get() && retry < RETRIES) {
        try {
          logger.info("Running data collection for {} for minute {}", dataCollectionInfo.getStateExecutionId(),
              dataCollectionMinute);
          dataCollectionInfo.setDataCollectionMinute(dataCollectionMinute);

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
                  .dataCollectionMinute(getDataCollectionMinuteForHeartbeat(is247Task))
                  .timeStamp(collectionStartTime)
                  .level(ClusterLevel.H0)
                  .groupName(DEFAULT_GROUP_NAME)
                  .build());

          List<NewRelicMetricDataRecord> recordsToSave = getAllMetricRecords(metricDataRecords);
          if (!saveMetrics(dataCollectionInfo.getAwsConfig().getAccountId(), dataCollectionInfo.getApplicationId(),
                  dataCollectionInfo.getStateExecutionId(), recordsToSave)) {
            retry = RETRIES;
            taskResult.setErrorMessage("Cannot save new CloudWatch metric records to Harness");
            throw new RuntimeException("Cannot save new CloudWatch metric records to Harness");
          }
          logger.info("Sent {} CloudWatch metric records to the server for minute {}.", recordsToSave.size(),
              getDataCollectionMinuteForHeartbeat(is247Task));

          dataCollectionMinute++;
          collectionStartTime += TimeUnit.MINUTES.toMillis(1);
          if (dataCollectionMinute >= dataCollectionInfo.getCollectionTime() || is247Task) {
            // We are done with all data collection, so setting task status to success and quitting.
            logger.info(
                "Completed CloudWatch collection task, so setting task status to success and quitting for StateExecutionId {}",
                dataCollectionInfo.getStateExecutionId());
            completed.set(true);
            taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
          }
          break;
        } catch (Throwable ex) {
          if (!(ex instanceof Exception) || ++retry >= RETRIES) {
            logger.error("Error fetching metrics for {} for minute {}", dataCollectionInfo.getStateExecutionId(),
                dataCollectionMinute, ex);
            taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
            completed.set(true);
            break;
          } else {
            if (retry == 1) {
              taskResult.setErrorMessage(ExceptionUtils.getMessage(ex));
            }
            logger.warn("Error fetching CloudWatch metrics for minute " + dataCollectionMinute + ". retrying in "
                    + RETRY_SLEEP + "s",
                ex);
            sleep(RETRY_SLEEP);
          }
        }
      }
      if (taskResult.getStatus() == DataCollectionTaskStatus.FAILURE) {
        completed.set(true);
        taskResult.setErrorMessage("Error fetching cloud watch metrics for minute " + dataCollectionMinute);
        logger.error("Error fetching CloudWatch metrics for minute " + dataCollectionMinute);
      }

      if (completed.get()) {
        logger.info("Shutting down CloudWatch data collection");
        shutDownCollection();
        return;
      }
    }

    private int getDataCollectionMinuteForHeartbeat(boolean is247Task) {
      int collectionMin = dataCollectionMinute;
      if (is247Task) {
        collectionMin = (int) TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getStartTime())
            + dataCollectionInfo.getCollectionTime();
      }
      return collectionMin;
    }

    public TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricsData() throws IOException {
      final TreeBasedTable<String, Long, NewRelicMetricDataRecord> metricDataResponses = TreeBasedTable.create();
      List<Callable<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> callables = new ArrayList<>();

      AmazonCloudWatchClientBuilder clientBuilder =
          AmazonCloudWatchClientBuilder.standard().withRegion(dataCollectionInfo.getRegion());

      if (dataCollectionInfo.getAwsConfig().isUseEc2IamCredentials()) {
        logger.info("isUseEc2IamCredentials is true. Instantiating EC2ContainerCredentialsProviderWrapper");
        clientBuilder.withCredentials(new EC2ContainerCredentialsProviderWrapper());
      } else {
        clientBuilder.withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(dataCollectionInfo.getAwsConfig().getAccessKey(),
                String.valueOf(dataCollectionInfo.getAwsConfig().getSecretKey()))));
      }

      AmazonCloudWatchClient cloudWatchClient = (AmazonCloudWatchClient) clientBuilder.build();

      Map<String, List<CloudWatchMetric>> cloudWatchMetricsByLambdaFunction =
          dataCollectionInfo.getLambdaFunctionNames();
      Map<String, List<CloudWatchMetric>> cloudWatchMetricsByECSClusterName =
          dataCollectionInfo.getMetricsByECSClusterName();
      Map<String, List<CloudWatchMetric>> cloudWatchMetricsByELBName = dataCollectionInfo.getLoadBalancerMetrics();

      if (isNotEmpty(cloudWatchMetricsByECSClusterName)) {
        logger.info("for {} fetching metrics for ECS Cluster {}", dataCollectionInfo.getStateExecutionId(),
            cloudWatchMetricsByECSClusterName);
        addCallablesForGetMetricData(cloudWatchClient, callables, AwsNameSpace.ECS, cloudWatchMetricsByECSClusterName);
      }
      if (isNotEmpty(cloudWatchMetricsByLambdaFunction)) {
        logger.info("for {} fetching metrics for lambda functions {}", dataCollectionInfo.getStateExecutionId(),
            cloudWatchMetricsByLambdaFunction);
        addCallablesForGetMetricData(
            cloudWatchClient, callables, AwsNameSpace.LAMBDA, cloudWatchMetricsByLambdaFunction);
      }
      if (isNotEmpty(cloudWatchMetricsByELBName)) {
        logger.info("for {} fetching metrics for load balancers {}", dataCollectionInfo.getStateExecutionId(),
            cloudWatchMetricsByELBName);
        addCallablesForGetMetricData(cloudWatchClient, callables, AwsNameSpace.ELB, cloudWatchMetricsByELBName);
      }
      if (isNotEmpty(dataCollectionInfo.getHosts()) && isNotEmpty(dataCollectionInfo.getEc2Metrics())) {
        logger.info("for {} fetching {} metrics for hosts {}", dataCollectionInfo.getStateExecutionId(),
            dataCollectionInfo.getEc2Metrics(), dataCollectionInfo.getHosts());
        dataCollectionInfo.getHosts().forEach(
            (host, groupName)
                -> dataCollectionInfo.getEc2Metrics().forEach(cloudWatchMetric
                    -> callables.add(
                        ()
                            -> cloudWatchDelegateService.getMetricDataRecords(AwsNameSpace.EC2, cloudWatchClient,
                                cloudWatchMetric, host, groupName, dataCollectionInfo, getAppId(),
                                collectionStartTime - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES),
                                collectionStartTime, createApiCallLog(dataCollectionInfo.getStateExecutionId()),
                                is247Task, hostStartTimeMap))));
      }
      logger.info("Fetching CloudWatch metrics for {} strategy {} for min {}", dataCollectionInfo.getStateExecutionId(),
          dataCollectionInfo.getAnalysisComparisonStrategy(), dataCollectionMinute);
      List<Optional<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> results = executeParallel(callables);
      logger.info("Done fetching CloudWatch metrics for {} strategy {} for min {}",
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

    private void addCallablesForGetMetricData(AmazonCloudWatchClient cloudWatchClient,
        List<Callable<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> callables, AwsNameSpace nameSpace,
        Map<String, List<CloudWatchMetric>> cloudWatchMetricsByType) {
      cloudWatchMetricsByType.forEach((entityType, cloudWatchMetrics) -> cloudWatchMetrics.forEach(cloudWatchMetric -> {
        callables.add(()
                          -> cloudWatchDelegateService.getMetricDataRecords(nameSpace, cloudWatchClient,
                              cloudWatchMetric, entityType, DEFAULT_GROUP_NAME, dataCollectionInfo, getAppId(),
                              System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES),
                              System.currentTimeMillis(), createApiCallLog(dataCollectionInfo.getStateExecutionId()),
                              is247Task, hostStartTimeMap));
      }));
    }
  }
}

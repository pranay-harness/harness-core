package software.wings.service.impl.cloudwatch;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.states.DynatraceState.CONTROL_HOST_NAME;
import static software.wings.sm.states.DynatraceState.TEST_HOST_NAME;

import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.cloudwatch.CloudWatchDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pranjal on 09/04/2018
 */
public class CloudWatchDelegateServiceImpl implements CloudWatchDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(CloudWatchDelegateServiceImpl.class);
  private static final int DURATION_TO_ASK_MINUTES = 2;
  private static final int CANARY_DAYS_TO_COLLECT = 7;

  @Inject private DataCollectionExecutorService dataCollectionService;
  @Inject private EncryptionService encryptionService;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(final AwsConfig config,
      List<EncryptedDataDetail> encryptionDetails, CloudWatchSetupTestNodeData setupTestNodeData,
      ThirdPartyApiCallLog thirdPartyApiCallLog, String hostName) throws IOException {
    logger.info("Initiating getMetricsWithDataForNode for hostname : " + hostName
        + " setupTestNodeData : " + setupTestNodeData);
    List<Callable<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> callables = new ArrayList<>();

    encryptionService.decrypt(config, encryptionDetails);

    AmazonCloudWatchClient cloudWatchClient =
        (AmazonCloudWatchClient) AmazonCloudWatchClientBuilder.standard()
            .withRegion(setupTestNodeData.getRegion())
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(config.getAccessKey(), String.valueOf(config.getSecretKey()))))
            .build();

    if (!isEmpty(setupTestNodeData.getLoadBalancerMetricsByLBName())) {
      setupTestNodeData.getLoadBalancerMetricsByLBName().forEach(
          (loadBalancerName, cloudWatchMetrics) -> cloudWatchMetrics.forEach(cloudWatchMetric -> {
            callables.add(
                ()
                    -> getMetricDataRecords(AwsNameSpace.ELB, cloudWatchClient, cloudWatchMetric, loadBalancerName,
                        DEFAULT_GROUP_NAME,
                        CloudWatchDataCollectionInfo.builder()
                            .analysisComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
                            .build(),
                        setupTestNodeData.getAppId(), setupTestNodeData.getFromTime(), setupTestNodeData.getToTime()));
          }));
    }

    List<Optional<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> lBMetricsResults =
        dataCollectionService.executeParrallel(callables);
    List<NewRelicMetricDataRecord> metricDataRecordsForLBMetrics = new ArrayList<>();

    lBMetricsResults.forEach(metricDataRecordsOptional -> {
      if (metricDataRecordsOptional.isPresent()) {
        metricDataRecordsForLBMetrics.addAll(metricDataRecordsOptional.get().values());
      }
    });

    if (!isEmpty(setupTestNodeData.getEc2Metrics())) {
      setupTestNodeData.getEc2Metrics().forEach(cloudWatchMetric
          -> callables.add(
              ()
                  -> getMetricDataRecords(AwsNameSpace.EC2, cloudWatchClient, cloudWatchMetric, hostName,
                      NewRelicMetricDataRecord.DEFAULT_GROUP_NAME,
                      CloudWatchDataCollectionInfo.builder()
                          .analysisComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
                          .build(),
                      setupTestNodeData.getAppId(), setupTestNodeData.getFromTime(), setupTestNodeData.getToTime())));
    }

    List<Optional<TreeBasedTable<String, Long, NewRelicMetricDataRecord>>> hostMetricsResults =
        dataCollectionService.executeParrallel(callables);
    List<NewRelicMetricDataRecord> metricDataRecordsForHostMetrics = new ArrayList<>();
    hostMetricsResults.forEach(metricDataRecordsOptional -> {
      if (metricDataRecordsOptional.isPresent()) {
        metricDataRecordsForHostMetrics.addAll(metricDataRecordsOptional.get().values());
      }
    });

    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder()
                          .loadResponse(metricDataRecordsForLBMetrics)
                          .isLoadPresent(!metricDataRecordsForLBMetrics.isEmpty())
                          .build())
        .dataForNode(metricDataRecordsForHostMetrics)
        .build();
  }

  public TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricDataRecords(AwsNameSpace awsNameSpace,
      AmazonCloudWatchClient cloudWatchClient, CloudWatchMetric cloudWatchMetric, String dimensionValue,
      String groupName, CloudWatchDataCollectionInfo dataCollectionInfo, String appId, long startTime, long endTime) {
    TreeBasedTable<String, Long, NewRelicMetricDataRecord> rv = TreeBasedTable.create();
    switch (dataCollectionInfo.getAnalysisComparisonStrategy()) {
      case COMPARE_WITH_PREVIOUS:
        fetchMetrics(awsNameSpace, cloudWatchClient, cloudWatchMetric, dimensionValue, dimensionValue, groupName,
            startTime, endTime, appId, dataCollectionInfo.getDataCollectionMinute(), rv, null, null, null, null);
        break;

      case COMPARE_WITH_CURRENT:
        switch (awsNameSpace) {
          case EC2:
            fetchMetrics(awsNameSpace, cloudWatchClient, cloudWatchMetric, dimensionValue, dimensionValue, groupName,
                startTime, endTime, appId, dataCollectionInfo.getDataCollectionMinute(), rv,
                dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                dataCollectionInfo.getServiceId(), dataCollectionInfo.getStateExecutionId());
            break;
          case ELB:
            for (int i = 0; i <= CANARY_DAYS_TO_COLLECT; i++) {
              String hostName = i == 0 ? TEST_HOST_NAME : CONTROL_HOST_NAME + "-" + i;
              endTime = endTime - TimeUnit.DAYS.toMillis(i);
              startTime = startTime - TimeUnit.DAYS.toMillis(i);
              fetchMetrics(awsNameSpace, cloudWatchClient, cloudWatchMetric, dimensionValue, hostName, groupName,
                  startTime, endTime, appId, dataCollectionInfo.getDataCollectionMinute(), rv,
                  dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                  dataCollectionInfo.getServiceId(), dataCollectionInfo.getStateExecutionId());
            }
            break;
          default:
            throw new WingsException("Invalid name space " + awsNameSpace);
        }
        break;
      default:
        throw new WingsException("Invalid strategy " + dataCollectionInfo.getAnalysisComparisonStrategy());
    }
    return rv;
  }

  private void fetchMetrics(AwsNameSpace awsNameSpace, AmazonCloudWatchClient cloudWatchClient,
      CloudWatchMetric cloudWatchMetric, String dimensionValue, String host, String groupName, long startTime,
      long endTime, String appId, int dataCollectionMinute, TreeBasedTable<String, Long, NewRelicMetricDataRecord> rv,
      String workflowId, String workflowExecutionId, String serviceId, String stateExecutionId) {
    GetMetricStatisticsRequest metricStatisticsRequest = new GetMetricStatisticsRequest();
    metricStatisticsRequest.withNamespace(awsNameSpace.getNameSpace())
        .withMetricName(cloudWatchMetric.getMetricName())
        .withDimensions(new Dimension().withName(cloudWatchMetric.getDimension()).withValue(dimensionValue))
        .withStartTime(new Date(startTime))
        .withEndTime(new Date(endTime))
        .withPeriod(60)
        .withStatistics("Average");
    GetMetricStatisticsResult metricStatistics = cloudWatchClient.getMetricStatistics(metricStatisticsRequest);
    List<Datapoint> datapoints = metricStatistics.getDatapoints();
    String metricName = awsNameSpace == AwsNameSpace.EC2 ? "EC2 Metrics" : dimensionValue;
    datapoints.forEach(datapoint -> {
      NewRelicMetricDataRecord newRelicMetricDataRecord = NewRelicMetricDataRecord.builder()
                                                              .stateType(StateType.CLOUD_WATCH)
                                                              .appId(appId)
                                                              .name(metricName)
                                                              .workflowId(workflowId)
                                                              .workflowExecutionId(workflowExecutionId)
                                                              .serviceId(serviceId)
                                                              .stateExecutionId(stateExecutionId)
                                                              .timeStamp(datapoint.getTimestamp().getTime())
                                                              .dataCollectionMinute(dataCollectionMinute)
                                                              .host(host)
                                                              .groupName(groupName)
                                                              .tag(awsNameSpace.name())
                                                              .values(new HashMap<>())
                                                              .build();
      newRelicMetricDataRecord.getValues().put(cloudWatchMetric.getMetricName(), datapoint.getAverage());

      rv.put(dimensionValue, datapoint.getTimestamp().getTime(), newRelicMetricDataRecord);
    });
  }
}

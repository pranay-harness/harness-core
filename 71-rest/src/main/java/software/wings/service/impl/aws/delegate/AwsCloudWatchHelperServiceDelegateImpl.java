package software.wings.service.impl.aws.delegate;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricDataResult;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import io.harness.beans.ExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.request.AwsCloudWatchMetricDataRequest;
import software.wings.service.impl.aws.model.request.AwsCloudWatchStatisticsRequest;
import software.wings.service.impl.aws.model.response.AwsCloudWatchMetricDataResponse;
import software.wings.service.impl.aws.model.response.AwsCloudWatchStatisticsResponse;
import software.wings.service.intfc.aws.delegate.AwsCloudWatchHelperServiceDelegate;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class AwsCloudWatchHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsCloudWatchHelperServiceDelegate {
  @Override
  public AwsCloudWatchStatisticsResponse getMetricStatistics(AwsCloudWatchStatisticsRequest request) {
    final GetMetricStatisticsRequest getMetricStatisticsRequest =
        new GetMetricStatisticsRequest()
            .withNamespace(request.getNamespace())
            .withDimensions(request.getDimensions())
            .withStartTime(request.getStartTime())
            .withEndTime(request.getEndTime())
            .withPeriod(request.getPeriod())
            .withStatistics(request.getStatistics())
            .withExtendedStatistics(request.getExtendedStatistics())
            .withMetricName(request.getMetricName())
            .withUnit(request.getUnit());

    final GetMetricStatisticsResult metricStatisticResult = getMetricStatistics(
        getMetricStatisticsRequest, request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());

    return AwsCloudWatchStatisticsResponse.builder()
        .datapoints(metricStatisticResult.getDatapoints())
        .label(metricStatisticResult.getLabel())
        .executionStatus(ExecutionStatus.SUCCESS)
        .build();
  }

  @Override
  public AwsCloudWatchMetricDataResponse getMetricData(AwsCloudWatchMetricDataRequest request) {
    GetMetricDataRequest metricDataRequest = new GetMetricDataRequest()
                                                 .withStartTime(request.getStartTime())
                                                 .withEndTime(request.getEndTime())
                                                 .withMetricDataQueries(request.getMetricDataQueries());
    List<MetricDataResult> metricDataResults = new ArrayList<>();
    String nextToken = null;
    do {
      metricDataRequest.withNextToken(nextToken);
      GetMetricDataResult metricDataResult =
          getMetricData(metricDataRequest, request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
      metricDataResults.addAll(metricDataResult.getMetricDataResults());
      nextToken = metricDataResult.getNextToken();
    } while (nextToken != null);
    return AwsCloudWatchMetricDataResponse.builder()
        .metricDataResults(metricDataResults)
        .executionStatus(ExecutionStatus.SUCCESS)
        .build();
  }

  private GetMetricStatisticsResult getMetricStatistics(GetMetricStatisticsRequest request, final AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudWatchClient cloudWatchClient = getAwsCloudWatchClient(region, awsConfig);
      tracker.trackCloudWatchCall("Get Metric Statistics");
      return cloudWatchClient.getMetricStatistics(request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  private GetMetricDataResult getMetricData(
      GetMetricDataRequest request, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudWatchClient cloudWatchClient = getAwsCloudWatchClient(region, awsConfig);
      return cloudWatchClient.getMetricData(request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  @VisibleForTesting
  AmazonCloudWatchClient getAwsCloudWatchClient(String region, AwsConfig awsConfig) {
    AmazonCloudWatchClientBuilder builder = AmazonCloudWatchClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig);
    return (AmazonCloudWatchClient) builder.build();
  }
}

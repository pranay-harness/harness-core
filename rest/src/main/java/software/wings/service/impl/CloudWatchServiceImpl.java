package software.wings.service.impl;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.AwsInfrastructureProviderConfig;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.StateExecutionException;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 12/14/16.
 */
public class CloudWatchServiceImpl implements CloudWatchService {
  @Inject private SettingsService settingsService;
  @Inject private AwsHelperService awsHelperService;

  @Override
  public List<String> listNamespaces(String settingId) {
    AmazonCloudWatchClient cloudWatchClient = getAmazonCloudWatchClient(settingId);
    ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics();
    return listMetricsResult.getMetrics().stream().map(Metric::getNamespace).distinct().collect(Collectors.toList());
  }

  @Override
  public List<String> listMetrics(String settingId, String namespace) {
    AmazonCloudWatchClient cloudWatchClient = getAmazonCloudWatchClient(settingId);
    ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
    listMetricsRequest.setNamespace(namespace);
    ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics(listMetricsRequest);
    return listMetricsResult.getMetrics().stream().map(Metric::getMetricName).distinct().collect(Collectors.toList());
  }

  @Override
  public List<String> listDimensions(String settingId, String namespace, String metricName) {
    AmazonCloudWatchClient cloudWatchClient = getAmazonCloudWatchClient(settingId);
    ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
    listMetricsRequest.withNamespace(namespace).withMetricName(metricName);
    ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics(listMetricsRequest);
    return listMetricsResult.getMetrics()
        .stream()
        .flatMap(metric -> metric.getDimensions().stream().map(Dimension::getName))
        .distinct()
        .collect(Collectors.toList());
  }

  private AmazonCloudWatchClient getAmazonCloudWatchClient(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsInfrastructureProviderConfig)) {
      throw new StateExecutionException("AWS account setting not found");
    }
    AwsInfrastructureProviderConfig awsInfrastructureProviderConfig =
        (AwsInfrastructureProviderConfig) settingAttribute.getValue();
    return awsHelperService.getAwsCloudWatchClient(
        awsInfrastructureProviderConfig.getAccessKey(), awsInfrastructureProviderConfig.getSecretKey());
  }
}

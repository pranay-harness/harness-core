package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateExecutionException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 12/14/16.
 */
@Singleton
public class CloudWatchServiceImpl implements CloudWatchService {
  @Inject private SettingsService settingsService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private SecretManager secretManager;

  @Override
  public List<String> listNamespaces(String settingId, String region) {
    AwsConfig awsConfig = getAwsConfig(settingId);
    return awsHelperService
        .getCloudWatchMetrics(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region)
        .stream()
        .map(Metric::getNamespace)
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public List<String> listMetrics(String settingId, String region, String namespace) {
    ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
    listMetricsRequest.setNamespace(namespace);
    AwsConfig awsConfig = getAwsConfig(settingId);
    List<Metric> metrics = awsHelperService.getCloudWatchMetrics(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, listMetricsRequest);
    return metrics.stream().map(Metric::getMetricName).distinct().collect(Collectors.toList());
  }

  @Override
  public List<String> listDimensions(String settingId, String region, String namespace, String metricName) {
    ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
    listMetricsRequest.withNamespace(namespace).withMetricName(metricName);
    AwsConfig awsConfig = getAwsConfig(settingId);
    List<Metric> metrics = awsHelperService.getCloudWatchMetrics(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, listMetricsRequest);
    return metrics.stream()
        .flatMap(metric -> metric.getDimensions().stream().map(Dimension::getName))
        .distinct()
        .collect(Collectors.toList());
  }

  private AwsConfig getAwsConfig(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsConfig)) {
      throw new StateExecutionException("AWS account setting not found");
    }
    return (AwsConfig) settingAttribute.getValue();
  }
}

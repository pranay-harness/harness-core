package software.wings.service.impl;

import com.google.inject.Singleton;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.StateExecutionException;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 12/14/16.
 */
@Singleton
public class CloudWatchServiceImpl implements CloudWatchService {
  @Inject private SettingsService settingsService;
  @Inject private AwsHelperService awsHelperService;

  @Override
  public List<String> listNamespaces(String settingId, String region) {
    return awsHelperService.getCloudWatchMetrics(getAwsConfig(settingId), region)
        .stream()
        .map(Metric::getNamespace)
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public List<String> listMetrics(String settingId, String region, String namespace) {
    ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
    listMetricsRequest.setNamespace(namespace);
    List<Metric> metrics = awsHelperService.getCloudWatchMetrics(getAwsConfig(settingId), region, listMetricsRequest);
    return metrics.stream().map(Metric::getMetricName).distinct().collect(Collectors.toList());
  }

  @Override
  public List<String> listDimensions(String settingId, String region, String namespace, String metricName) {
    ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
    listMetricsRequest.withNamespace(namespace).withMetricName(metricName);
    List<Metric> metrics = awsHelperService.getCloudWatchMetrics(getAwsConfig(settingId), region, listMetricsRequest);
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
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    return awsConfig;
  }

  /*
  https://console.aws.amazon.com/cloudwatch/home?
  region=us-east-1#metricsV2:graph=~(metrics~(~(~%27AWS*2fLambda~%27Errors))~period~300~stat~%27Sum~start~%27-P1D~end~%27P0D~yAxis~(left~null~right~null)~region~%27us-east-1)

  https://console.aws.amazon.com/cloudwatch/home?
  region=us-east-1#metricsV2:graph=~(metrics~(~(~%27AWS*2fLambda~%27Invocations))~period~300~stat~%27Sum~start~%27-P1D~end~%27P0D~yAxis~(left~null~right~null)~region~%27us-east-1)
   */
}

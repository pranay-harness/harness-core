package software.wings.service.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.serializer.YamlUtils;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateExecutionException;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 12/14/16.
 */
@Singleton
public class CloudWatchServiceImpl implements CloudWatchService {
  @Inject private SettingsService settingsService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private SecretManager secretManager;
  @Inject private AwsInfrastructureProvider awsInfrastructureProvider;

  private final Map<AwsNameSpace, List<CloudWatchMetric>> cloudWatchMetrics;

  @Override
  public Map<AwsNameSpace, List<CloudWatchMetric>> getCloudWatchMetrics() {
    return cloudWatchMetrics;
  }

  @Inject
  public CloudWatchServiceImpl(YamlUtils yamlUtils) {
    try {
      URL url = this.getClass().getResource(Constants.STATIC_CLOUD_WATCH_METRIC_URL);
      String yaml = Resources.toString(url, Charsets.UTF_8);
      cloudWatchMetrics = yamlUtils.read(yaml, new TypeReference<Map<AwsNameSpace, List<CloudWatchMetric>>>() {});
    } catch (Exception e) {
      throw new WingsException(e);
    }
  }

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
    DimensionFilter dimensionFilter = new DimensionFilter();
    dimensionFilter.withName(UUID.randomUUID().toString()).withValue(UUID.randomUUID().toString());
    ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
    listMetricsRequest.withNamespace(namespace);
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

  @Override
  public Set<String> getLoadBalancerNames(String settingId, String region) {
    final Set<String> loadBalancers = new HashSet<>();
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsConfig)) {
      throw new StateExecutionException("AWS account setting not found " + settingId);
    }
    loadBalancers.addAll(awsInfrastructureProvider.listClassicLoadBalancers(settingAttribute, region));
    loadBalancers.addAll(awsInfrastructureProvider.listLoadBalancers(settingAttribute, region));
    return loadBalancers;
  }

  private AwsConfig getAwsConfig(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsConfig)) {
      throw new StateExecutionException("AWS account setting not found");
    }
    return (AwsConfig) settingAttribute.getValue();
  }
}

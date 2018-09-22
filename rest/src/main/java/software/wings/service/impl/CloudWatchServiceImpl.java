package software.wings.service.impl;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.serializer.YamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.AwsConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtil;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.cloudwatch.CloudWatchSetupTestNodeData;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.cloudwatch.CloudWatchDelegateService;
import software.wings.service.intfc.security.SecretManager;

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
  private static final Logger logger = LoggerFactory.getLogger(CloudWatchServiceImpl.class);
  @Inject private SettingsService settingsService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private SecretManager secretManager;
  @Inject private AwsInfrastructureProvider awsInfrastructureProvider;
  @Inject private MLServiceUtil mlServiceUtil;
  @Inject private DelegateProxyFactory delegateProxyFactory;

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
      throw new WingsException("AWS account setting not found " + settingId);
    }
    loadBalancers.addAll(awsInfrastructureProvider.listClassicLoadBalancers(settingAttribute, region));
    loadBalancers.addAll(awsInfrastructureProvider.listLoadBalancers(settingAttribute, region));
    return loadBalancers;
  }

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(CloudWatchSetupTestNodeData setupTestNodeData) {
    String hostName = mlServiceUtil.getHostNameFromExpression(setupTestNodeData);
    try {
      final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = aContext()
                                            .withAccountId(settingAttribute.getAccountId())
                                            .withAppId(Base.GLOBAL_APP_ID)
                                            .withTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      return delegateProxyFactory.get(CloudWatchDelegateService.class, syncTaskContext)
          .getMetricsWithDataForNode((AwsConfig) settingAttribute.getValue(), encryptionDetails, setupTestNodeData,
              apiCallLogWithDummyStateExecution(settingAttribute.getAccountId()), hostName);
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(ErrorCode.CLOUDWATCH_ERROR)
          .addParam("message", "Error in getting metric data for the node. " + e.getMessage());
    }
  }

  private AwsConfig getAwsConfig(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsConfig)) {
      throw new WingsException("AWS account setting not found");
    }
    return (AwsConfig) settingAttribute.getValue();
  }
}

package software.wings.sm.states;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 12/9/16.
 */

@Singleton
public class CloudWatchMetricDataProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    List<SettingAttribute> settingAttributesByType =
        settingsService.getSettingAttributesByType(appId, SettingVariableTypes.AWS.name());
    if (settingAttributesByType != null && settingAttributesByType.size() > 0) {
      SettingAttribute settingAttribute = settingAttributesByType.get(0);
      AwsConfig awsInfrastructureProviderConfig = (AwsConfig) settingAttribute.getValue();
      BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
          awsInfrastructureProviderConfig.getAccessKey(), new String(awsInfrastructureProviderConfig.getSecretKey()));
      AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(awsCredentials);
      ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics();
      Map<String, String> namespaceMap = listMetricsResult.getMetrics().stream().collect(
          Collectors.toMap(Metric::getMetricName, Metric::getMetricName, (key1, key2) -> key1));
      return namespaceMap;
    }
    return new HashMap<>();
  }
}

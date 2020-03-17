package software.wings.verification;

import software.wings.beans.yaml.ChangeContext;
import software.wings.verification.datadog.DatadogLogCVConfiguration;
import software.wings.verification.datadog.DatadogLogCVConfiguration.DatadogLogCVConfigurationYaml;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

import java.util.List;

public class DatadogLogCVConfigurationYamlHandler extends LogsCVConfigurationYamlHandler {
  @Override
  public LogsCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    final DatadogLogCVConfigurationYaml yaml = (DatadogLogCVConfigurationYaml) super.toYaml(bean, appId);
    DatadogLogCVConfiguration datadogLogCVConfiguration = (DatadogLogCVConfiguration) bean;

    yaml.setHostnameField(datadogLogCVConfiguration.getHostnameField());

    return yaml;
  }

  @Override
  public LogsCVConfiguration upsertFromYaml(
      ChangeContext<LogsCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String appId = getAppId(changeContext);
    CVConfiguration previous = getPreviousCVConfiguration(changeContext);
    final DatadogLogCVConfiguration bean = DatadogLogCVConfiguration.builder().build();

    super.toBean(bean, changeContext, appId);

    DatadogLogCVConfigurationYaml yaml = (DatadogLogCVConfigurationYaml) changeContext.getYaml();
    bean.setHostnameField(yaml.getHostnameField());

    saveToDatabase(bean, previous, appId);

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return DatadogLogCVConfigurationYaml.class;
  }
}

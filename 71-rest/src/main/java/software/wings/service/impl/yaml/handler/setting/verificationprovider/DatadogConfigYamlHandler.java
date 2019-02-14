package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import io.harness.exception.HarnessException;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DatadogConfig.DatadogYaml;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class DatadogConfigYamlHandler
    extends VerificationProviderYamlHandler<DatadogConfig.DatadogYaml, DatadogConfig> {
  @Override
  public DatadogConfig.DatadogYaml toYaml(SettingAttribute settingAttribute, String appId) {
    DatadogConfig config = (DatadogConfig) settingAttribute.getValue();

    DatadogYaml yaml = DatadogYaml.builder()
                           .harnessApiVersion(getHarnessApiVersion())
                           .type(config.getType())
                           .url(config.getUrl())
                           .apiKey(new String(config.getApiKey()))
                           .applicationKey(new String(config.getApplicationKey()))
                           .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<DatadogConfig.DatadogYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    DatadogConfig.DatadogYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    DatadogConfig datadogConfig = DatadogConfig.builder()
                                      .accountId(accountId)
                                      .url(yaml.getUrl())
                                      .encryptedApiKey(yaml.getApiKey())
                                      .encryptedApplicationKey(yaml.getApplicationKey())
                                      .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, datadogConfig);
  }

  @Override
  public Class getYamlClass() {
    return PrometheusConfig.PrometheusYaml.class;
  }
}

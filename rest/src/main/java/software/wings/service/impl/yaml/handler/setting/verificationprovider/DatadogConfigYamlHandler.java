package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.DatadogConfig;
import software.wings.beans.DatadogConfig.DatadogConfigBuilder;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

public class DatadogConfigYamlHandler
    extends VerificationProviderYamlHandler<DatadogConfig.DatadogYaml, DatadogConfig> {
  @Override
  public DatadogConfig.DatadogYaml toYaml(SettingAttribute settingAttribute, String appId) {
    DatadogConfig config = (DatadogConfig) settingAttribute.getValue();

    return DatadogConfig.DatadogYaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(config.getType())
        .url(config.getUrl())
        .apiKey(new String(config.getApiKey()))
        .applicationKey(new String(config.getApplicationKey()))
        .build();
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<DatadogConfig.DatadogYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    DatadogConfig.DatadogYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    DatadogConfigBuilder builder = DatadogConfig.builder().accountId(accountId).url(yaml.getUrl());

    char[] decryptedApiKey;
    try {
      decryptedApiKey = secretManager.decryptYamlRef(yaml.getApiKey());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the api id ref:" + yaml.getApiKey());
    }

    char[] decryptedApplicationKey;
    try {
      decryptedApplicationKey = secretManager.decryptYamlRef(yaml.getApplicationKey());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the application key ref:" + yaml.getApplicationKey());
    }

    builder.apiKey(decryptedApiKey).applicationKey(decryptedApplicationKey);

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, builder.build());
  }

  @Override
  public Class getYamlClass() {
    return PrometheusConfig.PrometheusYaml.class;
  }
}

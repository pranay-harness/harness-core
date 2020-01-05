package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import com.google.inject.Singleton;

import io.harness.exception.HarnessException;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@Singleton
public class AzureConfigYamlHandler extends CloudProviderYamlHandler<Yaml, AzureConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AzureConfig azureConfig = (AzureConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(azureConfig.getType())
                    .clientId(azureConfig.getClientId())
                    .tenantId(azureConfig.getTenantId())
                    .key(getEncryptedValue(azureConfig, "key", false))
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    AzureConfig azureConfig = AzureConfig.builder()
                                  .accountId(accountId)
                                  .clientId(yaml.getClientId())
                                  .tenantId(yaml.getTenantId())
                                  .encryptedKey(yaml.getKey())
                                  .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, azureConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

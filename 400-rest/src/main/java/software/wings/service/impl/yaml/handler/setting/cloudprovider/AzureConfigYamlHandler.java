package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.AzureConfig;
import software.wings.beans.AzureConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class AzureConfigYamlHandler extends CloudProviderYamlHandler<AzureConfigYaml, AzureConfig> {
  @Override
  public AzureConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    AzureConfig azureConfig = (AzureConfig) settingAttribute.getValue();

    AzureConfigYaml yaml = AzureConfigYaml.builder()
                               .harnessApiVersion(getHarnessApiVersion())
                               .type(azureConfig.getType())
                               .clientId(azureConfig.getClientId())
                               .tenantId(azureConfig.getTenantId())
                               .key(getEncryptedYamlRef(azureConfig.getAccountId(), azureConfig.getEncryptedKey()))
                               .azureEnvironmentType(azureConfig.getAzureEnvironmentType())
                               .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<AzureConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    AzureConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    AzureConfig azureConfig = AzureConfig.builder()
                                  .accountId(accountId)
                                  .clientId(yaml.getClientId())
                                  .tenantId(yaml.getTenantId())
                                  .encryptedKey(yaml.getKey())
                                  .azureEnvironmentType(yaml.getAzureEnvironmentType())
                                  .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, azureConfig);
  }

  @Override
  public Class getYamlClass() {
    return AzureConfigYaml.class;
  }
}

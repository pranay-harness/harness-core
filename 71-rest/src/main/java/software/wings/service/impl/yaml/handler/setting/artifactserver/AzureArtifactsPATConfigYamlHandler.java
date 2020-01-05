package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class AzureArtifactsPATConfigYamlHandler extends AzureArtifactsYamlHandler<Yaml, AzureArtifactsPATConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AzureArtifactsPATConfig azureArtifactsPATConfig = (AzureArtifactsPATConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(azureArtifactsPATConfig.getType())
                    .azureDevopsUrl(azureArtifactsPATConfig.getAzureDevopsUrl())
                    .pat(getEncryptedValue(azureArtifactsPATConfig, "pat", false))
                    .build();

    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    AzureArtifactsPATConfig azureArtifactsPATConfig = AzureArtifactsPATConfig.builder()
                                                          .accountId(accountId)
                                                          .azureDevopsUrl(yaml.getAzureDevopsUrl())
                                                          .encryptedPat(yaml.getPat())
                                                          .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, azureArtifactsPATConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

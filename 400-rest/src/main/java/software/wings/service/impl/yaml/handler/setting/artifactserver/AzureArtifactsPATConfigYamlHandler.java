package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfigYaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class AzureArtifactsPATConfigYamlHandler
    extends AzureArtifactsYamlHandler<AzureArtifactsPATConfigYaml, AzureArtifactsPATConfig> {
  @Override
  public AzureArtifactsPATConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    AzureArtifactsPATConfig azureArtifactsPATConfig = (AzureArtifactsPATConfig) settingAttribute.getValue();
    AzureArtifactsPATConfigYaml yaml =
        AzureArtifactsPATConfigYaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(azureArtifactsPATConfig.getType())
            .azureDevopsUrl(azureArtifactsPATConfig.getAzureDevopsUrl())
            .pat(getEncryptedYamlRef(azureArtifactsPATConfig.getAccountId(), azureArtifactsPATConfig.getEncryptedPat()))
            .build();

    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<AzureArtifactsPATConfigYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    AzureArtifactsPATConfigYaml yaml = changeContext.getYaml();
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
    return AzureArtifactsPATConfigYaml.class;
  }
}

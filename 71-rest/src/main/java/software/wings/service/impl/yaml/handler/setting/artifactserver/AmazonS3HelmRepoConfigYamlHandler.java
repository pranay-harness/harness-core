package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class AmazonS3HelmRepoConfigYamlHandler extends HelmRepoYamlHandler<Yaml, AmazonS3HelmRepoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(amazonS3HelmRepoConfig.getType())
                    .bucket(amazonS3HelmRepoConfig.getBucketName())
                    .folderPath(amazonS3HelmRepoConfig.getFolderPath())
                    .region(amazonS3HelmRepoConfig.getRegion())
                    .cloudProvider(getCloudProviderName(appId, amazonS3HelmRepoConfig.getConnectorId()))
                    .build();

    toYaml(yaml, settingAttribute, appId);

    return yaml;
  }

  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig =
        AmazonS3HelmRepoConfig.builder()
            .accountId(accountId)
            .bucketName(yaml.getBucket())
            .region(yaml.getRegion())
            .folderPath(yaml.getFolderPath())
            .connectorId(getCloudProviderIdByName(accountId, yaml.getCloudProvider()))
            .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, amazonS3HelmRepoConfig);
  }

  @Override
  public Class getYamlClass() {
    return AmazonS3HelmRepoConfig.Yaml.class;
  }
}

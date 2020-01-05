package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class GcsHelmRepoConfigYamlHandler extends HelmRepoYamlHandler<Yaml, GCSHelmRepoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GCSHelmRepoConfig gcsHelmRepoConfig = (GCSHelmRepoConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(gcsHelmRepoConfig.getType())
                    .bucket(gcsHelmRepoConfig.getBucketName())
                    .cloudProvider(getCloudProviderName(appId, gcsHelmRepoConfig.getConnectorId()))
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

    GCSHelmRepoConfig gcsHelmRepoConfig = GCSHelmRepoConfig.builder()
                                              .accountId(accountId)
                                              .bucketName(yaml.getBucket())
                                              .connectorId(getCloudProviderIdByName(accountId, yaml.getCloudProvider()))
                                              .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, gcsHelmRepoConfig);
  }

  @Override
  public Class getYamlClass() {
    return GCSHelmRepoConfig.Yaml.class;
  }
}

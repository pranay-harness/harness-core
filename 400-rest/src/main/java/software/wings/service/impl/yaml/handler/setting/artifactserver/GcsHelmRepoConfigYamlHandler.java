package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfigYaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class GcsHelmRepoConfigYamlHandler extends HelmRepoYamlHandler<GCSHelmRepoConfigYaml, GCSHelmRepoConfig> {
  @Override
  public GCSHelmRepoConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    GCSHelmRepoConfig gcsHelmRepoConfig = (GCSHelmRepoConfig) settingAttribute.getValue();

    GCSHelmRepoConfigYaml yaml = GCSHelmRepoConfigYaml.builder()
                                     .harnessApiVersion(getHarnessApiVersion())
                                     .type(gcsHelmRepoConfig.getType())
                                     .bucket(gcsHelmRepoConfig.getBucketName())
                                     .cloudProvider(getCloudProviderName(appId, gcsHelmRepoConfig.getConnectorId()))
                                     .build();

    toYaml(yaml, settingAttribute, appId);

    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<GCSHelmRepoConfigYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    GCSHelmRepoConfigYaml yaml = changeContext.getYaml();
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
    return GCSHelmRepoConfigYaml.class;
  }
}

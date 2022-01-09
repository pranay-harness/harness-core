/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class AmazonS3HelmRepoConfigYamlHandler extends HelmRepoYamlHandler<Yaml, AmazonS3HelmRepoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(amazonS3HelmRepoConfig.getType())
                    .bucket(amazonS3HelmRepoConfig.getBucketName())
                    .region(amazonS3HelmRepoConfig.getRegion())
                    .cloudProvider(getCloudProviderName(appId, amazonS3HelmRepoConfig.getConnectorId()))
                    .build();

    toYaml(yaml, settingAttribute, appId);

    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    String folderPath = previous == null ? null : ((AmazonS3HelmRepoConfig) previous.getValue()).getFolderPath();
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig =
        AmazonS3HelmRepoConfig.builder()
            .accountId(accountId)
            .bucketName(yaml.getBucket())
            .region(yaml.getRegion())
            .connectorId(getCloudProviderIdByName(accountId, yaml.getCloudProvider()))
            .folderPath(folderPath)
            .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, amazonS3HelmRepoConfig);
  }

  @Override
  public Class getYamlClass() {
    return AmazonS3HelmRepoConfig.Yaml.class;
  }
}

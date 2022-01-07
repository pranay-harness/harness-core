/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class HttpHelmRepoConfigYamlHandler extends HelmRepoYamlHandler<Yaml, HttpHelmRepoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(httpHelmRepoConfig.getType())
                    .url(httpHelmRepoConfig.getChartRepoUrl())
                    .build();

    if (isNotBlank(httpHelmRepoConfig.getUsername())) {
      yaml.setUsername(httpHelmRepoConfig.getUsername());
      yaml.setPassword(
          getEncryptedYamlRef(httpHelmRepoConfig.getAccountId(), httpHelmRepoConfig.getEncryptedPassword()));
    }

    toYaml(yaml, settingAttribute, appId);

    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    HttpHelmRepoConfig httpHelmRepoConfig = HttpHelmRepoConfig.builder()
                                                .accountId(accountId)
                                                .chartRepoUrl(yaml.getUrl())
                                                .username(yaml.getUsername())
                                                .encryptedPassword(yaml.getPassword())
                                                .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, httpHelmRepoConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

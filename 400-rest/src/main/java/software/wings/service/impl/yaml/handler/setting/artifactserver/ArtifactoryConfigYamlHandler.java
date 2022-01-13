/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.ArtifactoryConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class ArtifactoryConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, ArtifactoryConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingAttribute.getValue();
    String encryptedPassword = null;
    if (artifactoryConfig.hasCredentials()) {
      encryptedPassword =
          getEncryptedYamlRef(artifactoryConfig.getAccountId(), artifactoryConfig.getEncryptedPassword());
    }

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(artifactoryConfig.getType())
                    .url(artifactoryConfig.getArtifactoryUrl())
                    .username(artifactoryConfig.getUsername())
                    .password(encryptedPassword)
                    .delegateSelectors(getDelegateSelectors(artifactoryConfig.getDelegateSelectors()))
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

    ArtifactoryConfig config = ArtifactoryConfig.builder()
                                   .accountId(accountId)
                                   .artifactoryUrl(yaml.getUrl())
                                   .encryptedPassword(yaml.getPassword())
                                   .username(yaml.getUsername())
                                   .delegateSelectors(getDelegateSelectors(yaml.getDelegateSelectors()))
                                   .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  private List<String> getDelegateSelectors(List<String> delegateSelectors) {
    return isNotEmpty(delegateSelectors)
        ? delegateSelectors.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList())
        : new ArrayList<>();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

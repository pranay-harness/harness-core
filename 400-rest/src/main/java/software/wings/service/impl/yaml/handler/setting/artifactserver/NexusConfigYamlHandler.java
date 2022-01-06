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
import software.wings.beans.config.NexusConfig;
import software.wings.beans.config.NexusConfig.Yaml;
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
public class NexusConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, NexusConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();
    Yaml yaml;
    List<String> delegateSelectors = getDelegateSelectors(nexusConfig.getDelegateSelectors());
    if (nexusConfig.hasCredentials()) {
      yaml = Yaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(nexusConfig.getType())
                 .url(nexusConfig.getNexusUrl())
                 .username(nexusConfig.getUsername())
                 .password(getEncryptedYamlRef(nexusConfig.getAccountId(), nexusConfig.getEncryptedPassword()))
                 .version(nexusConfig.getVersion())
                 .delegateSelectors(delegateSelectors)
                 .build();
    } else {
      yaml = Yaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(nexusConfig.getType())
                 .url(nexusConfig.getNexusUrl())
                 .version(nexusConfig.getVersion())
                 .delegateSelectors(delegateSelectors)
                 .build();
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
    List<String> delegateSelectors = getDelegateSelectors(yaml.getDelegateSelectors());

    NexusConfig config = NexusConfig.builder()
                             .accountId(accountId)
                             .nexusUrl(yaml.getUrl())
                             .encryptedPassword(yaml.getPassword())
                             .username(yaml.getUsername())
                             .version(yaml.getVersion())
                             .delegateSelectors(delegateSelectors)
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

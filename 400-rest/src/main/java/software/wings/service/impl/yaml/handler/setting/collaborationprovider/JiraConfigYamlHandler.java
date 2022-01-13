/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.HarnessException;

import software.wings.beans.JiraConfig;
import software.wings.beans.JiraConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Converstion between bean <-and-> yaml.
 *
 * @author swagat on 9/6/18
 */
@OwnedBy(CDC)
@Singleton
public class JiraConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, JiraConfig> {
  @Override
  protected SettingAttribute toBean(final SettingAttribute previous, final ChangeContext<Yaml> changeContext,
      final List<ChangeContext> changeSetContext) throws HarnessException {
    final Yaml yaml = changeContext.getYaml();
    final JiraConfig config = new JiraConfig();
    config.setBaseUrl(yaml.getBaseUrl());
    config.setUsername(yaml.getUsername());
    config.setPassword(yaml.getPassword().toCharArray());
    config.setDelegateSelectors(getDelegateSelectors(yaml.getDelegateSelectors()));

    final String accountId = changeContext.getChange().getAccountId();
    config.setAccountId(accountId);

    final String uuid = previous == null ? null : previous.getUuid();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Yaml toYaml(final SettingAttribute settingAttribute, final String appId) {
    final JiraConfig jiraConfig = (JiraConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(jiraConfig.getType())
                    .baseUrl(jiraConfig.getBaseUrl())
                    .username(jiraConfig.getUsername())
                    .password(getEncryptedYamlRef(jiraConfig.getAccountId(), jiraConfig.getEncryptedPassword()))
                    .delegateSelectors(getDelegateSelectors(jiraConfig.getDelegateSelectors()))
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  private List<String> getDelegateSelectors(List<String> delegateSelectors) {
    return isNotEmpty(delegateSelectors)
        ? delegateSelectors.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList())
        : new ArrayList<>();
  }

  @Override
  public Class getYamlClass() {
    return JiraConfig.Yaml.class;
  }
}

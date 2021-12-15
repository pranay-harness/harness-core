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

import software.wings.beans.ServiceNowConfig;
import software.wings.beans.ServiceNowConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDC)
public class ServiceNowConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, ServiceNowConfig> {
  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    final Yaml yaml = changeContext.getYaml();
    final ServiceNowConfig config = new ServiceNowConfig();
    config.setBaseUrl(yaml.getBaseUrl());
    config.setUsername(yaml.getUsername());
    config.setPassword(yaml.getPassword().toCharArray());
    config.setDelegateSelectors(getDelegateSelectors(yaml.getDelegateSelectors()));
    config.setSkipValidation(yaml.isSkipValidation());

    final String accountId = changeContext.getChange().getAccountId();
    config.setAccountId(accountId);

    final String uuid = previous == null ? null : previous.getUuid();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    final ServiceNowConfig serviceNowConfig = (ServiceNowConfig) settingAttribute.getValue();
    Yaml yaml =
        Yaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(serviceNowConfig.getType())
            .baseUrl(serviceNowConfig.getBaseUrl())
            .username(serviceNowConfig.getUsername())
            .password(getEncryptedYamlRef(serviceNowConfig.getAccountId(), serviceNowConfig.getEncryptedPassword()))
            .delegateSelectors(getDelegateSelectors(serviceNowConfig.getDelegateSelectors()))
            .skipValidation(serviceNowConfig.isSkipValidation())
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
    return ServiceNowConfig.Yaml.class;
  }
}

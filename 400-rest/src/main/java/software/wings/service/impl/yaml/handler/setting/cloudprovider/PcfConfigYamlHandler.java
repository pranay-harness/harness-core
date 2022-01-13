/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.PcfConfig;
import software.wings.beans.PcfConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(CDP)
public class PcfConfigYamlHandler extends CloudProviderYamlHandler<Yaml, PcfConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();
    boolean useEncryptedUsername = pcfConfig.isUseEncryptedUsername();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .username(useEncryptedUsername ? null : String.valueOf(pcfConfig.getUsername()))
                    .usernameSecretId(useEncryptedUsername
                            ? getEncryptedYamlRef(pcfConfig.getAccountId(), pcfConfig.getEncryptedUsername())
                            : null)
                    .endpointUrl(pcfConfig.getEndpointUrl())
                    .password(getEncryptedYamlRef(pcfConfig.getAccountId(), pcfConfig.getEncryptedPassword()))
                    .type(pcfConfig.getType())
                    .skipValidation(pcfConfig.isSkipValidation())
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  @VisibleForTesting
  public SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    if (isNotEmpty(yaml.getUsername()) && isNotEmpty(yaml.getUsernameSecretId())) {
      throw new InvalidRequestException("Cannot set both value and secret reference for username field", USER);
    }

    PcfConfig config = PcfConfig.builder()
                           .accountId(accountId)
                           .username(yaml.getUsername() != null ? yaml.getUsername().toCharArray() : null)
                           .encryptedUsername(yaml.getUsernameSecretId())
                           .useEncryptedUsername(yaml.getUsernameSecretId() != null)
                           .encryptedPassword(yaml.getPassword())
                           .endpointUrl(yaml.getEndpointUrl())
                           .skipValidation(yaml.isSkipValidation())
                           .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

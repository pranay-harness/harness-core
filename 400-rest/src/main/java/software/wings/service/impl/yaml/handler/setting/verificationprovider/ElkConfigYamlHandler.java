/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.ElkConfig;
import software.wings.beans.ElkConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.utils.Utils;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class ElkConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, ElkConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    ElkConfig config = (ElkConfig) settingAttribute.getValue();
    String connectorType = Utils.getStringFromEnum(config.getElkConnector());

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(config.getType())
                    .elkUrl(config.getElkUrl())
                    .username(config.getUsername())
                    .password(config.getEncryptedPassword() != null
                            ? getEncryptedYamlRef(config.getAccountId(), config.getEncryptedPassword())
                            : null)
                    .connectorType(connectorType)
                    .validationType(config.getValidationType())
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
    ElkConnector elkConnector = Utils.getEnumFromString(ElkConnector.class, yaml.getConnectorType());
    ElkConfig config = ElkConfig.builder()
                           .accountId(accountId)
                           .elkUrl(yaml.getElkUrl())
                           .encryptedPassword(yaml.getPassword())
                           .validationType(yaml.getValidationType())
                           .username(yaml.getUsername())
                           .elkConnector(elkConnector)
                           .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

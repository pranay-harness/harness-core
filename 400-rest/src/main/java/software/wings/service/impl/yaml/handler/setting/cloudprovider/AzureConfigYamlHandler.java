/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.AzureConfig;
import software.wings.beans.AzureConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class AzureConfigYamlHandler extends CloudProviderYamlHandler<Yaml, AzureConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AzureConfig azureConfig = (AzureConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(azureConfig.getType())
                    .clientId(azureConfig.getClientId())
                    .tenantId(azureConfig.getTenantId())
                    .key(getEncryptedYamlRef(azureConfig.getAccountId(), azureConfig.getEncryptedKey()))
                    .azureEnvironmentType(azureConfig.getAzureEnvironmentType())
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

    AzureConfig azureConfig = AzureConfig.builder()
                                  .accountId(accountId)
                                  .clientId(yaml.getClientId())
                                  .tenantId(yaml.getTenantId())
                                  .encryptedKey(yaml.getKey())
                                  .azureEnvironmentType(yaml.getAzureEnvironmentType())
                                  .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, azureConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

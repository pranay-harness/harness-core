/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import software.wings.beans.GcpConfig;
import software.wings.beans.GcpConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class GcpConfigYamlHandler extends CloudProviderYamlHandler<Yaml, GcpConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(gcpConfig.getType())
                    .serviceAccountKeyFileContent(getEncryptedYamlRef(
                        gcpConfig.getAccountId(), gcpConfig.getEncryptedServiceAccountKeyFileContent()))
                    .useDelegateSelectors(gcpConfig.isUseDelegateSelectors())
                    .delegateSelectors(gcpConfig.getDelegateSelectors())
                    .skipValidation(gcpConfig.isSkipValidation())
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  public SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    List<String> delegagateSelectors;
    boolean isUseDelegateSelectors;
    if (isNotEmpty(yaml.getDelegateSelector()) && isEmpty(yaml.getDelegateSelectors())) {
      delegagateSelectors = Collections.singletonList(yaml.getDelegateSelector());
      isUseDelegateSelectors = yaml.isUseDelegate();
    } else {
      delegagateSelectors = yaml.getDelegateSelectors();
      isUseDelegateSelectors = yaml.isUseDelegateSelectors();
    }

    GcpConfig config = GcpConfig.builder()
                           .accountId(accountId)
                           .encryptedServiceAccountKeyFileContent(yaml.getServiceAccountKeyFileContent())
                           .useDelegateSelectors(isUseDelegateSelectors)
                           .delegateSelectors(delegagateSelectors)
                           .skipValidation(yaml.isSkipValidation())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

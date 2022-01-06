/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.PhysicalDataCenterConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class PhysicalDataCenterConfigYamlHandler extends CloudProviderYamlHandler<Yaml, PhysicalDataCenterConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    PhysicalDataCenterConfig physicalDataCenterConfig = (PhysicalDataCenterConfig) settingAttribute.getValue();

    Yaml yaml =
        Yaml.builder().harnessApiVersion(getHarnessApiVersion()).type(physicalDataCenterConfig.getType()).build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    String accountId = changeContext.getChange().getAccountId();
    PhysicalDataCenterConfig config = new PhysicalDataCenterConfig();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

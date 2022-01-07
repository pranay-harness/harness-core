/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.yaml.directory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.yaml.YamlVersion.Type;

@OwnedBy(HarnessTeam.DX)
public class ServiceLevelYamlNode extends AppLevelYamlNode {
  private String serviceId;

  public ServiceLevelYamlNode() {}

  public ServiceLevelYamlNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
  }

  public ServiceLevelYamlNode(String accountId, String uuid, String appId, String serviceId, String name,
      Class theClass, DirectoryPath directoryPath, Type yamlVersionType) {
    super(accountId, uuid, appId, name, theClass, directoryPath, yamlVersionType);
    this.serviceId = serviceId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}

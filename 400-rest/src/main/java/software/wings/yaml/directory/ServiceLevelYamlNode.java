/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

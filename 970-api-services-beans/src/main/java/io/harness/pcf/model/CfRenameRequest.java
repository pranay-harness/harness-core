/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CfRenameRequest extends CfRequestConfig {
  private String guid;
  private String name;
  private String newName;

  public CfRenameRequest(CfRequestConfig requestConfig, String guid, String name, String newName) {
    super(requestConfig.getOrgName(), requestConfig.getSpaceName(), requestConfig.getUserName(),
        requestConfig.getPassword(), requestConfig.getEndpointUrl(), requestConfig.getApplicationName(),
        requestConfig.getManifestYaml(), requestConfig.getDesiredCount(), requestConfig.getRouteMaps(),
        requestConfig.getServiceVariables(), requestConfig.getSafeDisplayServiceVariables(),
        requestConfig.getTimeOutIntervalInMins(), requestConfig.isUseCFCLI(), requestConfig.getCfCliPath(),
        requestConfig.getCfCliVersion(), requestConfig.getCfHomeDirPath(), requestConfig.isLoggedin(),
        requestConfig.isLimitPcfThreads(), requestConfig.isIgnorePcfConnectionContextCache(),
        requestConfig.isUseNumbering());
    this.guid = guid;
    this.name = name;
    this.newName = newName;
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class CfRequestConfig {
  private String orgName;
  private String spaceName;
  private String userName;
  private String password;
  private String endpointUrl;
  private String applicationName;
  private String manifestYaml;
  private int desiredCount;
  private List<String> routeMaps;
  private Map<String, String> serviceVariables;
  Map<String, String> safeDisplayServiceVariables;
  private int timeOutIntervalInMins;
  private boolean useCFCLI;
  private String cfCliPath;
  private CfCliVersion cfCliVersion;
  private String cfHomeDirPath;
  private boolean loggedin;
  private boolean limitPcfThreads;
  private boolean ignorePcfConnectionContextCache;
  private boolean useNumbering;
}

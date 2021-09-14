/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.configuration;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DEL)
public class DelegateConfiguration {
  private String accountId;
  private String accountSecret;
  private String managerUrl;
  private String verificationServiceUrl;
  private String cvNextGenUrl;
  private String watcherCheckLocation;
  private long heartbeatIntervalMs;
  private String localDiskPath;
  private boolean doUpgrade;
  private Integer maxCachedArtifacts;
  private boolean pollForTasks;
  private String description;

  private String kubectlPath;
  private String ocPath;
  private String kustomizePath;

  private String managerTarget;
  private String managerAuthority;
  private String queueFilePath;

  private boolean useCdn;

  private String cdnUrl;

  private String helmPath;
  private String helm3Path;

  private String cfCli6Path;
  private String cfCli7Path;

  private boolean grpcServiceEnabled;
  private Integer grpcServiceConnectorPort;

  private String logStreamingServiceBaseUrl;
  private boolean clientToolsDownloadDisabled;
  private boolean installClientToolsInBackground;
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
  private boolean versionCheckDisabled;
}

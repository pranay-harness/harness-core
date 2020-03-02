package io.harness.delegate.configuration;

import lombok.Builder;
import lombok.Data;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
@Data
@Builder
public class DelegateConfiguration {
  private String accountId;
  private String accountSecret;
  private String managerUrl;
  private String verificationServiceUrl;
  private String watcherCheckLocation;
  private long heartbeatIntervalMs;
  private String localDiskPath;
  private boolean doUpgrade;
  private Integer maxCachedArtifacts;
  private boolean proxy;
  private boolean pollForTasks;
  private String description;

  private String kubectlPath;
  private String helmPath;
  private String ocPath;
  private String kustomizePath;

  private boolean enablePerpetualTasks;
  private String managerTarget;
  private String managerAuthority;
  private String queueFilePath;
}

package io.harness.delegate.configuration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
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

  private boolean grpcServiceEnabled;
  private Integer grpcServiceConnectorPort;

  private String managerServiceSecret;

  private String logStreamingServiceBaseUrl;
}

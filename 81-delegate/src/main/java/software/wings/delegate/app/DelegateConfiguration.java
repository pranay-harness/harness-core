package software.wings.delegate.app;

import lombok.Data;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
@Data
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
}

package io.harness.watcher.app;

import lombok.Data;

/**
 * Created by brett on 10/30/17
 */
@Data
public class WatcherConfiguration {
  private String accountId;
  private String accountSecret;
  private String managerUrl;
  private boolean doUpgrade;
  private String upgradeCheckLocation;
  private long upgradeCheckIntervalSeconds;
  private String delegateCheckLocation;
  private String queueFilePath;
  private String publishTarget;
  private String publishAuthority;
  private boolean fileHandlesMonitoringEnabled;
  private long fileHandlesMonitoringIntervalInMinutes;
  private long fileHandlesLogsRetentionInMinutes;
}

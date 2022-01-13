/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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

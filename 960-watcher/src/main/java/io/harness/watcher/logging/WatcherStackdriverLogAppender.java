/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.watcher.logging;

import static io.harness.network.SafeHttpCall.execute;
import static io.harness.watcher.app.WatcherApplication.getConfiguration;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBetween;

import io.harness.concurrent.HTimeLimiter;
import io.harness.logging.AccessTokenBean;
import io.harness.logging.RemoteStackdriverLogAppender;
import io.harness.managerclient.ManagerClientV2;
import io.harness.rest.RestResponse;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WatcherStackdriverLogAppender extends RemoteStackdriverLogAppender {
  private static final String APP_NAME = "watcher";

  private static TimeLimiter timeLimiter;
  private static ManagerClientV2 managerClient;

  private String accountId = "";
  private String managerHost = "";

  @Override
  protected String getAppName() {
    return APP_NAME;
  }

  @Override
  protected String getAccountId() {
    if (isBlank(accountId) && getConfiguration() != null) {
      accountId = getConfiguration().getAccountId();
    }
    return accountId;
  }

  @Override
  protected String getManagerHost() {
    if (isBlank(managerHost) && getConfiguration() != null) {
      managerHost = substringBetween(getConfiguration().getManagerUrl(), "://", "/api/");
    }
    return managerHost;
  }

  @Override
  protected String getDelegateId() {
    return null;
  }

  @Override
  protected AccessTokenBean getLoggingToken() {
    if (timeLimiter == null || managerClient == null) {
      return null;
    }

    try {
      RestResponse<AccessTokenBean> response = HTimeLimiter.callInterruptible21(
          timeLimiter, Duration.ofSeconds(15), () -> execute(managerClient.getLoggingToken(getAccountId())));
      if (response != null) {
        return response.getResource();
      }
    } catch (UncheckedTimeoutException ex) {
      log.warn("Timed out getting logging token");
    } catch (Exception e) {
      log.error("Error getting logging token", e);
    }

    return null;
  }

  public static void setTimeLimiter(TimeLimiter timeLimiter) {
    WatcherStackdriverLogAppender.timeLimiter = timeLimiter;
  }

  public static void setManagerClient(ManagerClientV2 managerClient) {
    WatcherStackdriverLogAppender.managerClient = managerClient;
  }
}

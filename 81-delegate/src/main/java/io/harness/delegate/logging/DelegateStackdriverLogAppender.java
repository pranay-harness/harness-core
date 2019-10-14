package io.harness.delegate.logging;

import static io.harness.delegate.app.DelegateApplication.getConfiguration;
import static io.harness.network.SafeHttpCall.execute;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBetween;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import io.harness.logging.AccessTokenBean;
import io.harness.logging.RemoteStackdriverLogAppender;
import io.harness.managerclient.ManagerClient;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DelegateStackdriverLogAppender extends RemoteStackdriverLogAppender {
  private static final String APP_NAME = "delegate";

  private static TimeLimiter timeLimiter;
  private static ManagerClient managerClient;

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
  protected AccessTokenBean getLoggingToken() {
    if (timeLimiter == null || managerClient == null) {
      return null;
    }

    try {
      RestResponse<AccessTokenBean> response = timeLimiter.callWithTimeout(
          () -> execute(managerClient.getLoggingToken(getAccountId())), 15L, TimeUnit.SECONDS, true);
      if (response != null) {
        return response.getResource();
      }
    } catch (UncheckedTimeoutException ex) {
      logger.warn("Timed out getting logging token");
    } catch (Exception e) {
      logger.error("Error getting logging token", e);
    }

    return null;
  }

  public static void setTimeLimiter(TimeLimiter timeLimiter) {
    DelegateStackdriverLogAppender.timeLimiter = timeLimiter;
  }

  public static void setManagerClient(ManagerClient managerClient) {
    DelegateStackdriverLogAppender.managerClient = managerClient;
  }
}

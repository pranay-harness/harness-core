package io.harness.delegate.logging;

import static io.harness.delegate.app.DelegateApplication.getConfiguration;
import static io.harness.network.SafeHttpCall.execute;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBetween;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.AccessTokenBean;
import io.harness.logging.RemoteStackdriverLogAppender;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.rest.RestResponse;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._420_DELEGATE_AGENT)
public class DelegateStackdriverLogAppender extends RemoteStackdriverLogAppender {
  private static final String APP_NAME = "delegate";

  private static TimeLimiter timeLimiter;
  private static DelegateAgentManagerClient delegateAgentManagerClient;
  private static String delegateId;

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
    return delegateId;
  }

  @Override
  protected AccessTokenBean getLoggingToken() {
    if (timeLimiter == null || delegateAgentManagerClient == null) {
      return null;
    }

    try {
      RestResponse<AccessTokenBean> response = timeLimiter.callWithTimeout(
          () -> execute(delegateAgentManagerClient.getLoggingToken(getAccountId())), 15L, TimeUnit.SECONDS, true);
      if (response != null) {
        return response.getResource();
      }
    } catch (UncheckedTimeoutException ex) {
      log.warn("Timed out getting logging token", ex);
    } catch (Exception e) {
      log.error("Error getting logging token", e);
    }

    return null;
  }

  public static void setTimeLimiter(TimeLimiter timeLimiter) {
    DelegateStackdriverLogAppender.timeLimiter = timeLimiter;
  }

  public static void setManagerClient(DelegateAgentManagerClient delegateAgentManagerClient) {
    DelegateStackdriverLogAppender.delegateAgentManagerClient = delegateAgentManagerClient;
  }

  public static void setDelegateId(String delegateId) {
    DelegateStackdriverLogAppender.delegateId = delegateId;
  }
}

package software.wings.service.impl;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.configuration.NoLimitConfiguredException;
import io.harness.limits.lib.LimitChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;

@Singleton
public class PreDeploymentChecker {
  @Inject private AppService appService;
  @Inject private AccountService accountService;
  @Inject private LimitCheckerFactory limitCheckerFactory;

  private static final Logger log = LoggerFactory.getLogger(PreDeploymentChecker.class);

  public void check(String appId) {
    Application application = appService.get(appId, false);
    requireNonNull(application, "Application not found. Is the application ID correct? AppId: " + appId);

    checkDeploymentRateLimit(application.getAccountId(), appId);
    checkIfAccountExpired(application.getAccountId(), appId);
  }

  /**
   * checks if the deployments being done is within allowed rate limits.
   * @throws WingsException with errorCode {@link ErrorCode#USAGE_LIMITS_EXCEEDED} in case limits are exceeded.
   */
  private void checkDeploymentRateLimit(String accountId, String appId) {
    Action deployAction = new Action(accountId, ActionType.DEPLOY);
    try {
      LimitChecker checker = limitCheckerFactory.getInstance(deployAction);
      if (!checker.checkAndConsume()) {
        throw new WingsException(
            ErrorCode.USAGE_LIMITS_EXCEEDED, "Deployment Rate Limit Reached. Please contact Harness support.");
      }
    } catch (NoLimitConfiguredException e) {
      log.error(
          "No limit is configured for action: {}. Deployments will be allowed to maintain backward compatibility. But deployments are NOT being rate limited.",
          deployAction, e);
    }
  }

  private void checkIfAccountExpired(String accountId, String appId) throws WingsException {
    boolean isAccountExpired = accountService.isAccountExpired(accountId);
    if (isAccountExpired) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Trial license expired!!! Please contact Harness Support.");
    }
  }
}

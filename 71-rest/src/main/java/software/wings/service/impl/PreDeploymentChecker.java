package software.wings.service.impl;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.checker.UsageLimitExceededException;
import io.harness.limits.configuration.NoLimitConfiguredException;
import io.harness.limits.lib.RateLimitChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.licensing.LicenseService;
import software.wings.service.intfc.AppService;

@Singleton
public class PreDeploymentChecker {
  @Inject private AppService appService;
  @Inject private LicenseService licenseService;
  @Inject private LimitCheckerFactory limitCheckerFactory;

  private static final Logger log = LoggerFactory.getLogger(PreDeploymentChecker.class);

  /**
   * checks if the deployments being done is within allowed rate limits.
   * @throws UsageLimitExceededException in case limit exceeds
   */
  public void checkDeploymentRateLimit(String accountId, String appId) throws UsageLimitExceededException {
    Action deployAction = new Action(accountId, ActionType.DEPLOY);
    try {
      RateLimitChecker checker = (RateLimitChecker) limitCheckerFactory.getInstance(deployAction);
      if (!checker.checkAndConsume()) {
        throw new UsageLimitExceededException(checker.getLimit(), accountId);
      }
    } catch (NoLimitConfiguredException e) {
      log.error(
          "No limit is configured for action: {}. Deployments will be allowed to maintain backward compatibility. But deployments are NOT being rate limited.",
          deployAction, e);
    }
  }

  public void checkIfAccountExpired(String appId) throws WingsException {
    Application application = appService.get(appId, false);
    requireNonNull(application, "Application not found. Is the application ID correct? AppId: " + appId);

    boolean isAccountExpired = licenseService.isAccountExpired(application.getAccountId());
    if (isAccountExpired) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "License expired!!! Please contact Harness Support.");
    }
  }
}

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
import io.harness.limits.checker.LimitApproachingException;
import io.harness.limits.checker.UsageLimitExceededException;
import io.harness.limits.checker.rate.MongoSlidingWindowRateLimitChecker;
import io.harness.limits.checker.rate.RateLimitVicinityChecker;
import io.harness.limits.configuration.NoLimitConfiguredException;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.lib.RateLimitChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

@Singleton
public class PreDeploymentChecker {
  @Inject private AppService appService;
  @Inject private LicenseService licenseService;
  @Inject private GovernanceConfigService governanceConfigService;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private WingsPersistence persistence;

  private static final Logger log = LoggerFactory.getLogger(PreDeploymentChecker.class);

  private static final int PERCENT_TO_WARN_ON_DEFAULT = 85;
  private static final int LONG_LIMIT_DURATION = 24;
  private static final TimeUnit LONG_LIMIT_DURATION_UNIT = TimeUnit.HOURS;
  private static final double SHORT_LIMIT_PERCENT = 0.4D;

  /**
   * checks if the deployments being done is within allowed rate limits.
   * @throws UsageLimitExceededException in case limit exceeds
   * @throws LimitApproachingException in case user is close to hitting limit.
   * These exceptions should not block deployments, useful to give some warnings, though.
   */
  public void checkDeploymentRateLimit(String accountId, String appId) throws UsageLimitExceededException {
    Action deployAction = new Action(accountId, ActionType.DEPLOY);
    final int warningPercentage = percentToWarnOn();

    try {
      RateLimitChecker checker = (RateLimitChecker) limitCheckerFactory.getInstance(deployAction);
      RateLimitChecker shortDurationChecker = shortDurationChecker(checker, deployAction);

      if (null != shortDurationChecker && !shortDurationChecker.checkAndConsume()) {
        log.info("Short Duration Deployment Limit Reached. Limit: {} , accountId={}", shortDurationChecker.getLimit(),
            accountId);
        throw new UsageLimitExceededException(shortDurationChecker.getLimit(), accountId);
      }

      if (!checker.checkAndConsume()) {
        throw new UsageLimitExceededException(checker.getLimit(), accountId);
      }

      RateLimitVicinityChecker vicinityChecker = (RateLimitVicinityChecker) checker;
      if (vicinityChecker.crossed(warningPercentage)) {
        throw new LimitApproachingException(vicinityChecker.getLimit(), accountId, warningPercentage);
      }
    } catch (NoLimitConfiguredException e) {
      log.error(
          "No limit is configured for action: {}. Deployments will be allowed to maintain backward compatibility. But deployments are NOT being rate limited.",
          deployAction, e);
    }
  }

  // This is prevent against case where a customer might have a burst of deployment attempts and end up consuming all 24
  // limits and then are blocked for 24 hours. With this change they can only consume 40% of 24 hour limit in an hour.
  @Nullable
  private RateLimitChecker shortDurationChecker(RateLimitChecker longDurationChecker, Action action) {
    RateLimit limit = longDurationChecker.getLimit();

    if (!(limit.getDurationUnit() == LONG_LIMIT_DURATION_UNIT && limit.getDuration() == LONG_LIMIT_DURATION)) {
      return null;
    }

    // 40% of 24 hour limit allowed per hour
    return new MongoSlidingWindowRateLimitChecker(
        new RateLimit((int) (limit.getCount() * SHORT_LIMIT_PERCENT), 1, TimeUnit.HOURS), persistence,
        action.key() + "_hourly");
  }

  public void isDeploymentAllowed(String appId) throws WingsException {
    Application application = appService.get(appId, false);
    requireNonNull(application, "Application not found. Is the application ID correct? AppId: " + appId);
    String accountId = application.getAccountId();
    checkIfAccountExpired(accountId);
    checkIfDeploymentFreeze(accountId);
  }

  private void checkIfAccountExpired(String accountId) throws WingsException {
    boolean isAccountExpired = licenseService.isAccountExpired(accountId);
    if (isAccountExpired) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "License expired!!! Please contact Harness Support.");
    }
  }

  private void checkIfDeploymentFreeze(String accountId) throws WingsException {
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    if (governanceConfig == null) {
      return;
    }

    if (governanceConfig.isDeploymentFreeze()) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Deployment Freeze is active. No deployments are allowed.");
    }
  }

  private static int percentToWarnOn() {
    String percent = System.getenv().get("DEPLOYMENT_RATE_LIMIT_WARN_PERCENTAGE");
    if (null != percent) {
      try {
        return Integer.parseInt(percent);
      } catch (NumberFormatException e) {
        log.error("Error reading DEPLOYMENT_RATE_LIMIT_WARN_PERCENTAGE from env variables. Found Value: {}", percent);
        return PERCENT_TO_WARN_ON_DEFAULT;
      }
    }

    return PERCENT_TO_WARN_ON_DEFAULT;
  }
}

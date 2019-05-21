package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.FEAT_UNAVAILABLE_IN_COMMUNITY_VERSION;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.limits.InstanceUsageExceededLimitException;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.checker.LimitApproachingException;
import io.harness.limits.checker.UsageLimitExceededException;
import io.harness.limits.checker.rate.MongoSlidingWindowRateLimitChecker;
import io.harness.limits.checker.rate.RateLimitVicinityChecker;
import io.harness.limits.configuration.NoLimitConfiguredException;
import io.harness.limits.counter.service.CounterService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.lib.RateLimitChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.beans.instance.dashboard.InstanceStatsUtils;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.licensing.violations.checkers.PipelinePreDeploymentViolationChecker;
import software.wings.licensing.violations.checkers.WorkflowPreDeploymentViolationChecker;
import software.wings.licensing.violations.checkers.error.ValidationError;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Singleton
public class PreDeploymentChecker {
  @Inject private AppService appService;
  @Inject private LicenseService licenseService;
  @Inject private GovernanceConfigService governanceConfigService;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private WingsPersistence persistence;
  @Inject private InstanceUsageLimitChecker instanceUsageLimitChecker;
  @Inject private InstanceStatService instanceStatService;
  @Inject private AccountService accountService;
  @Inject private CounterService counterService;
  @Inject private WorkflowPreDeploymentViolationChecker workflowPreDeploymentViolationChecker;
  @Inject private PipelinePreDeploymentViolationChecker pipelinePreDeploymentViolationChecker;
  @Inject private MainConfiguration mainConfiguration;

  private static final Logger log = LoggerFactory.getLogger(PreDeploymentChecker.class);

  private static final int PERCENT_TO_WARN_ON_DEFAULT = 85;
  private static final int LONG_LIMIT_DURATION = 24;
  private static final TimeUnit LONG_LIMIT_DURATION_UNIT = TimeUnit.HOURS;
  private static final double SHORT_LIMIT_PERCENT = 0.4D;
  private static final int NUM_DAYS_TO_CHECK_FOR = 2;

  /**
   * checks if the deployments being done is within allowed rate limits.
   * @throws UsageLimitExceededException in case limit exceeds
   * @throws LimitApproachingException in case user is close to hitting limit. These exceptions should not block
   * deployments, useful to give some warnings, though.
   */
  public void checkDeploymentRateLimit(String accountId, String appId) throws UsageLimitExceededException {
    if (DeployMode.isOnPrem(mainConfiguration.getDeployMode().name())) {
      return;
    }

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

  public void checkInstanceUsageLimit(String accountId, String appId) throws InstanceUsageExceededLimitException {
    double ninetyFifthPercentileUsage = InstanceStatsUtils.actualUsage(accountId, instanceStatService);

    // has the customer reached 3x of limit?
    boolean within3x = instanceUsageLimitChecker.isWithinLimit(accountId, 300, ninetyFifthPercentileUsage);
    boolean isCommunityAccount = accountService.isCommunityAccount(accountId);

    if (!within3x) {
      if (isCommunityAccount) {
        log.info("Customer exceeded 3x the limit of their allowed service instance usage. accountId={}", accountId);
        throw new InstanceUsageExceededLimitException(
            accountId, ninetyFifthPercentileUsage, "Instance Usage Limit Exceeded.");
      } else {
        // logging as error to bring it in log alerts
        log.error("Non-Community customer exceeded 3x the limit of their allowed service instance usage. accountId={}",
            accountId);
      }
    }

    // has the customer's SI usage been greater than allowed usage for > X days?
    Counter counter = counterService.get(new Action(accountId, ActionType.INSTANCE_USAGE_LIMIT_EXCEEDED));
    if (isCommunityAccount && null != counter && counter.getValue() > NUM_DAYS_TO_CHECK_FOR) {
      log.info("SI usage has been over allowed usage for more than {} days. Deployments will be blocked. accountId={}",
          NUM_DAYS_TO_CHECK_FOR, accountId);
      throw new InstanceUsageExceededLimitException(
          accountId, ninetyFifthPercentileUsage, "Instance Usage Limit Exceeded.");
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

  public void checkIfWorkflowUsingRestrictedFeatures(@NotNull Workflow workflow) {
    Account account = accountService.get(workflow.getAccountId());
    if (account.isCommunity()) {
      List<ValidationError> validationErrorList = workflowPreDeploymentViolationChecker.checkViolations(workflow);
      if (isNotEmpty(validationErrorList)) {
        String validationMessage = validationErrorList.get(0).getMessage();
        log.warn("Pre-deployment restricted features check failed for workflowId ={} with reason={} ",
            workflow.getUuid(), validationMessage);
        throw new WingsException(FEAT_UNAVAILABLE_IN_COMMUNITY_VERSION, validationMessage, USER)
            .addParam("message", validationMessage);
      }
    }
  }

  public void checkIfPipelineUsingRestrictedFeatures(@NotNull Pipeline pipeline) {
    Account account = accountService.get(pipeline.getAccountId());
    if (account.isCommunity()) {
      List<ValidationError> validationErrorList = pipelinePreDeploymentViolationChecker.checkViolations(pipeline);
      if (isNotEmpty(validationErrorList)) {
        String validationMessage = validationErrorList.get(0).getMessage();
        log.warn("Pre-deployment restricted features check failed for pipelinedId ={} with reason={} ",
            pipeline.getUuid(), validationMessage);
        throw new WingsException(FEAT_UNAVAILABLE_IN_COMMUNITY_VERSION, validationMessage, WingsException.USER);
      }
    }
  }
}

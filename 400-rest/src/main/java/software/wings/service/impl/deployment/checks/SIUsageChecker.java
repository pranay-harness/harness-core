/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.deployment.checks;

import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.limits.InstanceUsageExceededLimitException;
import io.harness.limits.counter.service.CounterService;

import software.wings.beans.instance.dashboard.InstanceStatsUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.deployment.PreDeploymentChecker;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@ParametersAreNonnullByDefault
public class SIUsageChecker implements PreDeploymentChecker {
  private static final int NUM_DAYS_TO_CHECK_FOR = 2;

  private InstanceUsageLimitChecker instanceUsageLimitChecker;
  private AccountService accountService;
  private InstanceStatService instanceStatService;
  private CounterService counterService;

  @Inject
  public SIUsageChecker(InstanceUsageLimitChecker instanceUsageLimitChecker, AccountService accountService,
      InstanceStatService instanceStatService, CounterService counterService) {
    this.instanceUsageLimitChecker = instanceUsageLimitChecker;
    this.accountService = accountService;
    this.instanceStatService = instanceStatService;
    this.counterService = counterService;
  }

  @Override
  public void check(String accountId) throws InstanceUsageExceededLimitException {
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
        // logging as error to bring it in logger alerts
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
}

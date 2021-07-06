package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.migrations.Migration;

import software.wings.beans.Account;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class LoginRateLimitMigration implements Migration {
  @Inject LimitConfigurationService limitConfigurationService;

  private static final int globalRateLimit = 300;

  @Override
  public void migrate() {
    try {
      limitConfigurationService.configure(Account.GLOBAL_ACCOUNT_ID, ActionType.LOGIN_REQUEST_TASK,
          new RateLimit(globalRateLimit, 1, TimeUnit.MINUTES));
    } catch (Exception ex) {
      log.error("Exception while setting rate limit value during migration ", ex);
    }
  }
}

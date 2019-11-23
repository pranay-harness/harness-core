package migrations.all;

import com.google.inject.Inject;

import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;

import java.util.concurrent.TimeUnit;

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
      logger.error("Exception while setting rate limit value ", ex);
    }
  }
}

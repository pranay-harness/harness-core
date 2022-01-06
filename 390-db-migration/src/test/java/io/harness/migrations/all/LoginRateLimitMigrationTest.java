/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.lib.LimitType;
import io.harness.limits.lib.RateBasedLimit;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class LoginRateLimitMigrationTest extends WingsBaseTest {
  @Inject LimitConfigurationService limitConfigurationService;

  @InjectMocks @Inject private LoginRateLimitMigration loginRateLimitMigration;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testMigration() {
    loginRateLimitMigration.migrate();
    ConfiguredLimit<RateBasedLimit> configuredLimit =
        limitConfigurationService.getOrDefault(Account.GLOBAL_ACCOUNT_ID, ActionType.LOGIN_REQUEST_TASK);
    assertThat(configuredLimit.getLimit().getDuration()).isEqualTo(1);
    assertThat(configuredLimit.getLimit().getLimitType()).isEqualTo(LimitType.RATE_LIMIT);
    assertThat(configuredLimit.getLimit().getDurationUnit()).isEqualTo(TimeUnit.MINUTES);
  }
}

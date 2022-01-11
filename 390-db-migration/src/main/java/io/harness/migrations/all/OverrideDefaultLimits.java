/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.migrations.Migration;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;

public class OverrideDefaultLimits implements Migration {
  @Inject private LimitConfigurationService limitConfigurationService;

  @Override
  public void migrate() {
    // iHerb
    limitConfigurationService.configure(
        "bwBVO7N0RmKltRhTjk101A", ActionType.DEPLOY, new RateLimit(400, 24, TimeUnit.HOURS));
  }
}

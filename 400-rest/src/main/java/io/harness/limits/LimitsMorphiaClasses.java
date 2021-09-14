/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.limits.checker.rate.UsageBucket;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
@TargetModule(HarnessModule._957_CG_BEANS)
public class LimitsMorphiaClasses {
  public static final Set<Class> classes =
      ImmutableSet.<Class>of(Counter.class, ConfiguredLimit.class, UsageBucket.class);
}

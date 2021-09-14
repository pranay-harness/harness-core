/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sample.cd.creator;

import io.harness.pms.sdk.core.plan.creation.creators.SimpleStepPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class CdStepPlanCreator extends SimpleStepPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("k8sCanary", "k8sRolling");
  }
}

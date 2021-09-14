/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.cdng.services.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.cdng.beans.CVNGStepType;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;
@OwnedBy(HarnessTeam.CV)
public class CVNGPlanCreator extends GenericStepPMSPlanCreator {
  public static final Set<String> CVNG_SUPPORTED_TYPES = Sets.newHashSet(CVNGStepType.CVNG_VERIFY.getDisplayName());
  @Override
  public Set<String> getSupportedStepTypes() {
    return CVNG_SUPPORTED_TYPES;
  }
}

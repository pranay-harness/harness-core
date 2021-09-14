/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.creator.plan.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.CDCreatorUtils;
import io.harness.filters.GenericStepPMSFilterJsonCreator;

import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class CDPMSStepFilterJsonCreator extends GenericStepPMSFilterJsonCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return CDCreatorUtils.getSupportedSteps();
  }
}

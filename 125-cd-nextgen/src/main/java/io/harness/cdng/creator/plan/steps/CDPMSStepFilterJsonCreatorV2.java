/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.CDCreatorUtils;
import io.harness.filters.GenericStepPMSFilterJsonCreatorV2;

import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class CDPMSStepFilterJsonCreatorV2 extends GenericStepPMSFilterJsonCreatorV2 {
  @Override
  public Set<String> getSupportedStepTypes() {
    return CDCreatorUtils.getSupportedStepsV2();
  }
}

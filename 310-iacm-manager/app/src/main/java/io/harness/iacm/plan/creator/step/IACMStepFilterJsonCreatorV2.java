/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.iacm.plan.creator.step;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filters.GenericStepPMSFilterJsonCreatorV2;
import io.harness.iacm.plan.creator.IACMCreatorUtils;

import java.util.Set;

@OwnedBy(HarnessTeam.IACM)
public class IACMStepFilterJsonCreatorV2 extends GenericStepPMSFilterJsonCreatorV2 {
  @Override
  public Set<String> getSupportedStepTypes() {
    return IACMCreatorUtils.getSupportedStepsV2();
  }
}
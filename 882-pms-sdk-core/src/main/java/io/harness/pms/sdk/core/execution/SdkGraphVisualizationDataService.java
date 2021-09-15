/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.StepDetailsInfo;

@OwnedBy(HarnessTeam.PIPELINE)
public interface SdkGraphVisualizationDataService {
  void publishStepDetailInformation(Ambiance ambiance, StepDetailsInfo stepDetailsInfo, String name);
}

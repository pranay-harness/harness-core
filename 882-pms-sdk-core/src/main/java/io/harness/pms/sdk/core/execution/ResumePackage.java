/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.execution;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ResumePackage {
  @NonNull Ambiance ambiance;
  @NonNull StepParameters stepParameters;
  StepInputPackage stepInputPackage;
  Map<String, ResponseData> responseDataMap;
  ChainDetails chainDetails;
}

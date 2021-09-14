/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.steps.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Use this interface whn you want to perform synchronous requests.
 *
 * InterfaceDefinition:
 *
 * executeSync: This straight away responds with {@link StepResponse}
 */
@OwnedBy(CDC)
public interface SyncExecutable<T extends StepParameters> extends Step<T> {
  StepResponse executeSync(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData);

  default List<String> getLogKeys(Ambiance ambiance) {
    return new ArrayList<>();
  }

  default List<String> getCommandUnits(Ambiance ambiance) {
    return new ArrayList<>();
  }
}

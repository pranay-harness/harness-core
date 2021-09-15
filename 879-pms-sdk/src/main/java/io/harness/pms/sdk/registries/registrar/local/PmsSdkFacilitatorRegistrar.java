/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.registries.registrar.local;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.execution.events.node.facilitate.Facilitator;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class PmsSdkFacilitatorRegistrar {
  public Map<FacilitatorType, Class<? extends Facilitator>> getEngineFacilitators() {
    return new HashMap<>();
  }
}

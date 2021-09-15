/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.execution.events.node.facilitate.Facilitator;
import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;

@OwnedBy(CDC)
@Singleton
public class FacilitatorRegistry implements Registry<FacilitatorType, Facilitator> {
  private Map<FacilitatorType, Facilitator> registry = new ConcurrentHashMap<>();

  public void register(FacilitatorType facilitatorType, Facilitator facilitator) {
    if (registry.containsKey(facilitatorType)) {
      throw new DuplicateRegistryException(getType(), "Facilitator Already Registered with type: " + facilitatorType);
    }
    registry.put(facilitatorType, facilitator);
  }

  public Facilitator obtain(@Valid FacilitatorType facilitatorType) {
    if (registry.containsKey(facilitatorType)) {
      return registry.get(facilitatorType);
    }
    throw new UnregisteredKeyAccessException(getType(), "No Facilitator registered for type: " + facilitatorType);
  }

  @Override
  public String getType() {
    return RegistryType.FACILITATOR.name();
  }
}

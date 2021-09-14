/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.facilitation.facilitator;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;

// Marker Interface for Facilitators owned by the Pipeline service. These facilitators do not need to queue any event
public interface CoreFacilitator {
  FacilitatorResponseProto facilitate(Ambiance ambiance, byte[] parameters);
}

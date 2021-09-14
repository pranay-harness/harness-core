/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.NGInstanceUnitType;

@OwnedBy(CDP)
public enum K8sInstanceUnitType {
  Count(NGInstanceUnitType.COUNT),
  Percentage(NGInstanceUnitType.PERCENTAGE);

  private final NGInstanceUnitType instanceUnitType;

  K8sInstanceUnitType(NGInstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  public NGInstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }
}

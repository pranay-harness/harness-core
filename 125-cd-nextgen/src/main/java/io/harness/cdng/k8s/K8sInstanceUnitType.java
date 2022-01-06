/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

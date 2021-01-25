package io.harness.cdng.k8s;

import io.harness.beans.NGInstanceUnitType;

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

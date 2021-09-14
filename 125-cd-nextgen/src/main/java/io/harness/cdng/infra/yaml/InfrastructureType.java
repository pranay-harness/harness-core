/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.infra.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

@OwnedBy(HarnessTeam.CDP)
public enum InfrastructureType {
  @JsonProperty(InfrastructureKind.KUBERNETES_DIRECT) KUBERNETES_DIRECT(InfrastructureKind.KUBERNETES_DIRECT),
  @JsonProperty(InfrastructureKind.KUBERNETES_GCP) KUBERNETES_GCP(InfrastructureKind.KUBERNETES_GCP);
  private final String displayName;

  InfrastructureType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  @JsonCreator
  public static InfrastructureType getInfrastructureType(@JsonProperty("type") String displayName) {
    for (InfrastructureType infrastructureType : InfrastructureType.values()) {
      if (infrastructureType.displayName.equalsIgnoreCase(displayName)) {
        return infrastructureType;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", displayName, Arrays.toString(InfrastructureType.values())));
  }
}

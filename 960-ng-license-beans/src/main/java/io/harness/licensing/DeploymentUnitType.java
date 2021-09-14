/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(HarnessTeam.GTM)
public enum DeploymentUnitType {
  @JsonProperty("SERVICE") SERVICE,
  @JsonProperty("FUNCTION") FUNCTION;

  @JsonCreator
  public static DeploymentUnitType fromString(String type) {
    for (DeploymentUnitType typeEnum : DeploymentUnitType.values()) {
      if (typeEnum.name().equalsIgnoreCase(type)) {
        return typeEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + type);
  }
}

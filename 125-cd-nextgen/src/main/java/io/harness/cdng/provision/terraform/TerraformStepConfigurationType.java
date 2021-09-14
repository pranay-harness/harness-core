/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@OwnedBy(CDP)
public enum TerraformStepConfigurationType {
  @JsonProperty("Inline") INLINE("Inline"),
  @JsonProperty("InheritFromPlan") INHERIT_FROM_PLAN("InheritFromPlan"),
  @JsonProperty("InheritFromApply") INHERIT_FROM_APPLY("InheritFromApply");

  @Getter private final String displayName;
  TerraformStepConfigurationType(String displayName) {
    this.displayName = displayName;
  }

  @JsonCreator
  public static TerraformStepConfigurationType getConfigurationType(String displayName) {
    for (TerraformStepConfigurationType value : TerraformStepConfigurationType.values()) {
      if (value.displayName.equalsIgnoreCase(displayName)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }
}

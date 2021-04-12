package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@OwnedBy(CDP)
public enum TerraformStepConfigurationType {
  INLINE("Inline"),
  INHERIT_FROM_PLAN("InheritFromPlan"),
  INHERIT_FROM_APPLY("InheritFromApply");

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

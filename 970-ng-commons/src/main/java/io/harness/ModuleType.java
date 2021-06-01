package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PL)
public enum ModuleType {
  @JsonProperty("CD") CD("Continuous Deployment"),
  @JsonProperty("CI") CI("Continuous Integeration"),
  @JsonProperty("CORE") CORE("Core"),
  @JsonProperty("CV") CV("Continuous Verification"),
  @JsonProperty("CF") CF("Continuous Features"),
  @JsonProperty("CE") CE("Continuous Efficiency");

  String displayName;
  ModuleType(String displayName) {
    this.displayName = displayName;
  }
  @JsonCreator
  public static ModuleType fromString(String moduleType) {
    for (ModuleType moduleEnum : ModuleType.values()) {
      if (moduleEnum.name().equalsIgnoreCase(moduleType)) {
        return moduleEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + moduleType);
  }

  public String getDisplayName() {
    return displayName;
  }
}

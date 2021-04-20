package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PL)
public enum ModuleType {
  @JsonProperty("CD") CD,
  @JsonProperty("CI") CI,
  @JsonProperty("CORE") CORE,
  @JsonProperty("CV") CV,
  @JsonProperty("CF") CF,
  @JsonProperty("CE") CE;

  @JsonCreator
  public static ModuleType fromString(String moduleType) {
    for (ModuleType moduleEnum : ModuleType.values()) {
      if (moduleEnum.name().equalsIgnoreCase(moduleType)) {
        return moduleEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + moduleType);
  }
}

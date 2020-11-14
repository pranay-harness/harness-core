package io.harness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

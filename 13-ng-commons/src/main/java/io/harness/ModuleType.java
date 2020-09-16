package io.harness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ModuleType {
  @JsonProperty("cd") CD,
  @JsonProperty("ci") CI,
  @JsonProperty("core") CORE,
  @JsonProperty("cv") CV;

  // todo(abhinav): change product to module type for json creater
  @JsonCreator
  public static ModuleType fromString(@JsonProperty("product") String product) {
    for (ModuleType moduleEnum : ModuleType.values()) {
      if (moduleEnum.name().equalsIgnoreCase(product)) {
        return moduleEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + product);
  }
}

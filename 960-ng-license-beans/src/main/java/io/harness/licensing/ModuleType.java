package io.harness.licensing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(HarnessTeam.GTM)
public enum ModuleType {
  @JsonProperty("CD") CD,
  @JsonProperty("CI") CI,
  @JsonProperty("CV") CV,
  @JsonProperty("CE") CE,
  @JsonProperty("CF") CF;

  @JsonCreator
  public static io.harness.ModuleType fromString(String moduleType) {
    for (io.harness.ModuleType moduleEnum : io.harness.ModuleType.values()) {
      if (moduleEnum.name().equalsIgnoreCase(moduleType)) {
        return moduleEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + moduleType);
  }
}

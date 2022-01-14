package io.harness.azure.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(CDP)
public enum ARMResourceType {
  ARM("ARM"),
  BLUEPRINT("BLUEPRINT");

  ARMResourceType(String value) {
    this.value = value;
  }

  private final String value;

  @JsonCreator
  public static ARMResourceType fromString(final String value) {
    for (ARMResourceType type : ARMResourceType.values()) {
      if (type.toString().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException(String.format("Unrecognized ARM resource type, value: %s,", value));
  }

  @JsonValue
  @Override
  public String toString() {
    return this.value;
  }
}

package io.harness.azure.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(CDP)
public enum ARMScopeType {
  RESOURCE_GROUP("RESOURCE_GROUP"),
  SUBSCRIPTION("SUBSCRIPTION"),
  MANAGEMENT_GROUP("MANAGEMENT_GROUP"),
  TENANT("TENANT");

  ARMScopeType(String value) {
    this.value = value;
  }

  private final String value;

  @JsonCreator
  public static ARMScopeType fromString(final String value) {
    for (ARMScopeType type : ARMScopeType.values()) {
      if (type.toString().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException(String.format("Unrecognized ARM scope type, value: %s,", value));
  }

  @JsonValue
  @Override
  public String toString() {
    return this.value;
  }
}

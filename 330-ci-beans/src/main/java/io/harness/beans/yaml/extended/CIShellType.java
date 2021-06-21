package io.harness.beans.yaml.extended;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CIShellType {
  @JsonProperty("SH") SH("SH"),
  @JsonProperty("BASH") BASH("BASH");
  private final String displayName;

  @JsonCreator
  public static CIShellType getShellType(@JsonProperty("shellType") String displayName) {
    for (CIShellType shellType : CIShellType.values()) {
      if (shellType.displayName.equalsIgnoreCase(displayName)) {
        return shellType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }

  CIShellType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  public static CIShellType fromString(final String s) {
    return CIShellType.getShellType(s);
  }
}
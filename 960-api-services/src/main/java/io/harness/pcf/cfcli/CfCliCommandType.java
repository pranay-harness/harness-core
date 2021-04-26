package io.harness.pcf.cfcli;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.CDP)
public enum CfCliCommandType {
  VERSION(StringUtils.EMPTY),
  LOGIN("login");

  CfCliCommandType(String value) {
    this.value = value;
  }

  private final String value;

  public static CfCliCommandType fromString(final String value) {
    for (CfCliCommandType type : CfCliCommandType.values()) {
      if (type.toString().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException(String.format("Unrecognized CF CLI command, value: %s,", value));
  }

  @Override
  public String toString() {
    return this.value;
  }
}

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum ServiceNowActionNG {
  VALIDATE_CREDENTIALS("Validate Credentials");

  private final String displayName;

  ServiceNowActionNG(String s) {
    displayName = s;
  }

  public String getDisplayName() {
    return displayName;
  }
}

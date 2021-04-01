package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum JiraActionNG {
  VALIDATE_CREDENTIALS("Validate Credentials"),
  GET_PROJECTS("Get Projects"),
  GET_ISSUE("Get Issue"),
  GET_ISSUE_CREATE_METADATA("Get Issue Create Metadata"),
  CREATE_ISSUE("Create Issue"),
  UPDATE_ISSUE("Update Issue");

  private final String displayName;

  JiraActionNG(String s) {
    displayName = s;
  }

  public String getDisplayName() {
    return displayName;
  }
}

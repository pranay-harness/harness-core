package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum JiraAction {
  CREATE_TICKET("Create Ticket"),
  UPDATE_TICKET("Update Ticket"),
  AUTH("Auth"),

  GET_PROJECTS("Get Projects"),
  GET_FIELDS_OPTIONS("Get Field Options"),
  GET_STATUSES("Get Statuses"),
  GET_CREATE_METADATA("Get Create Metadata"),

  FETCH_ISSUE("Fetch Issue"),
  FETCH_ISSUE_DATA("Fetch Issue Details"),
  CHECK_APPROVAL("Check Jira Approval");

  private final String displayName;

  JiraAction(String s) {
    displayName = s;
  }

  public String getDisplayName() {
    return displayName;
  }
}

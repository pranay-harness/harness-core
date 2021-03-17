package io.harness.pms.approval.beans;

import io.harness.EntitySubtype;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApprovalType implements EntitySubtype {
  @JsonProperty(ApprovalTypeConstants.HARNESS_APPROVAL) HARNESS_APPROVAL(ApprovalTypeConstants.HARNESS_APPROVAL),
  @JsonProperty(ApprovalTypeConstants.JIRA_APPROVAL) JIRA_APPROVAL(ApprovalTypeConstants.JIRA_APPROVAL);

  private final String displayName;

  ApprovalType(String displayName) {
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
}

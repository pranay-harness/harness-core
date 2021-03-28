package io.harness.steps.approval.step.jira.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CriteriaSpecType {
  @JsonProperty("Jexl") JEXL("Jexl"),
  @JsonProperty("KeyValues") KEY_VALUES("KeyValues");

  private final String displayName;

  CriteriaSpecType(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}

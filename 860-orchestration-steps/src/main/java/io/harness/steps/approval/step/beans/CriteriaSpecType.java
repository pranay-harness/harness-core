package io.harness.steps.approval.step.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(CDC)
public enum CriteriaSpecType {
  @JsonProperty(CriteriaSpecTypeConstants.JEXL) JEXL(CriteriaSpecTypeConstants.JEXL),
  @JsonProperty(CriteriaSpecTypeConstants.KEY_VALUES) KEY_VALUES(CriteriaSpecTypeConstants.KEY_VALUES);

  private final String displayName;

  CriteriaSpecType(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}

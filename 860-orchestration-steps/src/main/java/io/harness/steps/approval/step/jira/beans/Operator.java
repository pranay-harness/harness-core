package io.harness.steps.approval.step.jira.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Operator {
  @JsonProperty("equals") EQ("equals"),
  @JsonProperty("not equals") NOT_EQ("not equals"),
  @JsonProperty("contains") CONTAINS("contains"),
  @JsonProperty("in") IN("in"),
  @JsonProperty("not in") NOT_IN("not in");
  String name;

  Operator(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}

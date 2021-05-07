package io.harness.ngtriggers.beans.source.webhook.v1.github.action;

import io.harness.ngtriggers.beans.source.webhook.v1.git.GitAction;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GithubIssueCommentAction implements GitAction {
  @JsonProperty("Create") CREATED("create", "Create"),
  @JsonProperty("Edit") EDITED("edit", "Edit"),
  @JsonProperty("Delete") DELETED("delete", "Delete");

  private String value;
  private String parsedValue;

  GithubIssueCommentAction(String parsedValue, String value) {
    this.parsedValue = parsedValue;
    this.value = value;
  }

  public String getParsedValue() {
    return parsedValue;
  }

  public String getValue() {
    return value;
  }
}

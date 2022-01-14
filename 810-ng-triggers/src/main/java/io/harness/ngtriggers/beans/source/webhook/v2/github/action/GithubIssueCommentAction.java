package io.harness.ngtriggers.beans.source.webhook.v2.github.action;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum GithubIssueCommentAction implements GitAction {
  @JsonProperty("Create") CREATE("create", "Create"),
  @JsonProperty("Edit") EDIT("edit", "Edit"),
  @JsonProperty("Delete") DELETE("delete", "Delete");

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

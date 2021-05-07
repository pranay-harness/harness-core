package io.harness.ngtriggers.beans.source.webhook.github.action;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GithubIssueCommentAction {
  @JsonProperty("created") CREATED("create", "created"),
  @JsonProperty("edited") EDITED("edit", "edited"),
  @JsonProperty("deleted") DELETED("delete", "deleted");

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

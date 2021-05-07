package io.harness.ngtriggers.beans.source.webhook.v1.github.event;

import io.harness.ngtriggers.beans.source.webhook.v1.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GithubTriggerEvent implements GitEvent {
  @JsonProperty("Pull Request") PULL_REQUEST("Pull Request"),
  @JsonProperty("Push") PUSH("Push"),
  @JsonProperty("Issue Comment") ISSUE_COMMENT("Issue Comment");

  private String value;

  GithubTriggerEvent(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}

package io.harness.ngtriggers.beans.source.webhook.github.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GithubTriggerEvent {
  @JsonProperty("Pull Request") PULL_REQUEST,
  @JsonProperty("Push") PUSH,
  @JsonProperty("Issue Comment") ISSUE_COMMENT
}

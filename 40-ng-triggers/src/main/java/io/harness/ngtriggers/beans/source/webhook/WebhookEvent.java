package io.harness.ngtriggers.beans.source.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum WebhookEvent {
  @JsonProperty("Pull Request") PULL_REQUEST,
  @JsonProperty("Push") PUSH,
  @JsonProperty("Issue") ISSUE
}
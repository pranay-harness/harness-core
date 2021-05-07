package io.harness.ngtriggers.beans.source.webhook.v1.bitbucket.event;

import io.harness.ngtriggers.beans.source.webhook.v1.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BitbucketTriggerEvent implements GitEvent {
  @JsonProperty("Pull Request") PULL_REQUEST("Pull Request"),
  @JsonProperty("Push") PUSH("Push");

  private String value;

  BitbucketTriggerEvent(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}

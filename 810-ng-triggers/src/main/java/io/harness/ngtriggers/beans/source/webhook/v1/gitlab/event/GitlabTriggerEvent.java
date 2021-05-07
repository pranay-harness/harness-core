package io.harness.ngtriggers.beans.source.webhook.v1.gitlab.event;

import io.harness.ngtriggers.beans.source.webhook.v1.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GitlabTriggerEvent implements GitEvent {
  @JsonProperty("Merge Request") MERGE_REQUEST("Merge Request"),
  @JsonProperty("Push") PUSH("Push");

  private String value;

  GitlabTriggerEvent(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}

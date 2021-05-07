package io.harness.ngtriggers.beans.source.webhook.v1.awscodecommit.event;

import io.harness.ngtriggers.beans.source.webhook.v1.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AwsCodeCommitTriggerEvent implements GitEvent {
  @JsonProperty("Push") PUSH("Push");

  private String value;

  AwsCodeCommitTriggerEvent(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}

package io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.Constants.PULL_REQUEST_EVENT_TYPE;
import static io.harness.ngtriggers.Constants.PUSH_EVENT_TYPE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum BitbucketTriggerEvent implements GitEvent {
  @JsonProperty(PULL_REQUEST_EVENT_TYPE) PULL_REQUEST(PULL_REQUEST_EVENT_TYPE),
  @JsonProperty(PUSH_EVENT_TYPE) PUSH(PUSH_EVENT_TYPE);

  private String value;

  BitbucketTriggerEvent(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}

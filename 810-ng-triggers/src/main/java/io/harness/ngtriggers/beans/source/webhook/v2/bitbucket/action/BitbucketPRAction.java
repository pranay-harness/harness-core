package io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.action;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum BitbucketPRAction implements GitAction {
  @JsonProperty("Create") PULL_REQUEST_CREATED("open", "Create"),
  @JsonProperty("Update") PULL_REQUEST_UPDATED("sync", "Update"),
  @JsonProperty("Merge") PULL_REQUEST_MERGED("merge", "Merge"),
  @JsonProperty("Decline") PULL_REQUEST_DECLINED("close", "Decline");

  private String value;
  private String parsedValue;

  BitbucketPRAction(String parsedValue, String value) {
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

package io.harness.ngtriggers.beans.source.webhook.bitbucket.action;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BitbucketPRAction {
  @JsonProperty("create") PULL_REQUEST_CREATED("open", "create"),
  @JsonProperty("update") PULL_REQUEST_UPDATED("sync", "update"),
  @JsonProperty("merge") PULL_REQUEST_MERGED("merge", "merge"),
  @JsonProperty("decline") PULL_REQUEST_DECLINED("close", "decline");

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

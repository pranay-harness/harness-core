package io.harness.ngtriggers.beans.source.webhook.gitlab.action;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GitlabPRAction {
  @JsonProperty("open") OPEN("open", "open"),
  @JsonProperty("close") CLOSE("close", "close"),
  @JsonProperty("reopen") REOPEN("reopen", "reopen"),
  @JsonProperty("merge") MERGED("merge", "merge"),
  @JsonProperty("update") UPDATED("update", "update"),
  @JsonProperty("sync") SYNC("sync", "sync");

  private String value;
  private String parsedValue;

  GitlabPRAction(String parsedValue, String value) {
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

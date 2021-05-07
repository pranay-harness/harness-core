package io.harness.ngtriggers.beans.source.webhook.github.action;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GithubPRAction {
  @JsonProperty("close") CLOSED("close", "close"),
  @JsonProperty("edit") EDITED("edit", "edit"),
  @JsonProperty("open") OPENED("open", "open"),
  @JsonProperty("reopen") REOPENED("reopen", "reopen"),
  @JsonProperty("label") LABELED("label", "label"),
  @JsonProperty("unlabel") UNLABELED("unlabel", "unlabel"),
  @JsonProperty("synchronize") SYNCHRONIZED("sync", "synchronize");

  private String value;
  private String parsedValue;

  GithubPRAction(String parsedValue, String value) {
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

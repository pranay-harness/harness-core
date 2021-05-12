package io.harness.ngtriggers.beans.source.webhook.v2.github.action;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum GithubPRAction implements GitAction {
  @JsonProperty("Close") CLOSED("close", "Close"),
  @JsonProperty("Edit") EDITED("edit", "Edit"),
  @JsonProperty("Open") OPENED("open", "Open"),
  @JsonProperty("Reopen") REOPENED("reopen", "Reopen"),
  @JsonProperty("Label") LABELED("label", "Label"),
  @JsonProperty("Unlabel") UNLABELED("unlabel", "Unlabel"),
  @JsonProperty("Synchronize") SYNCHRONIZED("sync", "Synchronize");

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

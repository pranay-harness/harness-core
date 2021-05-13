package io.harness.ngtriggers.beans.source.webhook.v2.gitlab.action;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum GitlabPRAction implements GitAction {
  @JsonProperty("Open") OPEN("open", "Open"),
  @JsonProperty("Close") CLOSE("close", "Close"),
  @JsonProperty("Reopen") REOPEN("reopen", "Reopen"),
  @JsonProperty("Merge") MERGED("merge", "Merge"),
  @JsonProperty("Update") UPDATED("update", "Update"),
  @JsonProperty("Sync") SYNC("sync", "Sync");

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

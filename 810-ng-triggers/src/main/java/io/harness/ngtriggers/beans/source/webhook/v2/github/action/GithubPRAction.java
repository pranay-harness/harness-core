/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.source.webhook.v2.github.action;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum GithubPRAction implements GitAction {
  @JsonProperty("Close") CLOSE("close", "Close"),
  @JsonProperty("Edit") EDIT("edit", "Edit"),
  @JsonProperty("Open") OPEN("open", "Open"),
  @JsonProperty("Reopen") REOPEN("reopen", "Reopen"),
  @JsonProperty("Label") LABEL("label", "Label"),
  @JsonProperty("Unlabel") UNLABEL("unlabel", "Unlabel"),
  @JsonProperty("Synchronize") SYNCHRONIZE("sync", "Synchronize");

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

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.scm.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GithubApiAccessType {
  @JsonProperty(GithubConnectorConstants.GITHUB_APP) GITHUB_APP(GithubConnectorConstants.GITHUB_APP),
  @JsonProperty(GithubConnectorConstants.TOKEN) TOKEN(GithubConnectorConstants.TOKEN);

  private final String displayName;

  GithubApiAccessType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}

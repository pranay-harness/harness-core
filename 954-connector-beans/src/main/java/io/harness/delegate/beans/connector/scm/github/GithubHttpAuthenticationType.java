/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.scm.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GithubHttpAuthenticationType {
  @JsonProperty(GithubConnectorConstants.USERNAME_AND_PASSWORD)
  USERNAME_AND_PASSWORD(GithubConnectorConstants.USERNAME_AND_PASSWORD),
  @JsonProperty(GithubConnectorConstants.USERNAME_AND_TOKEN)
  USERNAME_AND_TOKEN(GithubConnectorConstants.USERNAME_AND_TOKEN);

  private final String displayName;

  GithubHttpAuthenticationType(String displayName) {
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

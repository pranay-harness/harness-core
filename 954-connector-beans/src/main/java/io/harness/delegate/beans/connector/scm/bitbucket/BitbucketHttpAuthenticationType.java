/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.scm.bitbucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BitbucketHttpAuthenticationType {
  @JsonProperty(BitbucketConnectorConstants.USERNAME_AND_PASSWORD)
  USERNAME_AND_PASSWORD(BitbucketConnectorConstants.USERNAME_AND_PASSWORD);

  private final String displayName;

  BitbucketHttpAuthenticationType(String displayName) {
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

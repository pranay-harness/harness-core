/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.scm.awscodecommit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AwsCodeCommitUrlType {
  @JsonProperty(AwsCodeCommitConnectorConstants.REPO) REPO(AwsCodeCommitConnectorConstants.REPO),
  @JsonProperty(AwsCodeCommitConnectorConstants.REGION) REGION(AwsCodeCommitConnectorConstants.REGION);

  private final String displayName;

  AwsCodeCommitUrlType(String displayName) {
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

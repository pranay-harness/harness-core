package io.harness.delegate.beans.connector.scm.awscodecommit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AwsCodeCommitHttpsAuthType {
  @JsonProperty(AwsCodeCommitConnectorConstants.ACCESS_KEY_AND_SECRET_KEY)
  ACCESS_KEY_AND_SECRET_KEY(AwsCodeCommitConnectorConstants.ACCESS_KEY_AND_SECRET_KEY);

  private final String displayName;

  AwsCodeCommitHttpsAuthType(String displayName) {
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

package io.harness.delegate.beans.connector.scm.awscodecommit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AwsCodeCommitAuthType {
  @JsonProperty(AwsCodeCommitConnectorConstants.HTTPS) HTTPS(AwsCodeCommitConnectorConstants.HTTPS);

  private final String displayName;

  AwsCodeCommitAuthType(String displayName) {
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

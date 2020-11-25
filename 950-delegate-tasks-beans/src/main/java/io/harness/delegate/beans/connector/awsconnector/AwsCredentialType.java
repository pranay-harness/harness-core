package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AwsCredentialType {
  @JsonProperty(AwsConstants.INHERIT_FROM_DELEGATE) INHERIT_FROM_DELEGATE(AwsConstants.INHERIT_FROM_DELEGATE),
  @JsonProperty(AwsConstants.MANUAL_CONFIG) MANUAL_CREDENTIALS(AwsConstants.MANUAL_CONFIG);

  private final String displayName;

  AwsCredentialType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  @JsonValue
  final String displayName() {
    return this.displayName;
  }
}

package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AwsCredentialType {
  @JsonProperty(AwsConstants.inheritFromDelegate) INHERIT_FROM_DELEGATE(AwsConstants.inheritFromDelegate),
  @JsonProperty(AwsConstants.manualConfig) MANUAL_CREDENTIALS(AwsConstants.manualConfig);

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

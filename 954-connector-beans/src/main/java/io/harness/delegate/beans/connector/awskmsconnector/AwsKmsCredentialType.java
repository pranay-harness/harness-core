package io.harness.delegate.beans.connector.awskmsconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AwsKmsCredentialType {
  @JsonProperty(AwsKmsConstants.ASSUME_IAM_ROLE) ASSUME_IAM_ROLE(AwsKmsConstants.ASSUME_IAM_ROLE),
  @JsonProperty(AwsKmsConstants.ASSUME_STS_ROLE) ASSUME_STS_ROLE(AwsKmsConstants.ASSUME_STS_ROLE),
  @JsonProperty(AwsKmsConstants.MANUAL_CONFIG) MANUAL_CONFIG(AwsKmsConstants.MANUAL_CONFIG);
  private final String displayName;

  AwsKmsCredentialType(String displayName) {
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

  public static AwsKmsCredentialType fromString(String typeEnum) {
    for (AwsKmsCredentialType enumValue : AwsKmsCredentialType.values()) {
      if (enumValue.getDisplayName().equals(typeEnum)) {
        return enumValue;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + typeEnum);
  }
}

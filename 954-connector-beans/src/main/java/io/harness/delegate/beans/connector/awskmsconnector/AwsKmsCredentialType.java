/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(PL)
public enum AwsKmsCredentialType {
  @JsonProperty(io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants.ASSUME_IAM_ROLE)
  ASSUME_IAM_ROLE(io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants.ASSUME_IAM_ROLE),
  @JsonProperty(io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants.ASSUME_STS_ROLE)
  ASSUME_STS_ROLE(io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants.ASSUME_STS_ROLE),
  @JsonProperty(io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants.MANUAL_CONFIG)
  MANUAL_CONFIG(io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants.MANUAL_CONFIG);
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

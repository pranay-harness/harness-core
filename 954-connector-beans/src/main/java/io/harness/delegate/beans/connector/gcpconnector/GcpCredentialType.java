/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GcpCredentialType {
  @JsonProperty(GcpConstants.INHERIT_FROM_DELEGATE) INHERIT_FROM_DELEGATE(GcpConstants.INHERIT_FROM_DELEGATE),
  @JsonProperty(GcpConstants.MANUAL_CONFIG) MANUAL_CREDENTIALS(GcpConstants.MANUAL_CONFIG);

  private final String displayName;

  GcpCredentialType(String displayName) {
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

  public static GcpCredentialType fromString(String typeEnum) {
    for (GcpCredentialType enumValue : GcpCredentialType.values()) {
      if (enumValue.getDisplayName().equals(typeEnum)) {
        return enumValue;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + typeEnum);
  }
}

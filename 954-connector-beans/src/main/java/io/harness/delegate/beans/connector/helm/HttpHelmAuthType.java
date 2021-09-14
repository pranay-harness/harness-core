/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.helm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HttpHelmAuthType {
  @JsonProperty(HelmConstants.USERNAME_PASSWORD) USER_PASSWORD(HelmConstants.USERNAME_PASSWORD),
  @JsonProperty(HelmConstants.ANONYMOUS) ANONYMOUS(HelmConstants.ANONYMOUS);

  private final String displayName;

  HttpHelmAuthType(String displayName) {
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

  public static HttpHelmAuthType fromString(String typeEnum) {
    for (HttpHelmAuthType enumValue : HttpHelmAuthType.values()) {
      if (enumValue.getDisplayName().equals(typeEnum)) {
        return enumValue;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + typeEnum);
  }
}

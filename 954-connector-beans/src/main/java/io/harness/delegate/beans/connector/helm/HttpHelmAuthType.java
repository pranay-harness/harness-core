/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

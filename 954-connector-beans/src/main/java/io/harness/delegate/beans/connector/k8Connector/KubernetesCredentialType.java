/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum KubernetesCredentialType {
  @JsonProperty("InheritFromDelegate") INHERIT_FROM_DELEGATE("InheritFromDelegate", false),
  @JsonProperty("ManualConfig") MANUAL_CREDENTIALS("ManualConfig", true);

  private final String displayName;
  private final boolean decryptable;

  KubernetesCredentialType(String displayName, boolean decryptable) {
    this.displayName = displayName;
    this.decryptable = decryptable;
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

  @JsonIgnore
  public boolean isDecryptable() {
    return decryptable;
  }
  public static KubernetesCredentialType fromString(String typeEnum) {
    for (KubernetesCredentialType enumValue : KubernetesCredentialType.values()) {
      if (enumValue.getDisplayName().equals(typeEnum)) {
        return enumValue;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + typeEnum);
  }
}

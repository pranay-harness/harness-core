/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum KubernetesAuthType {
  @JsonProperty("UsernamePassword") USER_PASSWORD("UsernamePassword"),
  @JsonProperty("ClientKeyCert") CLIENT_KEY_CERT("ClientKeyCert"),
  @JsonProperty("ServiceAccount") SERVICE_ACCOUNT("ServiceAccount"),
  @JsonProperty("OpenIdConnect") OPEN_ID_CONNECT("OpenIdConnect");

  private final String displayName;

  KubernetesAuthType(String displayName) {
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

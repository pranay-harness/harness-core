/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

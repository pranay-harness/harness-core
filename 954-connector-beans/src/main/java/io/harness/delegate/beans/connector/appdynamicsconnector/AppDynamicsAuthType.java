/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.appdynamicsconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(HarnessTeam.CV)
public enum AppDynamicsAuthType {
  @JsonProperty("UsernamePassword") USERNAME_PASSWORD,
  @JsonProperty("ApiClientToken") API_CLIENT_TOKEN;
}

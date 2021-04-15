package io.harness.delegate.beans.connector.appdynamicsconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(HarnessTeam.CV)
public enum AppDynamicsAuthType {
  @JsonProperty("UsernamePassword") USERNAME_PASSWORD,
  @JsonProperty("ApiClientToken") API_CLIENT_TOKEN;
}

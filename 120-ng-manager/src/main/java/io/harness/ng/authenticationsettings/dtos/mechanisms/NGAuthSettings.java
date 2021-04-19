package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.security.authentication.AuthenticationMechanism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "settingsType", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
public abstract class NGAuthSettings {
  @JsonProperty("settingsType") protected AuthenticationMechanism settingsType;

  public NGAuthSettings(AuthenticationMechanism settingsType) {
    this.settingsType = settingsType;
  }

  public abstract AuthenticationMechanism getSettingsType();
}

package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authentication.AuthenticationMechanism;

import software.wings.beans.loginSettings.LoginSettings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("USERNAME_PASSWORD")
@OwnedBy(HarnessTeam.PL)
public class UsernamePasswordSettings extends NGAuthSettings {
  @NotNull @Valid private LoginSettings loginSettings;

  public UsernamePasswordSettings(@JsonProperty("loginSettings") LoginSettings loginSettings) {
    super(AuthenticationMechanism.USER_PASSWORD);
    this.loginSettings = loginSettings;
  }

  @Override
  public AuthenticationMechanism getSettingsType() {
    return AuthenticationMechanism.USER_PASSWORD;
  }
}

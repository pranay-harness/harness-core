package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.loginSettings.PasswordExpirationPolicy;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;
import software.wings.beans.loginSettings.UserLockoutPolicy;
import software.wings.security.authentication.AuthenticationMechanism;

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
  @NotNull @Valid private UserLockoutPolicy userLockoutPolicy;
  @NotNull @Valid private PasswordExpirationPolicy passwordExpirationPolicy;
  @NotNull @Valid private PasswordStrengthPolicy passwordStrengthPolicy;

  public UsernamePasswordSettings(@JsonProperty("userLockoutPolicy") UserLockoutPolicy userLockoutPolicy,
      @JsonProperty("passwordExpirationPolicy") PasswordExpirationPolicy passwordExpirationPolicy,
      @JsonProperty("passwordStrengthPolicy") PasswordStrengthPolicy passwordStrengthPolicy) {
    super(AuthenticationMechanism.USER_PASSWORD);
    this.userLockoutPolicy = userLockoutPolicy;
    this.passwordExpirationPolicy = passwordExpirationPolicy;
    this.passwordStrengthPolicy = passwordStrengthPolicy;
  }

  @Override
  public AuthenticationMechanism getSettingsType() {
    return AuthenticationMechanism.USER_PASSWORD;
  }
}

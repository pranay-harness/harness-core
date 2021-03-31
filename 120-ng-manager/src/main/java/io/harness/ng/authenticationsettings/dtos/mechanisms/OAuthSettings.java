package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.OauthProviderType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("OAUTH")
@OwnedBy(HarnessTeam.PL)
public class OAuthSettings extends NGAuthSettings {
  private String filter;
  private Set<OauthProviderType> allowedProviders;

  @Override
  public AuthenticationMechanism getSettingsType() {
    return AuthenticationMechanism.OAUTH;
  }
}

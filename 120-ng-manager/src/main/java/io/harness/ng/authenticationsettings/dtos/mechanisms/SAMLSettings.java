package io.harness.ng.authenticationsettings.dtos.mechanisms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.security.authentication.AuthenticationMechanism;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("SAML")
@OwnedBy(HarnessTeam.PL)
public class SAMLSettings extends NGAuthSettings {
  @NotNull private String origin;
  private String logoutUrl;
  private String groupMembershipAttr;

  @Override
  public AuthenticationMechanism getSettingsType() {
    return AuthenticationMechanism.SAML;
  }
}

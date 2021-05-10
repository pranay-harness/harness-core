package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.security.authentication.AuthenticationMechanism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
  private String displayName;
  private Boolean authorizationEnabled;

  public SAMLSettings(@JsonProperty("origin") String origin, @JsonProperty("logoutUrl") String logoutUrl,
      @JsonProperty("groupMembershipAttr") String groupMembershipAttr, @JsonProperty("displayName") String displayName,
      @JsonProperty("authorizationEnabled") Boolean authorizationEnabled) {
    super(AuthenticationMechanism.SAML);
    this.displayName = displayName;
    this.origin = origin;
    this.logoutUrl = logoutUrl;
    this.groupMembershipAttr = groupMembershipAttr;
    this.authorizationEnabled = authorizationEnabled;
  }

  @Override
  public AuthenticationMechanism getSettingsType() {
    return AuthenticationMechanism.SAML;
  }
}

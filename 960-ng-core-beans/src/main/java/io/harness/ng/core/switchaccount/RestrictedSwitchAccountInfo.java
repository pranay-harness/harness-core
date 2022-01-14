package io.harness.ng.core.switchaccount;

import io.harness.ng.core.account.AuthenticationMechanism;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
@AllArgsConstructor
public class RestrictedSwitchAccountInfo {
  boolean skipReAuthentication;
  boolean isHarnessSupportGroupUser;
  boolean isTwoFactorAuthEnabledForAccount;
  AuthenticationMechanism authenticationMechanism;
  LdapIdentificationInfo ldapIdentificationInfo;
  OauthIdentificationInfo oauthIdentificationInfo;
  SamlIdentificationInfo samlIdentificationInfo;
  Set<String> whitelistedDomains;
}

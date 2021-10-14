package io.harness.ng.core.account;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum AuthenticationMechanism {
  USER_PASSWORD("NON_SSO"),
  SAML("SSO"),
  LDAP("SSO"),
  OAUTH("SSO");
  private String type;

  AuthenticationMechanism(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}

package io.harness.ng.core.account;

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

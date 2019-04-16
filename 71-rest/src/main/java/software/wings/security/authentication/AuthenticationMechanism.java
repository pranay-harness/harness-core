package software.wings.security.authentication;

import com.google.common.collect.ImmutableList;

import java.util.List;

public enum AuthenticationMechanism {
  USER_PASSWORD("NON_SSO"),
  SAML("SSO"),
  LDAP("SSO"),
  OAUTH("SSO");
  private String type;

  AuthenticationMechanism(String type) {
    this.type = type;
  }

  public static final List<AuthenticationMechanism> DISABLED_FOR_COMMUNITY = ImmutableList.of(SAML, LDAP);
  public String getType() {
    return type;
  }
}

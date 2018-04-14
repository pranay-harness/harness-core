package software.wings.security.authentication;

public enum AuthenticationMechanism {
  USER_PASSWORD("NON_SSO"),
  SAML("SSO");
  private String type;

  AuthenticationMechanism(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}

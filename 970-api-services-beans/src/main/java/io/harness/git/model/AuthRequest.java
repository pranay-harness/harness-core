package io.harness.git.model;

public class AuthRequest implements AuthInfo {
  protected AuthType authType;

  public AuthRequest(AuthType authType) {
    this.authType = authType;
  }

  @Override
  public AuthType getAuthType() {
    return authType;
  }
}

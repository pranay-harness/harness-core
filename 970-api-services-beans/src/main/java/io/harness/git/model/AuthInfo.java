package io.harness.git.model;

public interface AuthInfo {
  AuthType getAuthType();

  enum AuthType { SSH_KEY, HTTP_PASSWORD }
}

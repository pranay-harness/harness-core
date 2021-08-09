package io.harness.git;

import io.harness.git.model.AuthRequest;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UsernamePasswordAuthRequest extends AuthRequest {
  private String username;
  private char[] password;

  public UsernamePasswordAuthRequest(String username, char[] password) {
    super(AuthType.HTTP_PASSWORD);
    this.username = username;
    this.password = password;
  }
}

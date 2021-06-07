package io.harness.argo.beans;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class UsernamePassword {
  private String username;
  private String password;
}

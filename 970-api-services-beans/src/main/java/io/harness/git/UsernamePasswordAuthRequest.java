/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

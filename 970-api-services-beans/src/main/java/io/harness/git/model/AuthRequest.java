/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

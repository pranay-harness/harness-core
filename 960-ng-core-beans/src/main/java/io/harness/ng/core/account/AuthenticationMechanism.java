/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

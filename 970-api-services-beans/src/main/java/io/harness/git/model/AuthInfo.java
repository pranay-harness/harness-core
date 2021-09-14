/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.git.model;

public interface AuthInfo {
  AuthType getAuthType();

  enum AuthType { SSH_KEY, HTTP_PASSWORD }
}

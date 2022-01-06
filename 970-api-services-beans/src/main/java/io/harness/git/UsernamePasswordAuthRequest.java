/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

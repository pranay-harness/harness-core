/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.git.model;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.jgit.transport.SshSessionFactory;

@Getter
@Builder
public class JgitSshAuthRequest extends AuthRequest {
  private SshSessionFactory factory;

  public JgitSshAuthRequest(SshSessionFactory factory) {
    super(AuthType.SSH_KEY);
    this.factory = factory;
  }
}

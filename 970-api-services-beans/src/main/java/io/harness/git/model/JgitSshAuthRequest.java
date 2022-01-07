/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

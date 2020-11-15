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

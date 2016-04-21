package software.wings.core.ssh.executors;

import com.jcraft.jsch.Session;

/**
 * Created by anubhaw on 2/5/16.
 */
public class SshJumpboxExecutor extends AbstractSshExecutor {
  @Override
  public Session getSession(SshSessionConfig config) {
    return SSHSessionFactory.getSSHSessionWithJumpbox(config);
  }
}

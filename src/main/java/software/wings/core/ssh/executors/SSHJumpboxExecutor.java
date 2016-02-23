package software.wings.core.ssh.executors;

import com.jcraft.jsch.Session;

/**
 * Created by anubhaw on 2/5/16.
 */

public class SSHJumpboxExecutor extends AbstractSSHExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) {
    return SSHSessionFactory.getSSHSessionWithJumpbox(config);
  }
}

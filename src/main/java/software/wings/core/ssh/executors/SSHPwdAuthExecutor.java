package software.wings.core.ssh.executors;

import com.jcraft.jsch.Session;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SSHPwdAuthExecutor extends AbstractSSHExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) {
    return SSHSessionFactory.getSSHSession(config, "PASSWORD");
  }
}

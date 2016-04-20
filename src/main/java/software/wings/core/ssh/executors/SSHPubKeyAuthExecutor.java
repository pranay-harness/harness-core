package software.wings.core.ssh.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SSHPubKeyAuthExecutor extends AbstractSSHExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) throws JSchException {
    return SSHSessionFactory.getSSHSession(config);
  }
}

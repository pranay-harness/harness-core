package software.wings.core.executors;

import com.jcraft.jsch.Session;

/**
 * Created by anubhaw on 2/8/16.
 */

public class SSHPubKeyAuthExecutor extends AbstractSSHExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) {
    return SSHSessionFactory.getSSHSessionWithKey(config);
  }
}

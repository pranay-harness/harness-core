package software.wings.core.ssh.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshPwdAuthExecutor extends AbstractSshExecutor {
  @Override
  public Session getSession(SshSessionConfig config) throws JSchException {
    return SshSessionFactory.getSshSession(config);
  }
}

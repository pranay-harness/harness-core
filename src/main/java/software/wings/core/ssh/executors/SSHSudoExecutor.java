package software.wings.core.ssh.executors;

import com.jcraft.jsch.Session;

import java.io.IOException;

/**
 * Created by anubhaw on 2/4/16.
 */

public class SSHSudoExecutor extends AbstractSSHExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) {
    return SSHSessionFactory.getSSHSession(config);
  }

  @Override
  public void postChannelConnect() {
    super.postChannelConnect();
    try {
      inputStream.write((config.getSudoUserPassword() + "\n").getBytes());
      inputStream.flush();
    } catch (IOException e) {
      LOGGER.error("PostChannelConnect failed " + e.getStackTrace());
    }
  }
}

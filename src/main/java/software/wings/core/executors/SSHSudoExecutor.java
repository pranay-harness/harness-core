package software.wings.core.executors;

import com.jcraft.jsch.Session;

import java.io.IOException;

/**
 * Created by anubhaw on 2/4/16.
 */

public class SSHSudoExecutor extends AbstractExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) {
    return SSHSessionFactory.getSSHSessionWithPwd(config);
  }

  @Override
  public void postChannelConnect() {
    super.postChannelConnect();
    try {
      inputStream.write((config.getPassword() + "\n").getBytes());
      inputStream.flush();
    } catch (IOException e) {
      LOGGER.error("PostChannelConnect failed " + e.getStackTrace());
    }
  }
}

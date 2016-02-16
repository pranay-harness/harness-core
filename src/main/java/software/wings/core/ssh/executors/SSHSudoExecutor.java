package software.wings.core.ssh.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/4/16.
 */

public class SSHSudoExecutor extends AbstractSSHExecutor {
  public static String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";

  @Override
  public Session getSession(SSHSessionConfig config) throws JSchException {
    return SSHSessionFactory.getSSHSession(config);
  }

  @Override
  public void postChannelConnect() {
    super.postChannelConnect();
    String outputStreamContent = null;
    try {
      int sudoPromptTimeout = 5;
      while (sudoPromptTimeout > 0) {
        outputStreamContent = new String(((ByteArrayOutputStream) outputStream).toByteArray(), "UTF-8");
        if (outputStreamContent.length() > 0) {
          break;
        }
        quietSleep(1000);
        sudoPromptTimeout--;
      }
      if (outputStreamContent.matches(DEFAULT_SUDO_PROMPT_PATTERN)) {
        inputStream.write((config.getSudoUserPassword() + "\n").getBytes());
        inputStream.flush();
      }
    } catch (IOException e) {
      LOGGER.error("PostChannelConnect failed " + e.getStackTrace());
    }
  }
}

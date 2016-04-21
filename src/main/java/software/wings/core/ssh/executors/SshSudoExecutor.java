package software.wings.core.ssh.executors;

import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_CODE;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_MEG;
import static software.wings.utils.Misc.quietSleep;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import software.wings.exception.WingsException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by anubhaw on 2/4/16.
 */
public class SshSudoExecutor extends AbstractSshExecutor {
  @Override
  public Session getSession(SshSessionConfig config) throws JSchException {
    return SSHSessionFactory.getSSHSession(config);
  }

  @Override
  public void postChannelConnect() {
    super.postChannelConnect();
    String inputStream = null;
    try {
      int sudoPromptTimeout = 5;
      while (sudoPromptTimeout > 0) {
        inputStream = new String(((ByteArrayOutputStream) outputStream).toByteArray(), "UTF-8");
        if (inputStream.length() > 0) {
          break;
        }
        quietSleep(1000);
        sudoPromptTimeout--;
      }
      if (inputStream.matches(DEFAULT_SUDO_PROMPT_PATTERN)) {
        outputStream.write((config.getSudoUserPassword() + "\n").getBytes());
        outputStream.flush();
      }
    } catch (IOException e) {
      logger.error("Reading writing to output/input stream failed");
      throw new WingsException(UNKNOWN_ERROR_MEG, UNKNOWN_ERROR_CODE, e.getCause());
    }
  }
}

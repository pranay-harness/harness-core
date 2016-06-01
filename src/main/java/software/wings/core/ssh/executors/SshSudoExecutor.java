package software.wings.core.ssh.executors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR;
import static software.wings.utils.Misc.quietSleep;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Created by anubhaw on 2/4/16.
 */
public class SshSudoExecutor extends AbstractSshExecutor {
  @Inject
  public SshSudoExecutor(ExecutionLogs executionLogs, FileService fileService) {
    super(executionLogs, fileService);
  }

  @Override
  public Session getSession(SshSessionConfig config) throws JSchException {
    return SshSessionFactory.getSSHSession(config);
  }

  @Override
  public void postChannelConnect() {
    super.postChannelConnect();
    String inputStream = null;
    try {
      int sudoPromptTimeout = 5; // TODO: read from config
      while (sudoPromptTimeout > 0) {
        inputStream = new String(((ByteArrayOutputStream) outputStream).toByteArray(), UTF_8);
        if (inputStream.length() > 0) {
          break;
        }
        quietSleep(1000);
        sudoPromptTimeout--;
      }
      if (inputStream.matches(DEFAULT_SUDO_PROMPT_PATTERN)) {
        outputStream.write((config.getSudoAppPassword() + "\n").getBytes(UTF_8));
        outputStream.flush();
      }
    } catch (IOException ex) {
      logger.error("Reading writing to output/input stream failed", ex);
      throw new WingsException(UNKNOWN_ERROR, ex.getCause());
    }
  }
}

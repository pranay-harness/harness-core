package software.wings.core.ssh.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.LogService;

import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 2/8/16.
 */
@ValidateOnExecution
public class SshPwdAuthExecutor extends AbstractSshExecutor {
  /**
   * Instantiates a new ssh pwd auth executor.
   *
   * @param fileService the file service
   * @param logService  the log service
   */
  @Inject
  public SshPwdAuthExecutor(FileService fileService, LogService logService) {
    super(fileService, logService);
  }

  /* (non-Javadoc)
   * @see
   * software.wings.core.ssh.executors.AbstractSshExecutor#getSession(software.wings.core.ssh.executors.SshSessionConfig)
   */
  @Override
  public Session getSession(SshSessionConfig config) {
    try {
      return SshSessionFactory.getSSHSession(config);
    } catch (JSchException jschEx) {
      throw new WingsException(normalizeError(jschEx), normalizeError(jschEx).getCode(), jschEx);
    }
  }
}

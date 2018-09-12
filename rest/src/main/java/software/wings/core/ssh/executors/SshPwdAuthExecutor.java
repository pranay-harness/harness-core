package software.wings.core.ssh.executors;

import static software.wings.utils.SshHelperUtil.normalizeError;

import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.harness.exception.WingsException;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

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
  public SshPwdAuthExecutor(DelegateFileManager fileService, DelegateLogService logService) {
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
      throw new WingsException(normalizeError(jschEx), normalizeError(jschEx).name(), jschEx);
    }
  }
}

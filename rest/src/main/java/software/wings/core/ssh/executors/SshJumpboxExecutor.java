package software.wings.core.ssh.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;

import javax.inject.Inject;

/**
 * Created by anubhaw on 2/5/16.
 */
public class SshJumpboxExecutor extends AbstractSshExecutor {
  /**
   * Instantiates a new ssh jumpbox executor.
   *
   * @param fileService the file service
   * @param logService  the log service
   */
  @Inject
  public SshJumpboxExecutor(DelegateFileManager fileService, DelegateLogService logService) {
    super(fileService, logService);
  }

  /* (non-Javadoc)
   * @see
   * software.wings.core.ssh.executors.AbstractSshExecutor#getSession(software.wings.core.ssh.executors.SshSessionConfig)
   */
  @Override
  public Session getSession(SshSessionConfig config) {
    try {
      return SshSessionFactory.getSSHSessionWithJumpbox(config);
    } catch (JSchException jschEx) {
      throw new WingsException(normalizeError(jschEx), normalizeError(jschEx).getCode(), jschEx);
    }
  }
}

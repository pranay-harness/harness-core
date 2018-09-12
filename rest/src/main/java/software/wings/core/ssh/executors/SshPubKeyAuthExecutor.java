package software.wings.core.ssh.executors;

import static software.wings.utils.SshHelperUtil.normalizeError;

import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.harness.exception.WingsException;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshPubKeyAuthExecutor extends AbstractSshExecutor {
  /**
   * Instantiates a new ssh pub key auth executor.
   *
   * @param fileService the file service
   * @param logService  the log service
   */
  @Inject
  public SshPubKeyAuthExecutor(DelegateFileManager fileService, DelegateLogService logService) {
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

package software.wings.core.ssh.executors;

import com.jcraft.jsch.Session;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 2/5/16.
 */
public class SshJumpboxExecutor extends AbstractSshExecutor {
  /**
   * Instantiates a new ssh jumpbox executor.
   *
   * @param executionLogs the execution logs
   * @param fileService   the file service
   */
  @Inject
  public SshJumpboxExecutor(ExecutionLogs executionLogs, FileService fileService) {
    super(executionLogs, fileService);
  }

  /* (non-Javadoc)
   * @see
   * software.wings.core.ssh.executors.AbstractSshExecutor#getSession(software.wings.core.ssh.executors.SshSessionConfig)
   */
  @Override
  public Session getSession(SshSessionConfig config) {
    return SshSessionFactory.getSSHSessionWithJumpbox(config);
  }
}

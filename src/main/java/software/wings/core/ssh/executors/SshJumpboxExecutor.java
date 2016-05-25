package software.wings.core.ssh.executors;

import com.jcraft.jsch.Session;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 2/5/16.
 */
public class SshJumpboxExecutor extends AbstractSshExecutor {
  @Inject
  public SshJumpboxExecutor(ExecutionLogs executionLogs, FileService fileService) {
    super(executionLogs, fileService);
  }

  @Override
  public Session getSession(SshSessionConfig config) {
    return SshSessionFactory.getSSHSessionWithJumpbox(config);
  }
}

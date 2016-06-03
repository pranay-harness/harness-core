package software.wings.core.ssh.executors;

import com.jcraft.jsch.Session;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.LogService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 2/5/16.
 */
public class SshJumpboxExecutor extends AbstractSshExecutor {
  @Inject
  public SshJumpboxExecutor(FileService fileService, LogService logService) {
    super(fileService, logService);
  }

  @Override
  public Session getSession(SshSessionConfig config) {
    return SshSessionFactory.getSSHSessionWithJumpbox(config);
  }
}

package software.wings.core.ssh.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshPwdAuthExecutor extends AbstractSshExecutor {
  @Inject
  public SshPwdAuthExecutor(ExecutionLogs executionLogs, FileService fileService) {
    super(executionLogs, fileService);
  }

  @Override
  public Session getSession(SshSessionConfig config) throws JSchException {
    return SshSessionFactory.getSSHSession(config);
  }
}

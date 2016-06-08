package software.wings.core.ssh.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.LogService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

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
  public SshPubKeyAuthExecutor(FileService fileService, LogService logService) {
    super(fileService, logService);
  }

  /* (non-Javadoc)
   * @see
   * software.wings.core.ssh.executors.AbstractSshExecutor#getSession(software.wings.core.ssh.executors.SshSessionConfig)
   */
  @Override
  public Session getSession(SshSessionConfig config) throws JSchException {
    return SshSessionFactory.getSSHSession(config);
  }
}

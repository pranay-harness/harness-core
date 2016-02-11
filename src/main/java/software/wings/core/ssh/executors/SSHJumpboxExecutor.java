package software.wings.core.ssh.executors;

import com.jcraft.jsch.*;
import org.slf4j.LoggerFactory;
import software.wings.core.ssh.executors.callbacks.SSHCommandExecutionCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/5/16.
 */

public class SSHJumpboxExecutor extends AbstractSSHExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) {
    return SSHSessionFactory.getSSHSessionWithJumpbox(config);
  }
}

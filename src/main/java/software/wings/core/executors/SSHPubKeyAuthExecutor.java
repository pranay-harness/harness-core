package software.wings.core.executors;

import com.jcraft.jsch.*;
import org.slf4j.LoggerFactory;
import software.wings.core.executors.callbacks.SSHCommandExecutionCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/8/16.
 */

public class SSHPubKeyAuthExecutor extends AbstractExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) {
    return SSHSessionFactory.getSSHSessionWithKey(config);
  }

  @Override
  public void preInit() {}

  @Override
  public void postInit() {}

  @Override
  public void preExecute() {}

  @Override
  public void postExecute() {}
}

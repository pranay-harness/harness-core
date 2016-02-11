package software.wings.core.ssh.executors.callbacks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Execution;

/**
 * Created by anubhaw on 2/3/16.
 */

public class ConsoleExecutionCallback implements SSHCommandExecutionCallback {
  private Execution execution;

  public ConsoleExecutionCallback(Execution execution) {
    this.execution = execution;
  }

  @Override
  public void log(String message) {
    LOGGER.info(message);
  }

  @Override
  public void updateStatus() {
    LOGGER.info("updateStatus is called");
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleExecutionCallback.class);
}

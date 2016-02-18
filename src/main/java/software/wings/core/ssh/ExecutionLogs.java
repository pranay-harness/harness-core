package software.wings.core.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by anubhaw on 2/17/16.
 */

public class ExecutionLogs {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionLogs.class);

  public void appendLogs(String executionID, String logs) {
    LOGGER.info("Saving log for execution ID: " + executionID);
    LOGGER.info(logs);
  }

  // Singleton
  private static volatile ExecutionLogs executionLogs;
  private ExecutionLogs() {}
  public static ExecutionLogs getInstance() {
    if (executionLogs == null) {
      synchronized (ExecutionLogs.class) {
        if (executionLogs == null) {
          executionLogs = new ExecutionLogs();
        }
      }
    }
    return executionLogs;
  }
}

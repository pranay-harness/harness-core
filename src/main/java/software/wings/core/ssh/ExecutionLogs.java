package software.wings.core.ssh;

import com.mongodb.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.GridFSDBFileExt;

/**
 * Created by anubhaw on 2/17/16.
 */

public class ExecutionLogs {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionLogs.class);
  private static final GridFSDBFileExt gridFSDBFIleExt =
      new GridFSDBFileExt(new MongoClient("localhost").getDatabase("wings"), "logs", 6); // TODO: Read from config

  public void appendLogs(String executionID, String logs) {
    LOGGER.info("Saving log for execution ID: " + executionID);
    gridFSDBFIleExt.appendToFile(executionID, logs);
    LOGGER.info("Saved following log text in GridFS: " + logs);
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

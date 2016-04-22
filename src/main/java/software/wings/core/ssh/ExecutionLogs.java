package software.wings.core.ssh;

import com.mongodb.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.GridFsDbFileExt;

/**
 * Created by anubhaw on 2/17/16.
 */
public class ExecutionLogs {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private static final GridFsDbFileExt gridFSDBFIleExt =
      new GridFsDbFileExt(new MongoClient("localhost").getDatabase("wings"), "logs", 6);
  // TODO: Read from config
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

  public void appendLogs(String executionId, String logs) {
    logger.info("Saving log for execution ID: " + executionId);
    gridFSDBFIleExt.appendToFile(executionId, logs);
    logger.info("Saved following log text in GridFS: " + logs);
  }
}

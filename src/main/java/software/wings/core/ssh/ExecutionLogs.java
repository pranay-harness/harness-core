package software.wings.core.ssh;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.GridFSDBFileExt;

import javax.inject.Inject;

/**
 * Created by anubhaw on 2/17/16.
 */
@Singleton
public class ExecutionLogs {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionLogs.class);
  @Inject private GridFSDBFileExt gridFSDBFileExt;

  public void appendLogs(String executionID, String logs) {
    LOGGER.info("Saving log for execution ID: " + executionID);
    gridFSDBFileExt.appendToFile(executionID, logs);
    LOGGER.info("Saved following log text in GridFS: " + logs);
  }
}

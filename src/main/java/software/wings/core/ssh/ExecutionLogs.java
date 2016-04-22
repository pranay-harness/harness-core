package software.wings.core.ssh;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.GridFsDbFileExt;

import javax.inject.Inject;

/**
 * Created by anubhaw on 2/17/16.
 */
@Singleton
public class ExecutionLogs {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private GridFsDbFileExt gridFSDBFileExt;

  public void appendLogs(String executionID, String logs) {
    logger.info("Saving log for execution ID: " + executionID);
    gridFSDBFileExt.appendToFile(executionID, logs);
    logger.info("Saved following log text in GridFS: " + logs);
  }
}

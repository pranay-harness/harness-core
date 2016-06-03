package software.wings.service.intfc;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/19/16.
 */
public interface ExecutionLogs {
  /**
   * Append logs.
   *
   * @param executionId the execution id
   * @param logs        the logs
   */
  void appendLogs(String executionId, String logs);
}

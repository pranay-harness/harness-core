package io.harness.delegate.beans.logstreaming;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.taskprogress.ITaskProgressClient;
import io.harness.logging.LogCallback;
import io.harness.logstreaming.LogLine;

import java.util.concurrent.ExecutorService;

@OwnedBy(HarnessTeam.CDP)
public interface ILogStreamingTaskClient {
  /**
   * Open new log stream on log streaming service.
   */
  void openStream(String baseLogKeySuffix);

  /**
   * Close existing log stream on log streaming service
   */
  void closeStream(String baseLogKeySuffix);

  /**
   * Push log message to the existing log stream
   */
  void writeLogLine(LogLine logLine, String baseLogKeySuffix);

  /**
   * Provides an instance of LogCallback interface implementation for backward compatibility reasons, to be used in
   * delegate task implementations, instead of direct constructor invocation. LogService and accountId are provided by
   * the delegate, while appId and activityId should be available as part of task parameters. Please make sure that your
   * task parameters class implements {@link io.harness.delegate.task.Cd1ApplicationAccess} and {@link
   * io.harness.delegate.task.ActivityAccess} interfaces.
   */
  LogCallback obtainLogCallback(String commandName);

  /**
   *
   * Provides an instance of ITaskProgressClient interface implementation that should be used to send task progress
   * updates.
   *
   * @return instance of taskProgressClient or null if the task was submitted to the delegate service without the
   *     delegateCallbackToken
   */
  ITaskProgressClient obtainTaskProgressClient();

  ExecutorService obtainTaskProgressExecutor();
}

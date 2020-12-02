package io.harness.logstreaming;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.LogLine;
import io.harness.delegate.beans.logstreaming.LogStreamingSanitizer;
import io.harness.delegate.beans.taskprogress.ITaskProgressClient;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.network.SafeHttpCall;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.stream.LogOutputStream;

@Builder
@Slf4j
public class LogStreamingTaskClient implements ILogStreamingTaskClient {
  public static final String COMMAND_UNIT_PLACEHOLDER = "-commandUnit:%s";

  private final DelegateLogService logService;
  private final DelegateAgentLogStreamingClient delegateAgentLogStreamingClient;
  private final LogStreamingSanitizer logStreamingSanitizer;
  private final String token;
  private final String accountId;
  private final String baseLogKey;
  @Deprecated private final String appId;
  @Deprecated private final String activityId;

  private final ITaskProgressClient taskProgressClient;

  @Override
  public void openStream(String baseLogKeySuffix) {
    String logKey =
        baseLogKey + (isBlank(baseLogKeySuffix) ? "" : String.format(COMMAND_UNIT_PLACEHOLDER, baseLogKeySuffix));

    try {
      SafeHttpCall.executeWithExceptions(delegateAgentLogStreamingClient.openLogStream(token, accountId, logKey));
    } catch (Exception ex) {
      log.error("Unable to open log stream for account {} and key {}", accountId, logKey, ex);
    }
  }

  @Override
  public void closeStream(String baseLogKeySuffix) {
    String logKey =
        baseLogKey + (isBlank(baseLogKeySuffix) ? "" : String.format(COMMAND_UNIT_PLACEHOLDER, baseLogKeySuffix));

    try {
      SafeHttpCall.executeWithExceptions(
          delegateAgentLogStreamingClient.closeLogStream(token, accountId, logKey, true));
    } catch (Exception ex) {
      log.error("Unable to close log stream for account {} and key {}", accountId, logKey, ex);
    }
  }

  @Override
  public void writeLogLine(LogLine logLine, String baseLogKeySuffix) {
    if (logLine == null) {
      throw new InvalidArgumentsException("Log line parameter is mandatory.");
    }

    String logKey =
        baseLogKey + (isBlank(baseLogKeySuffix) ? "" : String.format(COMMAND_UNIT_PLACEHOLDER, baseLogKeySuffix));

    logStreamingSanitizer.sanitizeLogMessage(logLine);

    try {
      SafeHttpCall.executeWithExceptions(
          delegateAgentLogStreamingClient.pushMessage(token, accountId, logKey, Arrays.asList(logLine)));
    } catch (Exception ex) {
      log.error("Unable to push message to log stream for account {} and key {}", accountId, logKey, ex);
    }
  }

  @Override
  public OutputStream obtainLogOutputStream(LogLevel logLevel, String baseLogKeySuffix) {
    return new LogOutputStream() {
      private LogLevel level = logLevel == null ? LogLevel.INFO : logLevel;
      private String logKeySuffix = baseLogKeySuffix;

      @Override
      protected void processLine(String line) {
        writeLogLine(LogLine.builder().level(level).message(line).timestamp(OffsetDateTime.now().toInstant()).build(),
            logKeySuffix);
      }
    };
  }

  @Override
  public LogCallback obtainLogCallback(String commandName) {
    if (isBlank(appId) || isBlank(activityId)) {
      throw new InvalidArgumentsException(
          "Application id and activity id were not available as part of task params. Please make sure that task params class implements Cd1ApplicationAccess and ActivityAccess interfaces.");
    }

    return new ExecutionLogCallback(logService, accountId, appId, activityId, commandName);
  }

  @Override
  public ITaskProgressClient obtainTaskProgressClient() {
    return taskProgressClient;
  }
}

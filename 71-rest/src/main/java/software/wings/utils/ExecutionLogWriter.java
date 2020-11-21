package software.wings.utils;

import static io.harness.logging.CommandExecutionStatus.RUNNING;

import static software.wings.beans.Log.Builder.aLog;

import io.harness.logging.LogLevel;

import software.wings.delegatetasks.DelegateLogService;

import java.io.Writer;
import lombok.Builder;

@Builder
public class ExecutionLogWriter extends Writer {
  private final DelegateLogService logService;
  @SuppressWarnings("PMD.AvoidStringBufferField") // This buffer is getting cleared on every newline.
  private final StringBuilder stringBuilder;
  private final LogLevel logLevel;
  private final String accountId;
  private final String appId;
  private final String executionId;
  private final String hostName;
  private final String commandUnitName;

  @Override
  public void write(char[] cbuf, int off, int len) {
    stringBuilder.append(cbuf, off, len);
    char lastChar = cbuf[off + len - 1];
    if (lastChar == '\n') {
      logAndFlush();
    }
  }

  @Override
  public void flush() {
    logAndFlush();
  }

  @Override
  public void close() {
    logAndFlush();
  }

  private void logAndFlush() {
    String logLine = stringBuilder.toString();
    if (!logLine.isEmpty()) {
      logService.save(accountId,
          aLog()
              .appId(appId)
              .activityId(executionId)
              .logLevel(logLevel)
              .commandUnitName(commandUnitName)
              .hostName(hostName)
              .logLine(logLine.trim())
              .executionResult(RUNNING)
              .build());
      stringBuilder.setLength(0);
    }
  }
}

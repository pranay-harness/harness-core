package software.wings.utils;

import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;

import lombok.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.delegatetasks.DelegateLogService;

import java.io.IOException;
import java.io.Writer;

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
  private boolean writeCalled;

  public boolean didWriteGetCalled() {
    return writeCalled;
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    writeCalled = true;
    stringBuilder.append(cbuf, off, len);
    char lastChar = cbuf[off + len - 1];
    if (lastChar == '\n') {
      logAndFlush();
    }
  }

  @Override
  public void flush() throws IOException {
    logAndFlush();
  }

  @Override
  public void close() throws IOException {}

  private void logAndFlush() {
    String logLine = stringBuilder.toString();
    if (!logLine.isEmpty()) {
      logService.save(accountId,
          aLog()
              .withAppId(appId)
              .withActivityId(executionId)
              .withLogLevel(logLevel)
              .withCommandUnitName(commandUnitName)
              .withHostName(hostName)
              .withLogLine(stringBuilder.toString().trim())
              .withExecutionResult(RUNNING)
              .build());
      stringBuilder.setLength(0);
    }
  }
}
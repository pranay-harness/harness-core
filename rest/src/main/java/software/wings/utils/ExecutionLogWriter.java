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
  private final StringBuilder stringBuilder;
  private final LogLevel logLevel;
  private final String accountId;
  private final String appId;
  private final String executionId;
  private final String hostName;
  private final String commandUnitName;

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    stringBuilder.append(cbuf, off, len);
    char lastChar = cbuf[off + len - 1];
    if (lastChar == '\n') {
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

  @Override
  public void flush() throws IOException {}

  @Override
  public void close() throws IOException {}
}
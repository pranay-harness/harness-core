package io.harness.delegate.beans.logstreaming;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.LogLevel;
import io.harness.network.SafeHttpCall;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;

@Builder
@Slf4j
public class LogStreamingTaskClient implements ILogStreamingTaskClient {
  private static final String COMMAND_UNIT_PLACEHOLDER = "-commandUnit:%s";

  private final DelegateAgentLogStreamingClient delegateAgentLogStreamingClient;
  private final LogStreamingSanitizer logStreamingSanitizer;
  private final String token;
  private final String accountId;
  private final String baseLogKey;

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
      SafeHttpCall.executeWithExceptions(delegateAgentLogStreamingClient.closeLogStream(token, accountId, logKey));
    } catch (Exception ex) {
      log.error("Unable to close log stream for account {} and key {}", accountId, logKey, ex);
    }
  }

  @Override
  public void writeLogLine(LogLine logLine, String baseLogKeySuffix) {
    if (logLine == null) {
      throw new InvalidArgumentsException("All parameters are mandatory.");
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
      private LogLevel level = (logLevel == null ? LogLevel.INFO : logLevel);
      private String logKeySuffix = baseLogKeySuffix;

      @Override
      protected void processLine(String line) {
        writeLogLine(LogLine.builder().level(level).message(line).timestamp(OffsetDateTime.now().toInstant()).build(),
            logKeySuffix);
      }
    };
  }
}

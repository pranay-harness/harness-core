package io.harness.logStreaming;

import static software.wings.beans.LogHelper.COMMAND_UNIT_PLACEHOLDER;

import io.harness.exception.InvalidRequestException;
import io.harness.logstreaming.LogLine;
import io.harness.logstreaming.LogStreamingClient;
import io.harness.network.SafeHttpCall;

import java.util.Collections;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Builder
@Data
@Slf4j
public class LogStreamingStepClientImpl implements ILogStreamingStepClient {
  private final LogStreamingClient logStreamingClient;
  private final String token;
  private final String accountId;
  private final String baseLogKey;

  @Override
  public void openStream(String logKeySuffix) {
    try {
      String logKey = baseLogKey + String.format(COMMAND_UNIT_PLACEHOLDER, logKeySuffix);
      SafeHttpCall.executeWithExceptions(logStreamingClient.openLogStream(token, accountId, logKey));
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage() + "\nPlease ensure log service is running.", ex);
    }
  }

  @Override
  public void closeStream(String logKeySuffix) {
    try {
      String logKey = baseLogKey + String.format(COMMAND_UNIT_PLACEHOLDER, logKeySuffix);
      SafeHttpCall.executeWithExceptions(logStreamingClient.closeLogStream(token, accountId, logKey, true));
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage() + "\nPlease ensure log service is running.", ex);
    }
  }

  @Override
  public void writeLogLine(LogLine logLine, String logKeySuffix) {
    try {
      String logKey = baseLogKey + String.format(COMMAND_UNIT_PLACEHOLDER, logKeySuffix);

      SafeHttpCall.executeWithExceptions(
          logStreamingClient.pushMessage(token, accountId, logKey, Collections.singletonList(logLine)));
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage() + "\nPlease ensure log service is running.", ex);
    }
  }
}

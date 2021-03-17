package io.harness.logstreaming;

import static software.wings.beans.LogColor.Red;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogHelper.doneColoring;
import static software.wings.beans.LogWeight.Bold;

import io.harness.logging.LogLevel;

import software.wings.beans.LogHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LogStreamingHelper {
  public void colorLog(LogLine logLine) {
    String message = logLine.getMessage();
    if (logLine.getLevel() == LogLevel.ERROR) {
      message = color(message, Red, Bold);
    } else if (logLine.getLevel() == LogLevel.WARN) {
      message = color(message, Yellow, Bold);
    }
    message = doneColoring(message);
    logLine.setMessage(message);
  }

  @Nonnull
  public String generateLogBaseKey(LinkedHashMap<String, String> logStreamingAbstractions) {
    // Generate base log key that will be used for witing logs to log streaming service
    StringBuilder logBaseKey = new StringBuilder();
    for (Map.Entry<String, String> entry : logStreamingAbstractions.entrySet()) {
      if (logBaseKey.length() != 0) {
        logBaseKey.append('/');
      }
      logBaseKey.append(entry.getKey()).append(':').append(entry.getValue());
    }
    return logBaseKey.toString();
  }

  public String generateLogKeyGivenCommandUnit(String baseLogKey, String commandUnit) {
    return baseLogKey + String.format(LogHelper.COMMAND_UNIT_PLACEHOLDER, commandUnit);
  }
}

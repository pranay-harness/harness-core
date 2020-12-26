package io.harness.beans.serializer;

import static java.lang.String.format;

import io.harness.exception.ngexception.CIStageExecutionException;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

@UtilityClass
public class TimeoutUtils {
  public static final char TIMEOUT_SUFFIX = 's';

  public long parseTimeoutString(String timeout, long defaultTimeout) {
    try {
      long timeoutLong = Long.parseLong(StringUtils.trimTrailingCharacter(timeout, TIMEOUT_SUFFIX));
      return timeoutLong > 0 ? timeoutLong : defaultTimeout;
    } catch (NumberFormatException e) {
      throw new CIStageExecutionException(format("Timout format is incorrect: %s", timeout), e);
    }
  }
}

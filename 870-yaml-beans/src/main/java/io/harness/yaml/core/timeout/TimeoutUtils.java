package io.harness.yaml.core.timeout;

import io.harness.pms.yaml.ParameterField;

import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeoutUtils {
  public long getTimeoutInSeconds(Timeout timeout, long defaultTimeoutInSeconds) {
    if (timeout == null) {
      return defaultTimeoutInSeconds;
    }
    long timeoutLong = TimeUnit.MILLISECONDS.toSeconds(timeout.getTimeoutInMillis());
    return timeoutLong > 0 ? timeoutLong : defaultTimeoutInSeconds;
  }

  public long getTimeoutInSeconds(ParameterField<Timeout> timeout, long defaultTimeoutInSeconds) {
    if (timeout == null) {
      return defaultTimeoutInSeconds;
    }
    return getTimeoutInSeconds(timeout.getValue(), defaultTimeoutInSeconds);
  }
}

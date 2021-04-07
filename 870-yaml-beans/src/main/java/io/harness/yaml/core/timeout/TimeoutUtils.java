package io.harness.yaml.core.timeout;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class TimeoutUtils {
  private final String DEFAULT_TIMEOUT = "10m";

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

  public String getTimeoutString(ParameterField<Timeout> timeout) {
    String timeoutString = DEFAULT_TIMEOUT;
    if (!ParameterField.isNull(timeout)) {
      if (timeout.isExpression()) {
        timeoutString = timeout.getExpressionValue();
      } else if (timeout.getValue() != null) {
        timeoutString = timeout.getValue().getTimeoutString();
      }
    }
    return timeoutString;
  }

  public ParameterField<Timeout> getTimeout(ParameterField<Timeout> timeout) {
    if (ParameterField.isNull(timeout)) {
      return ParameterField.createValueField(Timeout.fromString(DEFAULT_TIMEOUT));
    }
    return timeout;
  }
}

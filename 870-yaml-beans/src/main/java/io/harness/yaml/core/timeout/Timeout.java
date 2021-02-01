package io.harness.yaml.core.timeout;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.InvalidArgumentsException;
import io.harness.timeout.TimeoutParameters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

@Data
@Builder
public class Timeout {
  private static final String TIMEOUT_STRING = "timeout";

  @Getter
  public enum TimeUnit {
    SECONDS("s", 1000),
    MINUTES("m", 60 * SECONDS.coefficient),
    HOURS("h", 60 * MINUTES.coefficient),
    DAYS("d", 24 * HOURS.coefficient),
    WEEKS("w", 7 * DAYS.coefficient);

    String suffix;
    Integer coefficient; // in relation to milliseconds

    TimeUnit(String unit, Integer coefficient) {
      this.suffix = unit;
      this.coefficient = coefficient;
    }

    public static Optional<TimeUnit> findByUnit(String unit) {
      return Stream.of(TimeUnit.values()).filter(timeUnit -> timeUnit.suffix.equals(unit)).findFirst();
    }
  }

  private String timeoutString;
  private long timeoutInMillis;

  @JsonCreator
  public static Timeout fromString(String timeout) {
    try {
      if (isEmpty(timeout)) {
        return null;
      }
      long currentValue = 0;
      for (char ch : timeout.toCharArray()) {
        String currentSymbol = String.valueOf(ch);
        if (isDigit(currentSymbol)) {
          currentValue = (currentValue * 10) + Long.parseLong(currentSymbol);
        } else if (isUnitCharacter(currentSymbol)) {
          currentValue = currentValue * getMultiplyCoefficient(currentSymbol, timeout);
        } else {
          throw new InvalidArgumentsException(Pair.of(TIMEOUT_STRING, timeout));
        }
      }

      currentValue = currentValue != 0 ? currentValue : TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS;
      return Timeout.builder().timeoutInMillis(currentValue).timeoutString(timeout).build();
    } catch (NumberFormatException e) {
      throw new InvalidArgumentsException(Pair.of(TIMEOUT_STRING, timeout), e);
    }
  }

  private static boolean isDigit(String symbol) {
    return isNotEmpty(symbol) && Character.isDigit(symbol.charAt(0));
  }

  private static boolean isUnitCharacter(String symbol) {
    return TimeUnit.findByUnit(symbol).isPresent();
  }

  private static long getMultiplyCoefficient(String symbol, String timeout) {
    return TimeUnit.findByUnit(symbol)
        .orElseThrow(() -> new InvalidArgumentsException(Pair.of(TIMEOUT_STRING, timeout)))
        .coefficient;
  }

  @JsonValue
  public String getYamlProperty() {
    return timeoutString;
  }
}

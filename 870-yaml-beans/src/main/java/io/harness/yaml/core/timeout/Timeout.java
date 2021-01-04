package io.harness.yaml.core.timeout;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidArgumentsException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

@Data
@Builder
public class Timeout {
  private static final String PARTS_REGEX = "[smhdw]";

  @Getter
  public enum TimeUnit {
    SECONDS("s", 1),
    MINUTES("m", 60),
    HOURS("h", 60 * 60),
    DAYS("d", 24 * 60 * 60),
    WEEKS("w", 7 * 24 * 60 * 60),
    UNITLESS("", 1);

    String suffix;
    Integer coefficient; // in relation to seconds

    TimeUnit(String unit, Integer coefficient) {
      this.suffix = unit;
      this.coefficient = coefficient;
    }
  }

  private TimeUnit unit;
  private Long numericValue;

  @JsonCreator
  public static Timeout fromString(String timeout) {
    try {
      if (isEmpty(timeout)) {
        return null;
      }
      String[] parts = timeout.split("[smhdw]");
      Long numericValue = Long.parseLong(parts[0]);
      String suffix = timeout.substring(parts[0].length());
      TimeUnit unit = Stream.of(TimeUnit.values())
                          .filter(timeUnit -> timeUnit.getSuffix().equals(suffix))
                          .findFirst()
                          .orElseThrow(() -> new InvalidArgumentsException(Pair.of("timeout", timeout)));

      return Timeout.builder().numericValue(numericValue).unit(unit).build();
    } catch (NumberFormatException e) {
      throw new InvalidArgumentsException(Pair.of("timeout", timeout), e);
    }
  }

  @JsonValue
  public String getYamlProperty() {
    return numericValue + unit.getSuffix();
  }
}

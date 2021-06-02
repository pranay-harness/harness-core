package io.harness.yaml.extended.ci.container.quantity;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidArgumentsException;
import io.harness.yaml.extended.ci.container.quantity.unit.MemoryQuantityUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
@Builder
public class MemoryQuantity {
  private static final String PARTS_REGEX = "[GM][i]?";

  private String numericValue;
  private MemoryQuantityUnit unit;

  @JsonCreator
  public static MemoryQuantity fromString(String quantity) {
    try {
      if (isEmpty(quantity)) {
        return null;
      }

      String[] parts = quantity.split(PARTS_REGEX);
      String numericValue = parts[0];
      String suffix = quantity.substring(parts[0].length());
      MemoryQuantityUnit unit = Stream.of(MemoryQuantityUnit.values())
                                    .filter(quantityUnit -> quantityUnit.getSuffix().equals(suffix))
                                    .findFirst()
                                    .orElse(MemoryQuantityUnit.unitless);

      return MemoryQuantity.builder().numericValue(numericValue).unit(unit).build();
    } catch (NumberFormatException e) {
      throw new InvalidArgumentsException(Pair.of("memory", quantity), e);
    }
  }

  @JsonValue
  public String getYamlProperty() {
    return numericValue + unit.getSuffix();
  }
}

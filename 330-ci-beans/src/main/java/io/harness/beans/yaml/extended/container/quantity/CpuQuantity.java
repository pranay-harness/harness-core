package io.harness.beans.yaml.extended.container.quantity;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.yaml.extended.container.quantity.unit.DecimalQuantityUnit;
import io.harness.exception.InvalidArgumentsException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
@Builder
public class CpuQuantity {
  private static final String PARTS_REGEX = "[m]";

  private String numericValue;
  private DecimalQuantityUnit unit;

  @JsonCreator
  public static CpuQuantity fromString(String quantity) {
    try {
      if (isEmpty(quantity)) {
        return null;
      }
      String[] parts = quantity.split(PARTS_REGEX);
      String numericValue = parts[0];
      String suffix = quantity.substring(parts[0].length());
      DecimalQuantityUnit unit = Stream.of(DecimalQuantityUnit.values())
                                     .filter(quantityUnit -> quantityUnit.getSuffix().equals(suffix))
                                     .findFirst()
                                     .orElseThrow(() -> new InvalidArgumentsException(Pair.of("cpu", quantity)));

      return CpuQuantity.builder().numericValue(numericValue).unit(unit).build();
    } catch (NumberFormatException e) {
      throw new InvalidArgumentsException(Pair.of("cpu", quantity), e);
    }
  }

  @JsonValue
  public String getYamlProperty() {
    return numericValue + unit.getSuffix();
  }
}

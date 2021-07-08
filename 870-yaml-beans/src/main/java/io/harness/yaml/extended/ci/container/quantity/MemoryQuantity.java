package io.harness.yaml.extended.ci.container.quantity;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidArgumentsException;
import io.harness.yaml.extended.ci.container.quantity.unit.MemoryQuantityUnit;
import io.harness.yaml.validator.ResourceValidatorConstants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemoryQuantity {
  private String numericValue;
  private MemoryQuantityUnit unit;

  @JsonCreator
  public static MemoryQuantity fromString(String quantity) {
    if (isEmpty(quantity)) {
      return null;
    }

    Pattern r = Pattern.compile(ResourceValidatorConstants.MEMORY_PATTERN);
    Matcher m = r.matcher(quantity);
    if (m.find()) {
      String numericValue = m.group(1);
      String suffix = m.group(3);
      MemoryQuantityUnit unit = Stream.of(MemoryQuantityUnit.values())
                                    .filter(quantityUnit -> quantityUnit.getSuffix().equals(suffix))
                                    .findFirst()
                                    .orElse(MemoryQuantityUnit.unitless);

      return MemoryQuantity.builder().numericValue(numericValue).unit(unit).build();
    } else {
      throw new InvalidArgumentsException(String.format("Invalid memory format: %s. Memory should match regex: %s",
          quantity, ResourceValidatorConstants.MEMORY_PATTERN));
    }
  }

  @JsonValue
  public String getYamlProperty() {
    return numericValue + unit.getSuffix();
  }
}

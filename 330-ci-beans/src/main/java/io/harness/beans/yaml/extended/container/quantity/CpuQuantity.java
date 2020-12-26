package io.harness.beans.yaml.extended.container.quantity;

import io.harness.beans.yaml.extended.container.quantity.unit.DecimalQuantityUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CpuQuantity {
  private static final String PARTS_REGEX = "[imGM]+";

  private String numericValue;
  private DecimalQuantityUnit unit;

  @JsonCreator
  public static CpuQuantity fromString(String quantity) {
    String[] parts = quantity.split(PARTS_REGEX);
    String numericValue = parts[0];
    String suffix = quantity.substring(parts[0].length());
    DecimalQuantityUnit unit = Stream.of(DecimalQuantityUnit.values())
                                   .filter(quantityUnit -> quantityUnit.getSuffix().equals(suffix))
                                   .findFirst()
                                   .orElse(DecimalQuantityUnit.unitless);

    return CpuQuantity.builder().numericValue(numericValue).unit(unit).build();
  }

  @JsonValue
  public String getYamlProperty() {
    return numericValue + unit.name();
  }
}

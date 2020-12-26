package io.harness.beans.yaml.extended.container.quantity;

import io.harness.beans.yaml.extended.container.quantity.unit.BinaryQuantityUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemoryQuantity {
  private static final String PARTS_REGEX = "[imGM]+";

  private String numericValue;
  private BinaryQuantityUnit unit;

  @JsonCreator
  public static MemoryQuantity fromString(String quantity) {
    String[] parts = quantity.split(PARTS_REGEX);
    String numericValue = parts[0];
    String suffix = quantity.substring(parts[0].length());
    BinaryQuantityUnit unit = Stream.of(BinaryQuantityUnit.values())
                                  .filter(quantityUnit -> quantityUnit.getSuffix().equals(suffix))
                                  .findFirst()
                                  .orElse(BinaryQuantityUnit.unitless);

    return MemoryQuantity.builder().numericValue(numericValue).unit(unit).build();
  }

  @JsonValue
  public String getYamlProperty() {
    return numericValue + unit.name();
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.quantity;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.quantity.unit.DecimalQuantityUnit;
import io.harness.exception.InvalidArgumentsException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
@Builder
@OwnedBy(CI)
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

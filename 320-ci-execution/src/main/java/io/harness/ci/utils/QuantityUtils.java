package io.harness.ci.utils;

import io.harness.beans.yaml.extended.container.quantity.CpuQuantity;
import io.harness.beans.yaml.extended.container.quantity.MemoryQuantity;
import io.harness.beans.yaml.extended.container.quantity.unit.BinaryQuantityUnit;
import io.harness.beans.yaml.extended.container.quantity.unit.DecimalQuantityUnit;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QuantityUtils {
  public Integer getMemoryQuantityValueInUnit(String memoryQuantityString, BinaryQuantityUnit targetUnit) {
    MemoryQuantity memoryQuantity = MemoryQuantity.fromString(memoryQuantityString);
    double numeric = Double.parseDouble(memoryQuantity.getNumericValue());
    double multiplier = Math.pow(memoryQuantity.getUnit().getBase(), memoryQuantity.getUnit().getExponent());
    double targetUnitMultiplier = Math.pow(targetUnit.getBase(), targetUnit.getExponent());
    return Math.toIntExact(Math.round(numeric * (multiplier / targetUnitMultiplier)));
  }

  public Integer getCpuQuantityValueInUnit(String cpuQuantityString, DecimalQuantityUnit targetUnit) {
    CpuQuantity cpuQuantity = CpuQuantity.fromString(cpuQuantityString);
    double numeric = Double.parseDouble(cpuQuantity.getNumericValue());
    double multiplier = Math.pow(cpuQuantity.getUnit().getBase(), cpuQuantity.getUnit().getExponent());
    double targetUnitMultiplier = Math.pow(targetUnit.getBase(), targetUnit.getExponent());
    return Math.toIntExact(Math.round(numeric * (multiplier / targetUnitMultiplier)));
  }
}

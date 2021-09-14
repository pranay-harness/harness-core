/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ci.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.quantity.CpuQuantity;
import io.harness.beans.quantity.MemoryQuantity;
import io.harness.beans.quantity.unit.DecimalQuantityUnit;
import io.harness.beans.quantity.unit.MemoryQuantityUnit;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class QuantityUtils {
  public Integer getMemoryQuantityValueInUnit(String memoryQuantityString, MemoryQuantityUnit targetUnit) {
    MemoryQuantity memoryQuantity = MemoryQuantity.fromString(memoryQuantityString);
    double numeric = Double.parseDouble(memoryQuantity.getNumericValue());
    double multiplier = Math.pow(memoryQuantity.getUnit().getBase(), memoryQuantity.getUnit().getExponent());
    double targetUnitMultiplier = Math.pow(targetUnit.getBase(), targetUnit.getExponent());
    return Math.toIntExact((long) Math.ceil(numeric * (multiplier / targetUnitMultiplier)));
  }

  public Integer getCpuQuantityValueInUnit(String cpuQuantityString, DecimalQuantityUnit targetUnit) {
    CpuQuantity cpuQuantity = CpuQuantity.fromString(cpuQuantityString);
    double numeric = Double.parseDouble(cpuQuantity.getNumericValue());
    double multiplier = Math.pow(cpuQuantity.getUnit().getBase(), cpuQuantity.getUnit().getExponent());
    double targetUnitMultiplier = Math.pow(targetUnit.getBase(), targetUnit.getExponent());
    return Math.toIntExact(Math.round(numeric * (multiplier / targetUnitMultiplier)));
  }
}

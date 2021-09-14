/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.quantity.unit;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@Getter
@OwnedBy(CI)
public enum DecimalQuantityUnit {
  m(10, -3, "m"),
  unitless(10, 0, "");

  private final long base;
  private final long exponent;
  private final String suffix;

  DecimalQuantityUnit(long base, long exponent, String suffix) {
    this.base = base;
    this.exponent = exponent;
    this.suffix = suffix;
  }
}

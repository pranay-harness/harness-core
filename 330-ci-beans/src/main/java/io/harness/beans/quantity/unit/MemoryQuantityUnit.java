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
public enum MemoryQuantityUnit {
  Mi(2, 20, "Mi"),
  Gi(2, 30, "Gi"),
  M(10, 6, "M"),
  G(10, 9, "G"),
  unitless(2, 0, "");

  private final long base;
  private final long exponent;
  private final String suffix;

  MemoryQuantityUnit(long base, long exponent, String suffix) {
    this.base = base;
    this.exponent = exponent;
    this.suffix = suffix;
  }
}

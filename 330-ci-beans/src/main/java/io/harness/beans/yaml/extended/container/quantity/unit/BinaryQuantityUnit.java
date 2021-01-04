package io.harness.beans.yaml.extended.container.quantity.unit;

import lombok.Getter;

@Getter
public enum BinaryQuantityUnit {
  Mi(2, 10, "Mi"),
  Gi(2, 20, "Gi"),
  unitless(2, 0, "");

  private final long base;
  private final long exponent;
  private final String suffix;

  BinaryQuantityUnit(long base, long exponent, String suffix) {
    this.base = base;
    this.exponent = exponent;
    this.suffix = suffix;
  }
}

package io.harness.beans.yaml.extended.container.quantity.unit;

import lombok.Getter;

@Getter
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

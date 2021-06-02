package io.harness.yaml.extended.ci.container.quantity.unit;

import lombok.Getter;

@Getter
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

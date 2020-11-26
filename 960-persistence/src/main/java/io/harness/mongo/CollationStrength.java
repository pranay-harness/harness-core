package io.harness.mongo;

public enum CollationStrength {
  PRIMARY(1),
  SECONDARY(2),
  TERTIARY(3),
  QUATERNARY(4),
  IDENTICAL(5);

  private final int code;

  public int getCode() {
    return this.code;
  }

  CollationStrength(int code) {
    this.code = code;
  }
}

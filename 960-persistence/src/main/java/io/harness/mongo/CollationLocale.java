package io.harness.mongo;

public enum CollationLocale {
  ENGLISH("en");

  private final String code;

  CollationLocale(String code) {
    this.code = code;
  }

  public String getCode() {
    return this.code;
  }

  @Override
  public String toString() {
    return code;
  }
}

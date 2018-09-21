package io.harness.data.parser;

public class Parser {
  public static int asInt(String value) {
    return asInt(value, 0);
  }

  public static int asInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (Exception exception) {
      return defaultValue;
    }
  }
}

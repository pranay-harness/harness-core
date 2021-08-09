package io.harness.cvng.beans;

public enum TimeSeriesThresholdType {
  ACT_WHEN_LOWER("<"),
  ACT_WHEN_HIGHER(">");

  private String symbol;

  TimeSeriesThresholdType(String symbol) {
    this.symbol = symbol;
  }

  public static TimeSeriesThresholdType valueFromSymbol(String symbol) {
    for (TimeSeriesThresholdType timeSeriesThresholdType : TimeSeriesThresholdType.values()) {
      if (timeSeriesThresholdType.symbol.equals(symbol.trim())) {
        return timeSeriesThresholdType;
      }
    }

    throw new IllegalArgumentException("Invalid type symbol");
  }
}

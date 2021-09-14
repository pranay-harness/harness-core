/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

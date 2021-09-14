/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.common;

import io.harness.exception.InvalidRequestException;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.groovy.util.Maps;

@UtilityClass
public class NGTimeConversionHelper {
  public Map<String, Double> unitCharactersToMilliSeconds =
      Maps.of("w", 6.048e8, "d", 8.64e7, "h", 3.6e6, "m", 6e4, "s", 1e3, "ms", 1e0);

  public long convertTimeStringToMilliseconds(String timeInString) {
    double result = 0;
    double currentValue = 0;
    StringBuilder currentUnit = new StringBuilder();
    for (Character ch : timeInString.toCharArray()) {
      if (Character.isDigit(ch)) {
        if (currentUnit.length() != 0) {
          if (!unitCharactersToMilliSeconds.containsKey(currentUnit.toString())) {
            throw new InvalidRequestException(
                String.format("Given format not supported for timeout: %s. Supported formats are: %s",
                    currentUnit.toString(), unitCharactersToMilliSeconds.keySet()));
          }
          result += unitCharactersToMilliSeconds.get(currentUnit.toString()) * currentValue;
          currentUnit = new StringBuilder();
          currentValue = 0;
        }
        currentValue = currentValue * 10 + (ch - '0');
      } else {
        currentUnit.append(ch);
      }
    }
    if (currentUnit.length() != 0) {
      if (!unitCharactersToMilliSeconds.containsKey(currentUnit.toString())) {
        throw new InvalidRequestException(
            String.format("Given format not supported for timeout: %s:", currentUnit.toString()));
      }
      result += unitCharactersToMilliSeconds.get(currentUnit.toString()) * currentValue;
    }
    return (long) result;
  }

  public int convertTimeStringToMinutes(String timeInString) {
    long timeInMilliSeconds = convertTimeStringToMilliseconds(timeInString);
    if (timeInMilliSeconds == 0) {
      throw new InvalidRequestException("timeInMilliSeconds cannot be 0");
    }
    return (int) (convertTimeStringToMilliseconds(timeInString) / 60000);
  }
}

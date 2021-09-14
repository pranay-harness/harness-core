/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.utils;

public class ErrorMessageUtils {
  private ErrorMessageUtils() {}

  public static String generateErrorMessageFromParam(String paramName) {
    return paramName + " should not be null";
  }
}

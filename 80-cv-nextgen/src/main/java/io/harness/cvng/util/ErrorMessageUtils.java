package io.harness.cvng.util;

public class ErrorMessageUtils {
  private ErrorMessageUtils() {}

  public static String generateErrorMessageFromParam(String paramName) {
    return paramName + " should not be null";
  }
}

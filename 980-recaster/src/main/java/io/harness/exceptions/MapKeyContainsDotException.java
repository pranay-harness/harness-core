package io.harness.exceptions;

public class MapKeyContainsDotException extends RuntimeException {
  public MapKeyContainsDotException(final String message) {
    super(message);
  }

  public MapKeyContainsDotException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

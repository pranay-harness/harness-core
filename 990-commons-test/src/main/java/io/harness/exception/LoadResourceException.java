package io.harness.exception;

public class LoadResourceException extends RuntimeException {
  public LoadResourceException(String resource, Exception e) {
    super("Unable to load resource  " + resource, e);
  }
}

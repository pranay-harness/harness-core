package io.harness.pcf;

public class PivotalClientApiException extends Exception {
  public PivotalClientApiException(String s) {
    super(s);
  }

  public PivotalClientApiException(String s, Throwable t) {
    super(s, t);
  }
}

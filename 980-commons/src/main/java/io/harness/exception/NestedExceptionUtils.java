package io.harness.exception;

public class NestedExceptionUtils {
  public static WingsException hintWithExplanationException(
      String hintMessage, String explanationMessage, Throwable cause) {
    return new HintException(hintMessage, new ExplanationException(explanationMessage, cause));
  }
}

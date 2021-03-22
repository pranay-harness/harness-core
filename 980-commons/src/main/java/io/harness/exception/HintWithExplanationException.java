package io.harness.exception;

public class HintWithExplanationException {
  public static WingsException build(String hintMessage, String explanationMessage, Throwable cause) {
    return new HintException(hintMessage, new ExplanationException(explanationMessage, cause));
  }
}

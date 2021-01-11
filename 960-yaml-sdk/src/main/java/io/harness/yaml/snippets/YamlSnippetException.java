package io.harness.yaml.snippets;

import static io.harness.eraro.ErrorCode.UNEXPECTED_SNIPPET_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class YamlSnippetException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public YamlSnippetException(String message) {
    super(message, null, UNEXPECTED_SNIPPET_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}

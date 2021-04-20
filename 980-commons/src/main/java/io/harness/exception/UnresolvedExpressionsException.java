package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNRESOLVED_EXPRESSIONS_ERROR;

import io.harness.eraro.Level;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnresolvedExpressionsException extends WingsException {
  public static final String FINAL_EXPRESSION_ARG = "finalExpression";
  public static final String EXPRESSIONS_ARG = "expressions";

  public UnresolvedExpressionsException(String finalExpression, List<String> expressions) {
    super(null, null, UNRESOLVED_EXPRESSIONS_ERROR, Level.ERROR, null, null);
    super.param(FINAL_EXPRESSION_ARG, finalExpression);
    super.param(EXPRESSIONS_ARG,
        expressions == null ? "null" : expressions.stream().filter(Objects::nonNull).collect(Collectors.joining(", ")));
  }

  public String fetchFinalExpression() {
    return (String) getParams().get(FINAL_EXPRESSION_ARG);
  }
}

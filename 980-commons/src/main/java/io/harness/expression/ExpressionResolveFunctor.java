package io.harness.expression;

public interface ExpressionResolveFunctor {
  String renderExpression(String expression);
  Object evaluateExpression(String expression);
  boolean hasVariables(String expression);

  default ResolveObjectResponse processObject(Object o) {
    return new ResolveObjectResponse(false, false);
  }
}

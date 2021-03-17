package io.harness.expression;

public interface ExpressionResolveFunctor {
  String processString(String expression);

  default ResolveObjectResponse processObject(Object o) {
    return new ResolveObjectResponse(false, null);
  }
}

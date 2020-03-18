package software.wings.expression;

import io.harness.expression.ExpressionFunctor;

public class SecretManagerPreviewFunctor implements ExpressionFunctor, SecretManagerFunctorInterface {
  @Override
  public Object obtain(String secretName, int token) {
    return "<<<" + secretName + ">>>";
  }
}

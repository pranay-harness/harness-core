package software.wings.expression;

import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionFunctor;
import lombok.Value;

@Value
public class ManagerPreviewExpressionEvaluator extends ExpressionEvaluator {
  private final ExpressionFunctor secretManagerFunctor;

  public ManagerPreviewExpressionEvaluator() {
    secretManagerFunctor = new SecretManagerPreviewFunctor();
    addFunctor(SecretManagerFunctorInterface.FUNCTOR_NAME, secretManagerFunctor);
  }
}

package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionFunctor;
import lombok.Value;

@OwnedBy(CDC)
@Value
public class ManagerPreviewExpressionEvaluator extends ExpressionEvaluator {
  private final ExpressionFunctor secretManagerFunctor;

  public ManagerPreviewExpressionEvaluator() {
    secretManagerFunctor = new SecretManagerPreviewFunctor();
    addFunctor(SecretManagerFunctorInterface.FUNCTOR_NAME, secretManagerFunctor);
  }
}

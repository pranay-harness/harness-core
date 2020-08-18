package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;

@OwnedBy(CDC)
public class SecretManagerPreviewFunctor implements ExpressionFunctor, SecretManagerFunctorInterface {
  private String formatter;

  public SecretManagerPreviewFunctor() {
    this.formatter = "<<<%s>>>";
  }

  public SecretManagerPreviewFunctor(String formatter) {
    this.formatter = formatter;
  }

  @Override
  public Object obtain(String secretName, int token) {
    return String.format(formatter, secretName);
  }
}

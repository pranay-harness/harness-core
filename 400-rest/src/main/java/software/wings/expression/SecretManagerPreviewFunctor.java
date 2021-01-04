package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;

@OwnedBy(CDC)
public class SecretManagerPreviewFunctor implements ExpressionFunctor, SecretManagerFunctorInterface {
  public static final String SECRET_EXPRESSION_FORMATTER = "${secrets.getValue(\"%s\")}";
  public static final String SECRET_NAME_FORMATTER = "<<<%s>>>";

  private String formatter;

  public SecretManagerPreviewFunctor() {
    this.formatter = SECRET_NAME_FORMATTER;
  }

  public SecretManagerPreviewFunctor(String formatter) {
    this.formatter = formatter;
  }

  public static SecretManagerPreviewFunctor functorWithSecretExpressionFormat() {
    return new SecretManagerPreviewFunctor(SECRET_EXPRESSION_FORMATTER);
  }

  @Override
  public Object obtain(String secretName, int token) {
    return String.format(formatter, secretName);
  }
}

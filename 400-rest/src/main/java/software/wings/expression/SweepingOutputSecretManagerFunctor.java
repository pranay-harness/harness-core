package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;
import io.harness.expression.SecretString;
import io.harness.security.SimpleEncryption;

import java.util.Base64;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class SweepingOutputSecretManagerFunctor implements ExpressionFunctor {
  SimpleEncryption simpleEncryption;

  public Object obtain(String secretKey, String secretValue) {
    String decryptedValue = new String(simpleEncryption.decrypt(Base64.getDecoder().decode(secretValue)));
    return SecretString.builder().value(decryptedValue).build();
  }
}

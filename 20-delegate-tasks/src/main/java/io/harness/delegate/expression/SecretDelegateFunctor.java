package io.harness.delegate.expression;

import io.harness.exception.FunctorException;
import io.harness.expression.ExpressionFunctor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class SecretDelegateFunctor implements ExpressionFunctor {
  private Map<String, char[]> secrets;

  public Object obtain(String secretDetailsUuid) {
    if (secrets.containsKey(secretDetailsUuid)) {
      return new String(secrets.get(secretDetailsUuid));
    }
    throw new FunctorException("Secret details not found");
  }
}

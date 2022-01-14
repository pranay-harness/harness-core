package io.harness.pms.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.EngineFunctorException;
import io.harness.expression.LateBindingValue;
import io.harness.network.SafeHttpCall;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

@OwnedBy(PIPELINE)
public class AccountFunctor implements LateBindingValue {
  private final AccountClient accountClient;
  private final Ambiance ambiance;

  public AccountFunctor(AccountClient accountClient, Ambiance ambiance) {
    this.accountClient = accountClient;
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    if (EmptyPredicate.isEmpty(accountId)) {
      return null;
    }

    try {
      return SafeHttpCall.execute(accountClient.getAccountDTO(accountId)).getResource();
    } catch (Exception ex) {
      throw new EngineFunctorException(String.format("Invalid account: %s", accountId), ex);
    }
  }
}

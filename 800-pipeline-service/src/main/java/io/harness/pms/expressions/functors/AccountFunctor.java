package io.harness.pms.expressions.functors;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.FunctorException;
import io.harness.expression.LateBindingValue;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.account.remote.AccountClient;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;

public class AccountFunctor implements LateBindingValue {
  private final AccountClient accountClient;
  private final Ambiance ambiance;

  public AccountFunctor(AccountClient accountClient, Ambiance ambiance) {
    this.accountClient = accountClient;
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    if (EmptyPredicate.isEmpty(accountId)) {
      return null;
    }

    try {
      return SafeHttpCall.execute(accountClient.getAccountDTO(accountId)).getResource();
    } catch (Exception ex) {
      throw new FunctorException(String.format("Invalid account: %s", accountId), ex);
    }
  }
}

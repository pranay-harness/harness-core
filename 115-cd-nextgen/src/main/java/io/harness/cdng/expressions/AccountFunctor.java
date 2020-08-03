package io.harness.cdng.expressions;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.common.AmbianceHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.LateBindingValue;
import software.wings.service.intfc.AccountService;

public class AccountFunctor implements LateBindingValue {
  private final AccountService accountService;
  private final Ambiance ambiance;

  public AccountFunctor(AccountService accountService, Ambiance ambiance) {
    this.accountService = accountService;
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    return EmptyPredicate.isEmpty(accountId) ? null : accountService.get(accountId);
  }
}

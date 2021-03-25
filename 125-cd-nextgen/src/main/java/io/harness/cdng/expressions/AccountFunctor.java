package io.harness.cdng.expressions;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.expression.LateBindingValue;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;

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
    return hasNone(accountId) ? null : accountService.get(accountId);
  }
}

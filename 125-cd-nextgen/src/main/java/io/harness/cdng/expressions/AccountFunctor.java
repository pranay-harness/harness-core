/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.expressions;

import io.harness.data.structure.EmptyPredicate;
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
    return EmptyPredicate.isEmpty(accountId) ? null : accountService.get(accountId);
  }
}

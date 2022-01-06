/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.expressions;

import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.LateBindingValue;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

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
    String accountId = AmbianceUtils.getAccountId(ambiance);
    return EmptyPredicate.isEmpty(accountId) ? null : accountService.get(accountId);
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.changehandlers.AccountChangeDataHandler;
import io.harness.persistence.PersistentEntity;

import software.wings.beans.Account;

import com.google.inject.Inject;

public class AccountEntity implements CDCEntity<Account> {
  @Inject private AccountChangeDataHandler accountChangeDataHandler;

  @Override
  public ChangeHandler getChangeHandler(String handlerClass) {
    return accountChangeDataHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return Account.class;
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.springdata;

import java.util.function.Consumer;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class HTransactionTemplate extends TransactionTemplate {
  boolean transactionsEnabled;

  public HTransactionTemplate(MongoTransactionManager mongoTransactionManager, boolean transactionsEnabled) {
    super(mongoTransactionManager);
    this.transactionsEnabled = transactionsEnabled;
  }

  @Override
  public <T> T execute(TransactionCallback<T> action) throws TransactionException {
    if (!transactionsEnabled) {
      return action.doInTransaction(new SimpleTransactionStatus(false));
    }
    return super.execute(action);
  }

  @Override
  public void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
    if (!transactionsEnabled) {
      action.accept(new SimpleTransactionStatus(false));
    } else {
      super.executeWithoutResult(action);
    }
  }
}

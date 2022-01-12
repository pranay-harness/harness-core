/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.jobs.workflow.collection;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;

import software.wings.beans.Account;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CVDataCollectionJob implements Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    continuousVerificationService.processNextCVTasks(accountId);
    continuousVerificationService.expireLongRunningCVTasks(accountId);
    continuousVerificationService.retryCVTasks(accountId);
  }
}

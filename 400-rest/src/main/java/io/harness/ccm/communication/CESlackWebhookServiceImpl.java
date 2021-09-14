/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.communication;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.communication.entities.CESlackWebhook;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public class CESlackWebhookServiceImpl implements CESlackWebhookService {
  @Inject private CESlackWebhookDao ceSlackWebhookDao;

  public CESlackWebhook upsert(CESlackWebhook slackWebhook) {
    return ceSlackWebhookDao.upsert(slackWebhook);
  }

  public CESlackWebhook getByAccountId(String accountId) {
    return ceSlackWebhookDao.getByAccountId(accountId);
  }
}

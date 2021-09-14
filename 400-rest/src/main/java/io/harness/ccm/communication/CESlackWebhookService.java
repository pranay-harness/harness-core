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

@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public interface CESlackWebhookService {
  CESlackWebhook upsert(CESlackWebhook slackWebhook);
  CESlackWebhook getByAccountId(String accountId);
}

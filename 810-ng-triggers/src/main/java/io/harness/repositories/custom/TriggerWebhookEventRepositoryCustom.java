/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public interface TriggerWebhookEventRepositoryCustom {
  TriggerWebhookEvent update(Criteria criteria, TriggerWebhookEvent ngTriggerEntity);
}

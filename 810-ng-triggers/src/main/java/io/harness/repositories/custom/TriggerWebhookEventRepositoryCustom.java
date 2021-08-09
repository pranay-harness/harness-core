package io.harness.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public interface TriggerWebhookEventRepositoryCustom {
  TriggerWebhookEvent update(Criteria criteria, TriggerWebhookEvent ngTriggerEntity);
}

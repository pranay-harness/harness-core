package io.harness.repositories.ng.core.custom;

import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

import org.springframework.data.mongodb.core.query.Criteria;

public interface TriggerWebhookEventRepositoryCustom {
  TriggerWebhookEvent update(Criteria criteria, TriggerWebhookEvent ngTriggerEntity);
}

package io.harness.ngtriggers.service;

import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface NGTriggerService {
  NGTriggerEntity create(NGTriggerEntity ngTriggerEntity);

  Optional<NGTriggerEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String targetIdentifier, String identifier, boolean deleted);

  NGTriggerEntity update(NGTriggerEntity ngTriggerEntity);

  boolean updateTriggerStatus(NGTriggerEntity ngTriggerEntity, boolean status);

  Page<NGTriggerEntity> list(Criteria criteria, Pageable pageable);

  Page<NGTriggerEntity> listWebhookTriggers(
      TriggerWebhookEvent triggerWebhookEvent, List<String> repoUrls, boolean isDeleted, boolean enabledOnly);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String targetIdentifier,
      String identifier, Long version);

  TriggerWebhookEvent addEventToQueue(TriggerWebhookEvent webhookEventQueueRecord);
  TriggerWebhookEvent updateTriggerWebhookEvent(TriggerWebhookEvent webhookEventQueueRecord);
  void deleteTriggerWebhookEvent(TriggerWebhookEvent webhookEventQueueRecord);
}

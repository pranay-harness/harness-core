package io.harness.ngtriggers.service;

import io.harness.connector.ConnectorResponseDTO;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
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

  List<NGTriggerEntity> listEnabledTriggersForCurrentProject(
      String accountId, String orgIdentifier, String projectIdentifier);

  List<NGTriggerEntity> findTriggersForCustomWehbook(
      TriggerWebhookEvent triggerWebhookEvent, String decryptedAuthToken, boolean isDeleted, boolean enabled);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String targetIdentifier,
      String identifier, Long version);

  TriggerWebhookEvent addEventToQueue(TriggerWebhookEvent webhookEventQueueRecord);
  TriggerWebhookEvent updateTriggerWebhookEvent(TriggerWebhookEvent webhookEventQueueRecord);
  void deleteTriggerWebhookEvent(TriggerWebhookEvent webhookEventQueueRecord);
  List<ConnectorResponseDTO> fetchConnectorsByFQN(String accountId, List<String> fqns);

  void validateTriggerConfig(TriggerDetails triggerDetails);
}

package io.harness.ngtriggers.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.repository.custom.NGTriggerWebhookEventQueueRepositoryCustom;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface NGTriggerWebhookEventQueueRepository
    extends PagingAndSortingRepository<TriggerWebhookEvent, String>, NGTriggerWebhookEventQueueRepositoryCustom {
  Optional<TriggerWebhookEvent> findByAccountIdAndUuid(String accountId, String uuid);
}
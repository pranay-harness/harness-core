package io.harness.ngtriggers.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.repository.custom.TriggerWebhookEventRepositoryCustom;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface TriggerWebhookEventRepository
    extends PagingAndSortingRepository<TriggerWebhookEvent, String>, TriggerWebhookEventRepositoryCustom {
  Optional<TriggerWebhookEvent> findByAccountIdAndUuid(String accountId, String uuid);
}

package io.harness.repositories.ng.core.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.repositories.ng.core.custom.TriggerWebhookEventRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface TriggerWebhookEventRepository
    extends PagingAndSortingRepository<TriggerWebhookEvent, String>, TriggerWebhookEventRepositoryCustom {
  Optional<TriggerWebhookEvent> findByAccountIdAndUuid(String accountId, String uuid);
}

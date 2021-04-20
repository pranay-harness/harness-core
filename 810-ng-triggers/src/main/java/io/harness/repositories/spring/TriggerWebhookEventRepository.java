package io.harness.repositories.spring;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.repositories.custom.TriggerWebhookEventRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PIPELINE)
public interface TriggerWebhookEventRepository
    extends PagingAndSortingRepository<TriggerWebhookEvent, String>, TriggerWebhookEventRepositoryCustom {
  Optional<TriggerWebhookEvent> findByAccountIdAndUuid(String accountId, String uuid);
}

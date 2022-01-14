package io.harness.repositories.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.outbox.OutboxEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface OutboxEventRepository
    extends PagingAndSortingRepository<OutboxEvent, String>, OutboxEventCustomRepository {
  Page<OutboxEvent> findByBlockedFalseOrBlockedNull(Pageable pageable);
}

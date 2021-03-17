package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.outbox.OutboxEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface OutboxEventRepository
    extends PagingAndSortingRepository<OutboxEvent, String>, OutboxEventCustomRepository {
  Page<OutboxEvent> findByBlockedFalseOrBlockedNull(Pageable pageable);
}

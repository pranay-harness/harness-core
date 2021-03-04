package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.outbox.Outbox;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface OutboxRepository extends PagingAndSortingRepository<Outbox, String>, OutboxCustomRepository {}

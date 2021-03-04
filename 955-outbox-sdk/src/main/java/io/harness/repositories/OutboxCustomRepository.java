package io.harness.repositories;

import io.harness.outbox.Outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface OutboxCustomRepository {
  Page<Outbox> findAll(Criteria criteria, Pageable pageable);
}

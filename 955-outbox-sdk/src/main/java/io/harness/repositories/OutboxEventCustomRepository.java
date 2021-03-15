package io.harness.repositories;

import io.harness.outbox.OutboxEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OutboxEventCustomRepository { Page<OutboxEvent> findAll(Pageable pageable); }

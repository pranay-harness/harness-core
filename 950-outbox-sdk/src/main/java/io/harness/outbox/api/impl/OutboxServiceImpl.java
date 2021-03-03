package io.harness.outbox.api.impl;

import io.harness.outbox.Outbox;
import io.harness.outbox.api.OutboxService;
import io.harness.outbox.repositories.OutboxRepository;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class OutboxServiceImpl implements OutboxService {
  private final OutboxRepository outboxRepository;

  @Override
  public Outbox save(Outbox outbox) {
    return outboxRepository.save(outbox);
  }

  @Override
  public List<Outbox> list() {
    return outboxRepository.findAllByOrderByCreatedAtAsc();
  }

  @Override
  public boolean delete(String uuid) {
    outboxRepository.deleteById(uuid);
    return true;
  }
}

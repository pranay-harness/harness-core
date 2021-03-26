package io.harness.outbox.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxDao;
import io.harness.repositories.OutboxEventRepository;

import com.google.inject.Inject;
import org.springframework.util.Assert;

@OwnedBy(PL)
public class OutboxDaoImpl implements OutboxDao {
  private final OutboxEventRepository outboxRepository;

  @Inject
  public OutboxDaoImpl(OutboxEventRepository outboxRepository) {
    this.outboxRepository = outboxRepository;
  }

  @Override
  public OutboxEvent save(OutboxEvent outboxEvent) {
    return outboxRepository.save(outboxEvent);
  }

  @Override
  public PageResponse<OutboxEvent> list(PageRequest pageRequest) {
    Assert.notNull(pageRequest, "PageRequest must not be null!");
    return getNGPageResponse(outboxRepository.findByBlockedFalseOrBlockedNull(getPageRequest(pageRequest)));
  }

  @Override
  public boolean delete(String outboxEventId) {
    outboxRepository.deleteById(outboxEventId);
    return true;
  }
}

package io.harness.outbox.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_CREATED_AT_ASC_SORT_ORDER;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;
import io.harness.outbox.api.OutboxDao;
import io.harness.outbox.filter.OutboxEventFilter;
import io.harness.repositories.outbox.OutboxEventRepository;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;
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
  public List<OutboxEvent> list(OutboxEventFilter outboxEventFilter) {
    Assert.notNull(outboxEventFilter, "OutboxEventFilter must not be null!");
    return outboxRepository.findAll(getCriteria(outboxEventFilter),
        getPageRequest(PageRequest.builder()
                           .pageIndex(0)
                           .pageSize(outboxEventFilter.getMaximumEventsPolled())
                           .sortOrders(DEFAULT_CREATED_AT_ASC_SORT_ORDER)
                           .build()));
  }

  private Criteria getCriteria(OutboxEventFilter outboxEventFilter) {
    Criteria criteria = new Criteria();
    Criteria blockedNotTrueCriteria = Criteria.where(OutboxEventKeys.blocked).ne(Boolean.TRUE);
    Criteria blockedTrueCriteria = Criteria.where(OutboxEventKeys.blocked)
                                       .is(Boolean.TRUE)
                                       .and(OutboxEventKeys.nextUnblockAttemptAt)
                                       .lt(Instant.now());
    criteria.orOperator(blockedNotTrueCriteria, blockedTrueCriteria);
    return criteria;
  }

  @Override
  public boolean delete(String outboxEventId) {
    outboxRepository.deleteById(outboxEventId);
    return true;
  }
}

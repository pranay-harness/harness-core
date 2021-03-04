package io.harness.outbox.api.impl;

import static io.harness.beans.SortOrder.OrderType.ASC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.Outbox;
import io.harness.outbox.Outbox.OutboxKeys;
import io.harness.outbox.OutboxFilter;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.OutboxRepository;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class OutboxServiceImpl implements OutboxService {
  private final OutboxRepository outboxRepository;

  @Override
  public Outbox save(Outbox outbox) {
    return outboxRepository.save(outbox);
  }

  @Override
  public PageResponse<Outbox> list(OutboxFilter outboxFilter) {
    Criteria criteria = getOutboxFilterCriteria(outboxFilter);
    Pageable pageable = getPageable(outboxFilter);
    return getNGPageResponse(outboxRepository.findAll(criteria, pageable));
  }

  private Pageable getPageable(OutboxFilter outboxFilter) {
    PageRequest pageRequest;
    if (outboxFilter != null && outboxFilter.getPageRequest() != null) {
      pageRequest = outboxFilter.getPageRequest();
    } else {
      pageRequest = PageRequest.builder().pageIndex(0).pageSize(30).build();
    }
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order = SortOrder.Builder.aSortOrder().withField(OutboxKeys.createdAt, ASC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    return getPageRequest(pageRequest);
  }

  private Criteria getOutboxFilterCriteria(OutboxFilter outboxFilter) {
    Criteria criteria = new Criteria();
    if (outboxFilter != null) {
      if (isNotEmpty(outboxFilter.getOutboxIds())) {
        criteria.and(OutboxKeys.id).in(outboxFilter.getOutboxIds());
      }
      if (isNotEmpty(outboxFilter.getTypes())) {
        criteria.and(OutboxKeys.type).in(outboxFilter.getTypes());
      }
    }
    return criteria;
  }

  @Override
  public boolean delete(String outboxId) {
    outboxRepository.deleteById(outboxId);
    return true;
  }
}

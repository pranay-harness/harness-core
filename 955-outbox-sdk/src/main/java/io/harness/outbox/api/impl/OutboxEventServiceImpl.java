package io.harness.outbox.api.impl;

import static io.harness.beans.SortOrder.OrderType.ASC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.HEvent;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;
import io.harness.outbox.api.OutboxEventService;
import io.harness.repositories.OutboxEventRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class OutboxEventServiceImpl implements OutboxEventService {
  private final OutboxEventRepository outboxRepository;
  private final ObjectMapper objectMapper;

  @Override
  public OutboxEvent save(HEvent event) {
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .resourceScope(event.getResourceScope())
                                  .resource(event.getResource())
                                  .eventData(objectMapper.valueToTree(event.getEventData()))
                                  .eventType(event.getEventType())
                                  .build();
    return outboxRepository.save(outboxEvent);
  }

  @Override
  public PageResponse<OutboxEvent> list(PageRequest pageRequest) {
    Pageable pageable = getPageable(pageRequest);
    return getNGPageResponse(outboxRepository.findAll(pageable));
  }

  private Pageable getPageable(PageRequest pageRequest) {
    if (pageRequest == null) {
      pageRequest = PageRequest.builder().pageIndex(0).pageSize(20).build();
    }
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order = SortOrder.Builder.aSortOrder().withField(OutboxEventKeys.createdAt, ASC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    return getPageRequest(pageRequest);
  }

  @Override
  public boolean delete(String outboxEventId) {
    outboxRepository.deleteById(outboxEventId);
    return true;
  }
}

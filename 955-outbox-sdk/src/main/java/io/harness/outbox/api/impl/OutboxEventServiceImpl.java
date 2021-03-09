package io.harness.outbox.api.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.outbox.OutboxSDKConstants.OUTBOX_POLL_JOB_PAGE_REQUEST;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.Event;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventService;
import io.harness.repositories.OutboxEventRepository;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.springframework.data.domain.Pageable;

public class OutboxEventServiceImpl implements OutboxEventService {
  private final OutboxEventRepository outboxRepository;
  private final Gson gson;
  private final PageRequest pageRequest;

  @Inject
  public OutboxEventServiceImpl(
      OutboxEventRepository outboxRepository, Gson gson, @Named(OUTBOX_POLL_JOB_PAGE_REQUEST) PageRequest pageRequest) {
    this.outboxRepository = outboxRepository;
    this.gson = gson;
    this.pageRequest = pageRequest;
  }

  @Override
  public OutboxEvent save(Event event) {
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .resourceScope(event.getResourceScope())
                                  .resource(event.getResource())
                                  .eventData(gson.toJson(event.getEventData()))
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
      pageRequest = this.pageRequest;
    }
    if (isEmpty(pageRequest.getSortOrders())) {
      pageRequest.setSortOrders(this.pageRequest.getSortOrders());
    }
    return getPageRequest(pageRequest);
  }

  @Override
  public boolean delete(String outboxEventId) {
    outboxRepository.deleteById(outboxEventId);
    return true;
  }
}

package io.harness.outbox.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_PAGE_REQUEST;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxDao;
import io.harness.outbox.api.OutboxService;

import com.google.gson.Gson;
import com.google.inject.Inject;

@OwnedBy(PL)
public class OutboxServiceImpl implements OutboxService {
  private final OutboxDao outboxDao;
  private final Gson gson;
  private final PageRequest pageRequest;

  @Inject
  public OutboxServiceImpl(OutboxDao outboxDao, Gson gson) {
    this.outboxDao = outboxDao;
    this.gson = gson;
    this.pageRequest = DEFAULT_OUTBOX_POLL_PAGE_REQUEST;
  }

  @Override
  public OutboxEvent save(Event event) {
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .resourceScope(event.getResourceScope())
                                  .resource(event.getResource())
                                  .eventData(gson.toJson(event))
                                  .eventType(event.getEventType())
                                  .globalContext(GlobalContextManager.obtainGlobalContext())
                                  .build();
    return outboxDao.save(outboxEvent);
  }

  @Override
  public OutboxEvent update(OutboxEvent outboxEvent) {
    return outboxDao.save(outboxEvent);
  }

  @Override
  public PageResponse<OutboxEvent> list(PageRequest pageRequest) {
    return outboxDao.list(getPageRequest(pageRequest));
  }

  private PageRequest getPageRequest(PageRequest pageRequest) {
    if (pageRequest == null) {
      pageRequest = this.pageRequest;
    }
    if (isEmpty(pageRequest.getSortOrders())) {
      pageRequest.setSortOrders(this.pageRequest.getSortOrders());
    }
    return pageRequest;
  }

  @Override
  public boolean delete(String outboxEventId) {
    outboxDao.delete(outboxEventId);
    return true;
  }
}

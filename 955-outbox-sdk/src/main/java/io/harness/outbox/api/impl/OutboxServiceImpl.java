package io.harness.outbox.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_EVENT_FILTER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.manage.GlobalContextManager;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxDao;
import io.harness.outbox.api.OutboxService;
import io.harness.outbox.filter.OutboxEventFilter;

import com.google.gson.Gson;
import com.google.inject.Inject;
import java.util.List;

@OwnedBy(PL)
public class OutboxServiceImpl implements OutboxService {
  private final OutboxDao outboxDao;
  private final Gson gson;

  @Inject
  public OutboxServiceImpl(OutboxDao outboxDao, Gson gson) {
    this.outboxDao = outboxDao;
    this.gson = gson;
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
  public List<OutboxEvent> list(OutboxEventFilter outboxEventFilter) {
    if (outboxEventFilter == null) {
      outboxEventFilter = DEFAULT_OUTBOX_EVENT_FILTER;
    }
    return outboxDao.list(outboxEventFilter);
  }

  @Override
  public boolean delete(String outboxEventId) {
    outboxDao.delete(outboxEventId);
    return true;
  }
}

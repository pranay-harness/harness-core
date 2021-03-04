package io.harness.ng;

import io.harness.ng.outbox.NextGenOutboxEventHandler;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.outbox.api.OutboxEventService;
import io.harness.outbox.api.impl.OutboxEventServiceImpl;

import com.google.inject.AbstractModule;

public class OutboxEventModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(OutboxEventService.class).to(OutboxEventServiceImpl.class);
    bind(OutboxEventHandler.class).to(NextGenOutboxEventHandler.class);
  }
}

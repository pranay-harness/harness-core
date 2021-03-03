package io.harness.ng;

import io.harness.ng.outbox.NextGenOutboxHandler;
import io.harness.outbox.api.OutboxHandler;
import io.harness.outbox.api.OutboxService;
import io.harness.outbox.api.impl.OutboxServiceImpl;

import com.google.inject.AbstractModule;

public class OutboxModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(OutboxService.class).to(OutboxServiceImpl.class);
    bind(OutboxHandler.class).to(NextGenOutboxHandler.class);
  }
}

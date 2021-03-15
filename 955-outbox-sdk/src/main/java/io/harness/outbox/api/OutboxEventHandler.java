package io.harness.outbox.api;

import io.harness.outbox.OutboxEvent;

public interface OutboxEventHandler { boolean handle(OutboxEvent outboxEvent); }

package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;

// Don't use this interface, instead use MessageListener
@OwnedBy(PL)
public interface MessageProcessor {
  boolean processMessage(Message message);
}

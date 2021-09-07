package io.harness.accesscontrol.commons.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;

@OwnedBy(HarnessTeam.PL)
public interface EventFilter {
  boolean filter(Message message);
}

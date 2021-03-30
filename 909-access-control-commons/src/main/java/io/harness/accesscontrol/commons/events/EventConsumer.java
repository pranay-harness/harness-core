package io.harness.accesscontrol.commons.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface EventConsumer {
  EventFilter getEventFilter();
  EventHandler getEventHandler();
}

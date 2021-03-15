package io.harness.accesscontrol.scopes.harness.events;

import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ScopeEventConsumer implements EventConsumer {
  private final ScopeEventFilter eventFilter;
  private final ScopeEventHandler eventHandler;
  public static final Set<String> SCOPE_EVENT_ENTITY_TYPES =
      Arrays.stream(HarnessScopeLevel.values()).map(HarnessScopeLevel::getEventEntityName).collect(Collectors.toSet());

  @Inject
  public ScopeEventConsumer(ScopeEventFilter eventFilter, ScopeEventHandler eventHandler) {
    this.eventFilter = eventFilter;
    this.eventHandler = eventHandler;
  }

  @Override
  public EventFilter getEventFilter() {
    return eventFilter;
  }

  @Override
  public EventHandler getEventHandler() {
    return eventHandler;
  }
}

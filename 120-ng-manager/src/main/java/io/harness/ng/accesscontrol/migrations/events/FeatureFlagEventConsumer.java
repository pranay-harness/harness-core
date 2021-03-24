package io.harness.ng.accesscontrol.migrations.events;

import io.harness.eventsframework.api.EventConsumer;
import io.harness.eventsframework.api.EventFilter;
import io.harness.eventsframework.api.EventHandler;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class FeatureFlagEventConsumer implements EventConsumer {
  private final FeatureFlagEventHandler featureFlagEventHandler;
  private final FeatureFlagFilter featureFlagFilter;

  @Override
  public EventFilter getEventFilter() {
    return this.featureFlagFilter;
  }

  @Override
  public EventHandler getEventHandler() {
    return this.featureFlagEventHandler;
  }
}

package io.harness.accesscontrol.migrations;

import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.accesscontrol.commons.events.EventHandler;

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

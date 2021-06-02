package io.harness.accesscontrol.preference.events;

import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
public class NGRBACEnabledFeatureFlagEventConsumer implements EventConsumer {
  private final NGRBACEnabledFeatureFlagEventFilter ngrbacEnabledFeatureFlagEventFilter;
  private final NGRBACEnabledFeatureFlagEventHandler ngrbacEnabledFeatureFlagEventHandler;

  @Inject
  public NGRBACEnabledFeatureFlagEventConsumer(NGRBACEnabledFeatureFlagEventFilter ngRbacEnabledFeatureFlagEventFilter,
      NGRBACEnabledFeatureFlagEventHandler ngRbacEnabledFeatureFlagEventHandler) {
    this.ngrbacEnabledFeatureFlagEventFilter = ngRbacEnabledFeatureFlagEventFilter;
    this.ngrbacEnabledFeatureFlagEventHandler = ngRbacEnabledFeatureFlagEventHandler;
  }

  @Override
  public EventFilter getEventFilter() {
    return this.getNgrbacEnabledFeatureFlagEventFilter();
  }

  @Override
  public EventHandler getEventHandler() {
    return this.getNgrbacEnabledFeatureFlagEventHandler();
  }
}

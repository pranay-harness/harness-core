package io.harness.accesscontrol.commons.events;

import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PL)
public class FeatureFlagEventListenerService extends EventListenerService {
  @Inject
  public FeatureFlagEventListenerService(FeatureFlagEventListener featureFlagEventListener) {
    super(featureFlagEventListener);
  }

  @Override
  public String getServiceName() {
    return FEATURE_FLAG_STREAM;
  }
}

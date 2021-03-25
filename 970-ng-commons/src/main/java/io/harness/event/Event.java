package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

@OwnedBy(PL)
public interface Event {
  ResourceScope getResourceScope();
  Resource getResource();
  String getEventType();
}

package io.harness.event;

import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

public interface Event {
  ResourceScope getResourceScope();
  Resource getResource();
  String getEventType();
}

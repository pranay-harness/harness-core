package io.harness;

import io.harness.ng.core.ResourceScope;

public interface HEvent {
  ResourceScope getResourceScope();
  String getResourceIdentifier();
  String getResourceType();

  Object getEventData();
  String getEventType();
}

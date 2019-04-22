package io.harness.event.lite;

import lombok.Value;

@Value
public class FeatureAvailability {
  private HarnessFeature feature;
  private boolean available;
}

package io.harness.accesscontrol.commons.iterators;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IteratorConfig {
  boolean enabled;
  long targetIntervalInSeconds;
}

package io.harness.state.io;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class MetadataDefinition<T> {
  Class<T> implementingClass; // use same encapsulation
  boolean required;
}

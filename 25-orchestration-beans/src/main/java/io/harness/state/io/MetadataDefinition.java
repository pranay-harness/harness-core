package io.harness.state.io;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

// TODO => Need to clean this up as well

@Value
@Builder
@Redesign
public class MetadataDefinition {
  String implementingClass;
  boolean required;
}

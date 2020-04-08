package io.harness.state.metadata;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

import java.util.List;

// TODO => Need to clean this up

@Redesign
public class StateMetadata {
  List<MetadataDefinition> inputs;
  List<MetadataDefinition> outputs;

  @Value
  @Builder
  @Redesign
  public static class MetadataDefinition {
    String implementingClass;
    boolean required;
  }
}

package io.harness.state.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.List;

// TODO => Need to clean this up

@OwnedBy(CDC)
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

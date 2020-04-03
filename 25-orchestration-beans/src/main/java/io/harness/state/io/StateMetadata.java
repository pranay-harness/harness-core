package io.harness.state.io;

import io.harness.annotations.Redesign;

import java.util.List;

@Redesign
public interface StateMetadata {
  List<MetadataDefinition<StateInput>> getInputs();
  List<MetadataDefinition<StateOutput>> getOutputs();
}

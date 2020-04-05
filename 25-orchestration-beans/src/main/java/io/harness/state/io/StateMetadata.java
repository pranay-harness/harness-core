package io.harness.state.io;

import io.harness.annotations.Redesign;

import java.util.List;

// TODO => Need to clean this up

@Redesign
public interface StateMetadata {
  List<MetadataDefinition> getInputs();
  List<MetadataDefinition> getOutputs();
}

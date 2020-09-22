package io.harness.ngpipeline.overlayinputset.beans.resource;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OverlayInputSetResponseDTO {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  String identifier;
  String name;
  String description;
  List<String> inputSetReferences;
  String overlayInputSetYaml;
  // Add Tags
}

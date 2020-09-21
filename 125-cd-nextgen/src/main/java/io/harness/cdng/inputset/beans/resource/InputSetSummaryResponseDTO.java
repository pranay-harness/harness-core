package io.harness.cdng.inputset.beans.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputSetSummaryResponseDTO {
  String identifier;
  String name;
  String pipelineIdentifier;
  String description;
  boolean isOverlaySet;
  // add tags when needed
}
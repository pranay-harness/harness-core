package io.harness.cdng.inputset.beans.resource;

import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("InputSetResponse")
public class InputSetResponseDTO {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  String identifier;
  String inputSetYaml;
  String name;
  String description;
  // Add tags
}

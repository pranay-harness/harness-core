package io.harness.cdng.inputset.beans.resource;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("MergeInputSetError")
public class MergeInputSetErrorDTO {
  String fieldName;
  String message;
  String causedByInputSetIdentifier;
}

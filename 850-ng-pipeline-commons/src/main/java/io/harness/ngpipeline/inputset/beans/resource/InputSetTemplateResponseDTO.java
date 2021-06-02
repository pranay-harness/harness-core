package io.harness.ngpipeline.inputset.beans.resource;

import io.harness.annotations.dev.ToBeDeleted;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("InputSetTemplateResponse")
@ToBeDeleted
@Deprecated
public class InputSetTemplateResponseDTO {
  String inputSetTemplateYaml;
}

package io.harness.ngpipeline.inputset.beans.resource;

import io.harness.annotations.dev.ToBeDeleted;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("InputSetError")
@ToBeDeleted
@Deprecated
public class InputSetErrorDTO {
  String fieldName;
  String message;
  String identifierOfErrorSource;
}

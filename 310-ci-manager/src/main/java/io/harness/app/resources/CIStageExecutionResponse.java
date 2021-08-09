package io.harness.app.resources;

import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CIStageExecutionResponse {
  Status status;
  ErrorCode code;
  String message;
  String executionId;
}

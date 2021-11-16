package io.harness.delegate.beans.ci.awsvm.runner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecuteStepResponse {
  @JsonProperty("ExitCode") int ExitCode;
}

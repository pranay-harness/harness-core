package io.harness.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningEngineError {
  @JsonProperty("analysis_minute") private long analysisMinute;
  @JsonProperty("error_msg") private String errorMsg;
}

package io.harness.cdng.pipeline.beans;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CDPipelineValidationInfo {
  CDPipeline cdPipeline;
  boolean isError;
  Map<String, VisitorErrorResponseWrapper> uuidToValidationErrors;
}

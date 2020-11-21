package io.harness.cdng.pipeline.beans;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CDPipelineValidationInfo {
  NgPipeline ngPipeline;
  boolean isError;
  Map<String, VisitorErrorResponseWrapper> uuidToValidationErrors;
}

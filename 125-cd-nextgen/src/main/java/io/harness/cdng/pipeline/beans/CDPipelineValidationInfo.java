package io.harness.cdng.pipeline.beans;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CDPipelineValidationInfo {
  NgPipeline ngPipeline;
  boolean isError;
  Map<String, VisitorErrorResponseWrapper> uuidToValidationErrors;
}

package io.harness.ngpipeline.inputset.beans.entities;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class MergeInputSetResponse {
  NgPipeline mergedPipeline;
  boolean isErrorResponse;
  NgPipeline errorPipeline;
  Map<String, VisitorErrorResponseWrapper> uuidToErrorResponseMap;
}

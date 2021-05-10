package io.harness.ngpipeline.inputset.beans.entities;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ToBeDeleted
@Deprecated
public class MergeInputSetResponse {
  NgPipeline mergedPipeline;
  boolean isErrorResponse;
  NgPipeline errorPipeline;
  Map<String, VisitorErrorResponseWrapper> uuidToErrorResponseMap;
}

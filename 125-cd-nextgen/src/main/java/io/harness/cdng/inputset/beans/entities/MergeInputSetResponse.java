package io.harness.cdng.inputset.beans.entities;

import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class MergeInputSetResponse {
  String pipelineYaml;
  boolean isErrorResponse;
  String errorPipelineYaml;
  Map<String, VisitorErrorResponseWrapper> uuidToErrorResponseMap;
}

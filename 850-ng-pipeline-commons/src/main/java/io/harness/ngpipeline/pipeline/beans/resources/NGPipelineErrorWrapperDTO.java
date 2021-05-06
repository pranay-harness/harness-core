package io.harness.ngpipeline.pipeline.beans.resources;

import io.harness.annotations.dev.ToBeDeleted;

import io.swagger.annotations.ApiModel;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("NGPipelineErrorWrapper")
@ToBeDeleted
@Deprecated
public class NGPipelineErrorWrapperDTO {
  String errorPipelineYaml;
  Map<String, NGPipelineErrorResponseDTO> uuidToErrorResponseMap;
}

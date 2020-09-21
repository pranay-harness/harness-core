package io.harness.cdng.pipeline.mappers;

import io.harness.cdng.pipeline.beans.CDPipelineValidationInfo;
import io.harness.cdng.pipeline.beans.dto.CDPipelineValidationInfoDTO;
import io.harness.yaml.utils.JsonPipelineUtils;
import lombok.experimental.UtilityClass;

import java.io.IOException;

@UtilityClass
public class PipelineValidationMapper {
  public CDPipelineValidationInfoDTO writePipelineValidationDto(CDPipelineValidationInfo cdPipelineValidationInfo)
      throws IOException {
    return CDPipelineValidationInfoDTO.builder()
        .pipelineYaml(JsonPipelineUtils.writeYamlString(cdPipelineValidationInfo.getCdPipeline()))
        .uuidToErrorResponseMap(cdPipelineValidationInfo.getUuidToValidationErrors())
        .isErrorResponse(cdPipelineValidationInfo.isError())
        .build();
  }
}

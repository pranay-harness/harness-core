package io.harness.cdng.pipeline.mappers;

import io.harness.cdng.pipeline.beans.CDPipelineValidationInfo;
import io.harness.cdng.pipeline.beans.dto.CDPipelineValidationInfoDTO;
import io.harness.yaml.utils.JsonPipelineUtils;

import java.io.IOException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PipelineValidationMapper {
  public CDPipelineValidationInfoDTO writePipelineValidationDto(CDPipelineValidationInfo cdPipelineValidationInfo)
      throws IOException {
    return CDPipelineValidationInfoDTO.builder()
        .pipelineYaml(JsonPipelineUtils.writeYamlString(cdPipelineValidationInfo.getNgPipeline()))
        .uuidToErrorResponseMap(cdPipelineValidationInfo.getUuidToValidationErrors())
        .isErrorResponse(cdPipelineValidationInfo.isError())
        .build();
  }
}

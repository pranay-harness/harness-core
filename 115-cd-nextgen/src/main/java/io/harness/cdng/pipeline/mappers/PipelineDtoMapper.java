package io.harness.cdng.pipeline.mappers;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class PipelineDtoMapper {
  public CDPipelineEntity toPipelineEntity(
      String accountId, String orgId, String projectId, String yaml, CDPipeline cdPipeline) {
    return CDPipelineEntity.builder()
        .cdPipeline(cdPipeline)
        .yamlPipeline(yaml)
        .accountId(accountId)
        .orgIdentifier(orgId)
        .projectIdentifier(projectId)
        .identifier(cdPipeline.getIdentifier())
        .build();
  }

  public CDPipelineResponseDTO writePipelineDto(CDPipelineEntity cdPipelineEntity) {
    return CDPipelineResponseDTO.builder()
        .cdPipeline(cdPipelineEntity.getCdPipeline())
        .yamlPipeline(cdPipelineEntity.getYamlPipeline())
        .executionsPlaceHolder(new ArrayList<>())
        .build();
  }
}

package io.harness.pms.pipeline.mappers;

import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.pipeline.ExecutionSummaryInfoDTO;
import io.harness.pms.pipeline.PMSPipelineResponseDTO;
import io.harness.pms.pipeline.PMSPipelineSummaryResponseDTO;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.yaml.BasicPipeline;
import io.harness.pms.yaml.YamlUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PMSPipelineDtoMapper {
  public PMSPipelineResponseDTO writePipelineDto(PipelineEntity pipelineEntity) {
    return PMSPipelineResponseDTO.builder()
        .yamlPipeline(pipelineEntity.getYaml())
        .version(pipelineEntity.getVersion())
        .build();
  }

  public PipelineEntity toPipelineEntity(String accountId, String orgId, String projectId, String yaml) {
    try {
      BasicPipeline basicPipeline = YamlUtils.read(yaml, BasicPipeline.class);
      if (NGExpressionUtils.matchesInputSetPattern(basicPipeline.getIdentifier())) {
        throw new InvalidRequestException("Pipeline identifier cannot be runtime input");
      }
      return PipelineEntity.builder()
          .yaml(yaml)
          .accountId(accountId)
          .orgIdentifier(orgId)
          .projectIdentifier(projectId)
          .name(basicPipeline.getName())
          .identifier(basicPipeline.getIdentifier())
          .description(basicPipeline.getDescription())
          .tags(TagMapper.convertToList(basicPipeline.getTags()))
          .build();
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot create pipeline entity due to " + e.getMessage());
    }
  }

  public PMSPipelineSummaryResponseDTO preparePipelineSummary(PipelineEntity pipelineEntity) {
    return PMSPipelineSummaryResponseDTO.builder()
        .identifier(pipelineEntity.getIdentifier())
        .description(pipelineEntity.getDescription())
        .name(pipelineEntity.getName())
        .tags(TagMapper.convertToMap(pipelineEntity.getTags()))
        .version(pipelineEntity.getVersion())
        .numOfStages(pipelineEntity.getStageCount())
        .executionSummaryInfo(getExecutionSummaryInfoDTO(pipelineEntity))
        .lastUpdatedAt(pipelineEntity.getLastUpdatedAt())
        .createdAt(pipelineEntity.getCreatedAt())
        .build();
  }

  private ExecutionSummaryInfoDTO getExecutionSummaryInfoDTO(PipelineEntity pipelineEntity) {
    return ExecutionSummaryInfoDTO.builder()
        .deployments(getNumberOfDeployments(pipelineEntity))
        .numOfErrors(getNumberOfErrorsLast10Days(pipelineEntity))
        .lastExecutionStatus(pipelineEntity.getExecutionSummaryInfo() != null
                ? pipelineEntity.getExecutionSummaryInfo().getLastExecutionStatus()
                : null)
        .lastExecutionTs(pipelineEntity.getExecutionSummaryInfo() != null
                ? pipelineEntity.getExecutionSummaryInfo().getLastExecutionTs()
                : null)
        .build();
  }

  private int getNumberOfErrorsLast10Days(PipelineEntity pipeline) {
    if (pipeline.getExecutionSummaryInfo() == null) {
      return 0;
    }
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, -10);
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    int numOfErrors = 0;
    for (int i = 0; i < 10; i++) {
      cal.add(Calendar.DAY_OF_YEAR, 1);
      numOfErrors = pipeline.getExecutionSummaryInfo().getNumOfErrors().getOrDefault(sdf.format(cal.getTime()), 0);
    }
    return numOfErrors;
  }

  // TODO: Implement after implementation with Executions
  private List<Integer> getNumberOfDeployments(PipelineEntity pipeline) {
    if (pipeline.getExecutionSummaryInfo() == null) {
      return new ArrayList<>();
    }
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, -10);
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    List<Integer> numberOfDeployments = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      cal.add(Calendar.DAY_OF_YEAR, 1);
      numberOfDeployments.add(
          pipeline.getExecutionSummaryInfo().getNumOfErrors().getOrDefault(sdf.format(cal.getTime()), 0));
    }
    return numberOfDeployments;
  }
}

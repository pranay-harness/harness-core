package io.harness.pms.mappers;

import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.beans.entities.PipelineEntity;
import io.harness.pms.beans.resources.PMSPipelineResponseDTO;
import io.harness.pms.beans.resources.PMSPipelineSummaryResponseDTO;
import io.harness.pms.beans.yaml.BasicPipeline;
import io.harness.pms.yaml.YamlUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
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
        .deployments(getNumberOfDeployments(pipelineEntity))
        .numOfErrors(getNumberOfErrorsLast10Days(pipelineEntity))
        .numOfStages(getNumberOfStages(pipelineEntity))
        .build();
  }

  // TODO: Save as part of pipeline Entity.
  private int getNumberOfStages(PipelineEntity pipeline) {
    int min = 0;
    int maxPlusOne = 3;
    SecureRandom r = new SecureRandom();
    return r.ints(min, maxPlusOne).findFirst().getAsInt();
  }

  // TODO: Implement after implementation of executions
  private int getNumberOfErrorsLast10Days(PipelineEntity pipeline) {
    int min = 0;
    int maxPlusOne = 3;
    SecureRandom r = new SecureRandom();
    return r.ints(min, maxPlusOne).findFirst().getAsInt();
  }

  // TODO: Implement after implementation with Executions
  private List<Integer> getNumberOfDeployments(PipelineEntity pipeline) {
    int min = 0;
    int maxPlusOne = 6;
    SecureRandom r = new SecureRandom();
    List<Integer> deployments = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      deployments.add(r.ints(min, maxPlusOne).findFirst().getAsInt());
    }
    return deployments;
  }
}

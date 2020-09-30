package io.harness.cdng.pipeline.mappers;

import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PipelineDtoMapper {
  public NgPipelineEntity toPipelineEntity(
      String accountId, String orgId, String projectId, String yaml, NgPipeline ngPipeline) {
    return NgPipelineEntity.builder()
        .ngPipeline(ngPipeline)
        .yamlPipeline(yaml)
        .accountId(accountId)
        .orgIdentifier(orgId)
        .projectIdentifier(projectId)
        .identifier(ngPipeline.getIdentifier())
        .build();
  }

  public CDPipelineResponseDTO writePipelineDto(NgPipelineEntity ngPipelineEntity) {
    return CDPipelineResponseDTO.builder()
        .ngPipeline(ngPipelineEntity.getNgPipeline())
        .yamlPipeline(ngPipelineEntity.getYamlPipeline())
        .executionsPlaceHolder(new ArrayList<>())
        .build();
  }
  public CDPipelineSummaryResponseDTO preparePipelineSummary(NgPipelineEntity ngPipelineEntity) {
    return CDPipelineSummaryResponseDTO.builder()
        .identifier(ngPipelineEntity.getIdentifier())
        .description((String) ngPipelineEntity.getNgPipeline().getDescription().getJsonFieldValue())
        .name(ngPipelineEntity.getNgPipeline().getName())
        .tags(ngPipelineEntity.getNgPipeline().getTags())
        .numOfStages(getNumberOfStages(ngPipelineEntity.getNgPipeline()))
        .numOfErrors(getNumberOfErrorsLast10Days(ngPipelineEntity.getNgPipeline()))
        .deployments(getNumberOfDeployments(ngPipelineEntity.getNgPipeline()))
        .build();
  }

  private int getNumberOfStages(NgPipeline pipeline) {
    List<StageElementWrapper> stages = pipeline.getStages();
    int count = 0;
    for (StageElementWrapper wrapper : stages) {
      if (wrapper.getClass() == StageElement.class) {
        count++;
      } else {
        ParallelStageElement parallelStageElement = (ParallelStageElement) wrapper;
        count += parallelStageElement.getSections().size();
      }
    }
    return count;
  }

  // TODO: @Sahil for proper implementation
  private int getNumberOfErrorsLast10Days(NgPipeline pipeline) {
    int min = 0;
    int maxPlusOne = 3;
    SecureRandom r = new SecureRandom();
    return r.ints(min, maxPlusOne).findFirst().getAsInt();
  }

  // TODO: @Sahil for proper implementation
  private List<Integer> getNumberOfDeployments(NgPipeline pipeline) {
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

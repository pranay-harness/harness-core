package io.harness.cdng.pipeline.mappers;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

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
  public CDPipelineSummaryResponseDTO preparePipelineSummary(CDPipelineEntity cdPipelineEntity) {
    return CDPipelineSummaryResponseDTO.builder()
        .identifier(cdPipelineEntity.getIdentifier())
        .description((String) cdPipelineEntity.getCdPipeline().getDescription().getJsonFieldValue())
        .name(cdPipelineEntity.getCdPipeline().getName())
        .tags(cdPipelineEntity.getCdPipeline().getTags())
        .numOfStages(getNumberOfStages(cdPipelineEntity.getCdPipeline()))
        .numOfErrors(getNumberOfErrorsLast10Days(cdPipelineEntity.getCdPipeline()))
        .deployments(getNumberOfDeployments(cdPipelineEntity.getCdPipeline()))
        .build();
  }

  private int getNumberOfStages(CDPipeline pipeline) {
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
  private int getNumberOfErrorsLast10Days(CDPipeline pipeline) {
    int min = 0;
    int maxPlusOne = 3;
    SecureRandom r = new SecureRandom();
    return r.ints(min, maxPlusOne).findFirst().getAsInt();
  }

  // TODO: @Sahil for proper implementation
  private List<Integer> getNumberOfDeployments(CDPipeline pipeline) {
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

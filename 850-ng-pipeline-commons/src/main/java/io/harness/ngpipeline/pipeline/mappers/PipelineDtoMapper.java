package io.harness.ngpipeline.pipeline.mappers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.YamlException;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineResponseDTO;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineSummaryResponseDTO;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.utils.YamlPipelineUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PipelineDtoMapper {
  public NgPipelineEntity toPipelineEntity(String accountId, String orgId, String projectId, String yaml) {
    NgPipeline ngPipeline = null;
    try {
      ngPipeline = YamlPipelineUtils.read(yaml, NgPipeline.class);
    } catch (IOException ex) {
      throw new YamlException(ex.getMessage(), USER);
    }

    if (isEmpty(ngPipeline.getStages())) {
      throw new InvalidRequestException("stages cannot be empty for the given pipeline");
    }

    try {
      return NgPipelineEntity.builder()
          .ngPipeline(ngPipeline)
          .yamlPipeline(yaml)
          .accountId(accountId)
          .orgIdentifier(orgId)
          .projectIdentifier(projectId)
          .identifier(ngPipeline.getIdentifier())
          .tags(TagMapper.convertToList(ngPipeline.getTags()))
          .build();
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity due to " + e.getMessage());
    }
  }

  public NGPipelineResponseDTO writePipelineDto(NgPipelineEntity ngPipelineEntity) {
    return NGPipelineResponseDTO.builder()
        .ngPipeline(ngPipelineEntity.getNgPipeline())
        .yamlPipeline(ngPipelineEntity.getYamlPipeline())
        .executionsPlaceHolder(new ArrayList<>())
        .version(ngPipelineEntity.getVersion())
        .build();
  }

  public NGPipelineSummaryResponseDTO preparePipelineSummary(NgPipelineEntity ngPipelineEntity) {
    return NGPipelineSummaryResponseDTO.builder()
        .identifier(ngPipelineEntity.getIdentifier())
        .description((String) ngPipelineEntity.getNgPipeline().getDescription().getJsonFieldValue())
        .name(ngPipelineEntity.getNgPipeline().getName())
        .tags(ngPipelineEntity.getNgPipeline().getTags())
        .numOfStages(getNumberOfStages(ngPipelineEntity.getNgPipeline()))
        .numOfErrors(getNumberOfErrorsLast10Days(ngPipelineEntity.getNgPipeline()))
        .deployments(getNumberOfDeployments(ngPipelineEntity.getNgPipeline()))
        .version(ngPipelineEntity.getVersion())
        .build();
  }

  private int getNumberOfStages(NgPipeline pipeline) {
    List<StageElementWrapper> stages = pipeline.getStages();
    if (isEmpty(stages)) {
      return 0;
    }
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

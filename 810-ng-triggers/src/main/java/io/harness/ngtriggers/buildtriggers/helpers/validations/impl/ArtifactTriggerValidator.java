package io.harness.ngtriggers.buildtriggers.helpers.validations.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.ngtriggers.buildtriggers.helpers.validations.TriggerValidator;
import io.harness.pms.pipeline.PipelineEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ArtifactTriggerValidator implements TriggerValidator {
  private final BuildTriggerHelper validationHelper;

  @Override
  public void validate(TriggerDetails triggerDetails) {
    try {
      Optional<PipelineEntity> pipelineEntity =
          validationHelper.fetchPipelineForTrigger(triggerDetails.getNgTriggerEntity());

      if (!pipelineEntity.isPresent()) {
        throw new InvalidRequestException("");
      }

      PipelineEntity pipeline = pipelineEntity.get();
      String pipelineYml = pipeline.getYaml();
      BuildTriggerOpsData buildTriggerOpsData =
          validationHelper.generateBuildTriggerOpsDataForArtifact(triggerDetails, pipelineYml);
      // stageRef & manifestRef exists
      if (isEmpty(buildTriggerOpsData.getPipelineBuildSpecMap())) {
        throw new InvalidRequestException("");
      }

      // type is validated {Gcr, DockerRegistry}
      validationHelper.validateBuildType(buildTriggerOpsData);

      validateBasedOnArtifactType(buildTriggerOpsData);
    } catch (Exception e) {
    }
  }

  private void validateBasedOnArtifactType(BuildTriggerOpsData buildTriggerOpsData) {
    String typeFromTrigger = validationHelper.fetchBuildType(buildTriggerOpsData.getTriggerSpecMap());
  }
}

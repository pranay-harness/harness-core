package io.harness.ngtriggers.validations.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.ngtriggers.helpers.TriggerHelper;
import io.harness.ngtriggers.validations.TriggerValidator;
import io.harness.ngtriggers.validations.ValidationResult;
import io.harness.ngtriggers.validations.ValidationResult.ValidationResultBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class ArtifactTriggerValidator implements TriggerValidator {
  private final BuildTriggerHelper validationHelper;

  @Override
  public ValidationResult validate(TriggerDetails triggerDetails) {
    ValidationResultBuilder builder = ValidationResult.builder().success(true);
    try {
      Optional<String> pipelineYmlOptional =
          validationHelper.fetchPipelineForTrigger(triggerDetails.getNgTriggerEntity());

      if (!pipelineYmlOptional.isPresent()) {
        return builder.success(false).message("Pipeline doesn't exists").build();
      }

      String pipelineYml = pipelineYmlOptional.get();
      BuildTriggerOpsData buildTriggerOpsData =
          validationHelper.generateBuildTriggerOpsDataForArtifact(triggerDetails, pipelineYml);
      // stageRef & manifestRef exists
      if (isEmpty(buildTriggerOpsData.getPipelineBuildSpecMap())) {
        throw new InvalidRequestException(
            "Artifact With Given StageIdentifier and ArtifactRef in Trigger does not exist in Pipeline");
      }

      // type is validated {Gcr, DockerRegistry}
      validationHelper.validateBuildType(buildTriggerOpsData);

      validateBasedOnArtifactType(buildTriggerOpsData);
    } catch (Exception e) {
      String message = new StringBuilder(128)
                           .append("Exception while applying ArtifactTriggerValidation for Trigger: ")
                           .append(TriggerHelper.getTriggerRef(triggerDetails.getNgTriggerEntity()))
                           .toString();
      log.error(message, e);
      builder.success(false).message(
          new StringBuilder(message).append(". Exception: ").append(e.getMessage()).toString());
    }

    return builder.build();
  }

  private void validateBasedOnArtifactType(BuildTriggerOpsData buildTriggerOpsData) {
    validationHelper.fetchBuildType(buildTriggerOpsData.getTriggerSpecMap());
  }
}

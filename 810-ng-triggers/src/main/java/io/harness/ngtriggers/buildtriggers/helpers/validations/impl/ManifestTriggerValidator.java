package io.harness.ngtriggers.buildtriggers.helpers.validations.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.source.ManifestType.HELM_MANIFEST;

import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.ngtriggers.buildtriggers.helpers.generator.GeneratorFactory;
import io.harness.ngtriggers.buildtriggers.helpers.generator.PollingItemGenerator;
import io.harness.ngtriggers.buildtriggers.helpers.validations.TriggerValidator;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.polling.contracts.PollingItem;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.assertj.core.util.VisibleForTesting;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ManifestTriggerValidator implements TriggerValidator {
  private final BuildTriggerHelper validationHelper;
  private final GeneratorFactory generatorFactory;

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
          validationHelper.generateBuildTriggerOpsDataForManifest(triggerDetails, pipelineYml);

      // stageRef & manifestRef exists
      if (isEmpty(buildTriggerOpsData.getPipelineBuildSpecMap())) {
        throw new InvalidRequestException("");
      }

      // type is validated {HemlChart}
      validationHelper.validateBuildType(buildTriggerOpsData);

      validateBasedOnManifestType(buildTriggerOpsData);

    } catch (Exception e) {
    }
  }

  @VisibleForTesting
  void validateBasedOnManifestType(BuildTriggerOpsData buildTriggerOpsData) {
    String typeFromTrigger = validationHelper.fetchBuildType(buildTriggerOpsData.getTriggerSpecMap());

    if (HELM_MANIFEST.getValue().equals(typeFromTrigger)) {
      validateForHelmChart(buildTriggerOpsData);
    }
  }

  @VisibleForTesting
  void validateForHelmChart(BuildTriggerOpsData buildTriggerOpsData) {
    String storeTypeFromTrigger = validationHelper.fetchStoreTypeForHelm(buildTriggerOpsData);
    String storeTypeFromPipeline =
        ((TextNode) buildTriggerOpsData.getPipelineBuildSpecMap().get("spec.store.type")).asText();

    // Store type mismatch
    if (!storeTypeFromPipeline.equals(storeTypeFromTrigger)) {
      throw new InvalidRequestException("");
    }

    // ChartVersion can not be a fixed value
    if (buildTriggerOpsData.getPipelineBuildSpecMap().containsKey("spec.chartVersion")) {
      String chartVersion =
          ((TextNode) buildTriggerOpsData.getPipelineBuildSpecMap().get("spec.chartVersion")).asText();
      if (!chartVersion.equals("<+input>")) {
        // throw new InvalidRequestException("");
      }
    }

    validateRuntimeInputsForHelmChart(buildTriggerOpsData);
  }

  @VisibleForTesting
  void validateRuntimeInputsForHelmChart(BuildTriggerOpsData buildTriggerOpsData) {
    PollingItemGenerator pollingItemGenerator = generatorFactory.retrievePollingItemGenerator(buildTriggerOpsData);
    if (pollingItemGenerator == null) {
      throw new InvalidRequestException("");
    }

    PollingItem pollingItem = pollingItemGenerator.generatePollingItem(buildTriggerOpsData);
    validationHelper.validatePollingItemForHelmChart(pollingItem);
  }
}

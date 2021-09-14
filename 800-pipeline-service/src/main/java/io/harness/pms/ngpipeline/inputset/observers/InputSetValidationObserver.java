/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.ngpipeline.inputset.observers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetListTypePMS;
import io.harness.pms.ngpipeline.inputset.helpers.InputSetErrorsHelper;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetFilterHelper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.observer.PipelineActionObserver;
import io.harness.repositories.inputset.PMSInputSetRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class InputSetValidationObserver implements PipelineActionObserver {
  @Inject PMSInputSetRepository inputSetRepository;
  @Inject PMSInputSetService inputSetService;
  @Inject ValidateAndMergeHelper validateAndMergeHelper;

  @Override
  public void onUpdate(PipelineEntity pipelineEntity) {
    Criteria criteria = PMSInputSetFilterHelper.createCriteriaForGetListForBranchAndRepo(pipelineEntity.getAccountId(),
        pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(),
        InputSetListTypePMS.INPUT_SET);

    List<InputSetEntity> allInputSets = inputSetRepository.findAll(criteria);
    allInputSets.forEach(inputSet -> checkIfInputSetIsValid(inputSet, pipelineEntity));

    Criteria criteriaOverlay = PMSInputSetFilterHelper.createCriteriaForGetListForBranchAndRepo(
        pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(),
        pipelineEntity.getIdentifier(), InputSetListTypePMS.OVERLAY_INPUT_SET);

    List<InputSetEntity> allOverlayInputSets = inputSetRepository.findAll(criteriaOverlay);
    allOverlayInputSets.forEach(inputSet -> checkIfOverlayInputSetIsValid(inputSet, pipelineEntity));
  }

  private void checkIfInputSetIsValid(InputSetEntity inputSet, PipelineEntity pipelineEntity) {
    InputSetErrorWrapperDTOPMS errorWrapperDTO =
        InputSetErrorsHelper.getErrorMap(pipelineEntity.getYaml(), inputSet.getYaml());
    if (errorWrapperDTO != null) {
      markAsInvalid(inputSet);
    } else {
      markAsValid(inputSet);
    }
  }

  private void checkIfOverlayInputSetIsValid(InputSetEntity overlayInputSet, PipelineEntity pipelineEntity) {
    Map<String, String> invalidReferences =
        validateAndMergeHelper.validateOverlayInputSet(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
            pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(), overlayInputSet.getYaml());
    if (!invalidReferences.isEmpty()) {
      markAsInvalid(overlayInputSet);
    } else {
      markAsValid(overlayInputSet);
    }
  }

  private void markAsInvalid(InputSetEntity inputSet) {
    String inputSetId = inputSet.getIdentifier();
    String pipelineId = inputSet.getPipelineIdentifier();
    boolean isMarked = inputSetService.switchValidationFlag(inputSet, true);
    if (!isMarked) {
      log.error("Could not mark input set " + inputSetId + " for pipeline " + pipelineId + " as invalid.");
    } else {
      log.info("Marked input set " + inputSetId + " for pipeline " + pipelineId + " as invalid.");
    }
  }

  private void markAsValid(InputSetEntity inputSet) {
    String inputSetId = inputSet.getIdentifier();
    String pipelineId = inputSet.getPipelineIdentifier();
    log.info("Input set " + inputSetId + " for pipeline " + pipelineId + " is valid.");
    if (inputSet.getIsInvalid()) {
      boolean isMarked = inputSetService.switchValidationFlag(inputSet, false);
      if (!isMarked) {
        log.error("Could not mark input set " + inputSetId + " for pipeline " + pipelineId + " as valid.");
      } else {
        log.info("Marked input set " + inputSetId + " for pipeline " + pipelineId + " as valid.");
      }
    }
  }
}

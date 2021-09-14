/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.validations.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.validations.TriggerValidator;
import io.harness.ngtriggers.validations.ValidationResult;
import io.harness.ngtriggers.validations.ValidationResult.ValidationResultBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PIPELINE)
public class PipelineRefValidator implements TriggerValidator {
  private final BuildTriggerHelper validationHelper;

  @Override
  public ValidationResult validate(TriggerDetails triggerDetails) {
    ValidationResultBuilder builder = ValidationResult.builder().success(true);
    NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
    Optional<String> pipelineYmlOptional = validationHelper.fetchPipelineForTrigger(ngTriggerEntity);

    if (!pipelineYmlOptional.isPresent()) {
      String ref = new StringBuilder(128)
                       .append("Pipeline with Ref -> ")
                       .append(ngTriggerEntity.getAccountId())
                       .append(':')
                       .append(ngTriggerEntity.getOrgIdentifier())
                       .append(':')
                       .append(ngTriggerEntity.getProjectIdentifier())
                       .append(':')
                       .append(ngTriggerEntity.getTargetIdentifier())
                       .append(" does not exists")
                       .toString();
      builder.success(false).message(ref);
    }

    return builder.build();
  }
}

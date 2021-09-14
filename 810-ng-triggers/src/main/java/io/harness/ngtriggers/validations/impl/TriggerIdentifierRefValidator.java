/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.validations.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.validations.TriggerValidator;
import io.harness.ngtriggers.validations.ValidationResult;
import io.harness.ngtriggers.validations.ValidationResult.ValidationResultBuilder;

@OwnedBy(PIPELINE)
public class TriggerIdentifierRefValidator implements TriggerValidator {
  @Override
  public ValidationResult validate(TriggerDetails triggerDetails) {
    ValidationResultBuilder builder = ValidationResult.builder().success(true);
    boolean success = true;
    StringBuilder message = new StringBuilder(512);

    NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
    if (ngTriggerEntity == null) {
      throw new InvalidArgumentsException("Trigger Entity was NULL");
    }

    if (isBlank(ngTriggerEntity.getIdentifier())) {
      success = false;
      message.append("Identifier can not be null for trigger\n");
    }

    if (isBlank(ngTriggerEntity.getName())) {
      success = false;
      message.append("Name can not be null for trigger\n");
    }

    if (isBlank(ngTriggerEntity.getAccountId())) {
      success = false;
      message.append("AccountId can not be null for trigger\n");
    }

    if (isBlank(ngTriggerEntity.getOrgIdentifier())) {
      success = false;
      message.append("OrgIdentifier can not be null for trigger\n");
    }

    if (isBlank(ngTriggerEntity.getProjectIdentifier())) {
      success = false;
      message.append("ProjectIdentifier can not be null for trigger\n");
    }

    if (isBlank(ngTriggerEntity.getTargetIdentifier())) {
      success = false;
      message.append("PipelineIdentifier can not be null for trigger\n");
    }

    if (!success) {
      builder.success(false).message(message.toString());
    }

    return builder.build();
  }
}

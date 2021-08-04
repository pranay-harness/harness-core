package io.harness.ngtriggers.buildtriggers.helpers.validations.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.buildtriggers.helpers.validations.TriggerValidator;

public class TriggerIdentifierRefValidator implements TriggerValidator {
  @Override
  public void validate(TriggerDetails triggerDetails) {
    NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
    if (ngTriggerEntity == null) {
      throw new InvalidRequestException("");
    }

    if (isBlank(ngTriggerEntity.getIdentifier())) {
      throw new InvalidRequestException("");
    }

    if (isBlank(ngTriggerEntity.getName())) {
      throw new InvalidRequestException("");
    }

    if (isBlank(ngTriggerEntity.getOrgIdentifier())) {
      throw new InvalidRequestException("");
    }

    if (isBlank(ngTriggerEntity.getProjectIdentifier())) {
      throw new InvalidRequestException("");
    }

    if (isBlank(ngTriggerEntity.getTargetIdentifier())) {
      throw new InvalidRequestException("");
    }
  }
}

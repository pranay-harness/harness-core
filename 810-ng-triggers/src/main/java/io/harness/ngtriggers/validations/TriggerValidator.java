package io.harness.ngtriggers.validations;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.TriggerDetails;

@OwnedBy(PIPELINE)
public interface TriggerValidator {
  ValidationResult validate(TriggerDetails triggerDetails);
}

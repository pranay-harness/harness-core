package io.harness.ngtriggers.buildtriggers.helpers.validations;

import io.harness.ngtriggers.beans.dto.TriggerDetails;

public interface TriggerValidator {
  void validate(TriggerDetails triggerDetails);
}

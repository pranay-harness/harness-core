package io.harness.pms.plan.creation.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface CreationValidator<T> {
  void validate(String accountId, T object);
}

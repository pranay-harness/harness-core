package io.harness.feature.handlers;

import io.harness.feature.beans.RestrictionDTO;
import io.harness.feature.constants.RestrictionType;

public interface RestrictionHandler {
  RestrictionType getRestrictionType();
  void check(String accountIdentifier);
  RestrictionDTO toRestrictionDTO(String accountIdentifier);
}

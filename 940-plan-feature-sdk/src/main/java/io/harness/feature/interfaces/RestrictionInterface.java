package io.harness.feature.interfaces;

import io.harness.feature.constants.RestrictionType;

public interface RestrictionInterface {
  RestrictionType getRestrictionType();
  boolean check(String accountIdentifier);
}

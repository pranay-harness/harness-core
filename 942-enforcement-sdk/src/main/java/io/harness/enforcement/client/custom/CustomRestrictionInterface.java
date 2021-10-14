package io.harness.enforcement.client.custom;

import io.harness.enforcement.beans.CustomRestrictionEvaluationDTO;

public interface CustomRestrictionInterface {
  boolean evaluateCustomRestriction(CustomRestrictionEvaluationDTO customFeatureEvaluationDTO);
}

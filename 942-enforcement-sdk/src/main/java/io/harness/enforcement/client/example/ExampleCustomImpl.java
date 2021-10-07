package io.harness.enforcement.client.example;

import io.harness.enforcement.beans.CustomRestrictionEvaluationDTO;
import io.harness.enforcement.client.custom.CustomRestrictionInterface;
import io.harness.licensing.Edition;

import com.google.inject.Singleton;

@Singleton
public class ExampleCustomImpl implements CustomRestrictionInterface {
  @Override
  public boolean evaluateCustomRestriction(CustomRestrictionEvaluationDTO customFeatureEvaluationDTO) {
    Edition edition = customFeatureEvaluationDTO.getEdition();
    if (Edition.TEAM.equals(edition)) {
      return true;
    } else {
      return false;
    }
  }
}

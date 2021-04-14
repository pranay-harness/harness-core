package io.harness.cvng.cdng.services.impl;

import io.harness.filters.GenericStepPMSFilterJsonCreator;

import java.util.Set;

public class CVNGStepFilterJsonCreator extends GenericStepPMSFilterJsonCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return CVNGPlanCreator.CVNG_SUPPORTED_TYPES;
  }
}

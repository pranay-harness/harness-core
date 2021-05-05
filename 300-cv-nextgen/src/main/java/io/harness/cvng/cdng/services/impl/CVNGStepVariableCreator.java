package io.harness.cvng.cdng.services.impl;

import io.harness.cvng.cdng.beans.CVNGStepType;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CVNGStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return new HashSet<>(Arrays.asList(CVNGStepType.CVNG_VERIFY.getDisplayName()));
  }
}
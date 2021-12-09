package io.harness.cdng.creator.variables;

import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HelmStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return new HashSet<>(Arrays.asList(StepSpecTypeConstants.HELM_DEPLOY, StepSpecTypeConstants.HELM_ROLLBACK));
  }
}

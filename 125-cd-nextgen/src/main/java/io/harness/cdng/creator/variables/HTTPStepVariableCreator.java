package io.harness.cdng.creator.variables;

import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Collections;
import java.util.Set;

public class HTTPStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Collections.singleton(StepSpecTypeConstants.HTTP);
  }
}

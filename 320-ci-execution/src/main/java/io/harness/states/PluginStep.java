package io.harness.states;

import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.state.StepType;

public class PluginStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = PluginStepInfo.typeInfo.getStepType();
}

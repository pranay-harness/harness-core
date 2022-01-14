package software.wings.service.impl.yaml.handler.workflow;

import software.wings.sm.StateType;

public class DynatraceStepYamlBuilder extends CVMetricStepYamlBuilder {
  @Override
  public StateType getStateType() {
    return StateType.DYNA_TRACE;
  }
}

package software.wings.service.impl.yaml.handler.workflow;

import software.wings.sm.StateType;

public class AppDynamicsStepYamlBuilder extends CVMetricStepYamlBuilder {
  @Override
  public StateType getStateType() {
    return StateType.APP_DYNAMICS;
  }
}

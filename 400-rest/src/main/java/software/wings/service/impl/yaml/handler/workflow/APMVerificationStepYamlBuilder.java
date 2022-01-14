package software.wings.service.impl.yaml.handler.workflow;

import software.wings.sm.StateType;

public class APMVerificationStepYamlBuilder extends CVMetricStepYamlBuilder {
  @Override
  public StateType getStateType() {
    return StateType.APM_VERIFICATION;
  }
}

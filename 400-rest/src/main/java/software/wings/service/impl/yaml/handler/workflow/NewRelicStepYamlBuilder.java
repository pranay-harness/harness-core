package software.wings.service.impl.yaml.handler.workflow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.StateType;

import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CV)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
public class NewRelicStepYamlBuilder extends CVMetricStepYamlBuilder {
  @Override
  public StateType getStateType() {
    return StateType.NEW_RELIC;
  }
}

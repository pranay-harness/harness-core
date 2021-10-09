package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.workflow.StepYaml;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class CommandStepYamlBuilder extends ShellScriptStepYamlBuilder {
  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    // nothing here
  }
}

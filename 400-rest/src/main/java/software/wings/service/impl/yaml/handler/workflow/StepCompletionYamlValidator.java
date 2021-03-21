package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.workflow.StepYaml;

@TargetModule(HarnessModule._870_YAML_BEANS)
@OwnedBy(CDC)
public interface StepCompletionYamlValidator {
  void validate(ChangeContext<StepYaml> changeContext);
}

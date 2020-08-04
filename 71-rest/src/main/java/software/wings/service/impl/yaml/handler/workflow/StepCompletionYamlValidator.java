package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.yaml.workflow.StepYaml;

@OwnedBy(CDC)
public interface StepCompletionYamlValidator {
  void validate(StepYaml stepYaml);
}

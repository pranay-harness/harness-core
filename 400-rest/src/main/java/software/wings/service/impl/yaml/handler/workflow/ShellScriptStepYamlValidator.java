package software.wings.service.impl.yaml.handler.workflow;

import io.harness.exception.InvalidRequestException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.workflow.StepYaml;

public class ShellScriptStepYamlValidator implements StepCompletionYamlValidator {
  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    String name = changeContext.getYaml().getName();
    if (name.contains(".")) {
      throw new InvalidRequestException("Shell script step [" + name + "] has '.' in its name");
    }
  }
}

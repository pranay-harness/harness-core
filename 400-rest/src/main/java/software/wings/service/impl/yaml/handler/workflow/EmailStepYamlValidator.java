package software.wings.service.impl.yaml.handler.workflow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.IncompleteStateException;
import software.wings.yaml.workflow.StepYaml;

import org.apache.commons.lang3.StringUtils;

@TargetModule(HarnessModule._870_YAML_BEANS)
public class EmailStepYamlValidator implements StepCompletionYamlValidator {
  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    StepYaml stepYaml = changeContext.getYaml();

    if (StringUtils.isBlank((String) stepYaml.getProperties().get("toAddress"))) {
      throw new IncompleteStateException("\"toAddress\" could not be empty or null, please provide a value.");
    } else if (StringUtils.isBlank((String) stepYaml.getProperties().get("subject"))) {
      throw new IncompleteStateException("\"subject\" could not be empty or null, please provide a value.");
    } else if (StringUtils.isBlank((String) stepYaml.getProperties().get("body"))) {
      throw new IncompleteStateException("\"body\" could not be empty or null, please provide a value.");
    }
  }
}

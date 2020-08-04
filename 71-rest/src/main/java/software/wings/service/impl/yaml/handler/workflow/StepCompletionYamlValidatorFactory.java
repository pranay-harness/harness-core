package software.wings.service.impl.yaml.handler.workflow;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import software.wings.sm.StepType;

@UtilityClass
@Slf4j
public class StepCompletionYamlValidatorFactory {
  public static StepCompletionYamlValidator getValidatorForStepType(StepType stepType) {
    StepCompletionYamlValidator validator = null;
    try {
      validator = stepType.getYamlValidatorClass().getConstructor().newInstance();
    } catch (Exception e) {
      logger.warn("Can not get yaml validator for step type" + stepType.getType(), e);
    }
    return validator;
  }
}

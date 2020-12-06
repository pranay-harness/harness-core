package software.wings.service.impl.yaml.handler.workflow;

import software.wings.sm.StepType;

import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepCompletionYamlValidatorFactory {
  @Inject private Injector injector;

  public StepCompletionYamlValidator getValidatorForStepType(StepType stepType) {
    StepCompletionYamlValidator validator = null;
    try {
      validator = injector.getInstance(stepType.getYamlValidatorClass());
    } catch (Exception e) {
      log.warn("Can not get yaml validator for step type" + stepType.getType(), e);
    }
    return validator;
  }
}

package io.harness.ngpipeline.inputset.validators;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.InputSetValidator;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.exception.InvalidRequestException;

@Singleton
public class InputSetValidatorFactory {
  @Inject private Injector injector;

  public RuntimeValidator obtainValidator(
      InputSetValidator inputSetValidator, EngineExpressionService engineExpressionService, Ambiance ambiance) {
    RuntimeValidator runtimeValidator;
    switch (inputSetValidator.getValidatorType()) {
      case ALLOWED_VALUES:
        runtimeValidator = new AllowedValuesValidator(engineExpressionService, ambiance);
        break;
      case REGEX:
        runtimeValidator = new RegexValidator(engineExpressionService, ambiance);
        break;
      default:
        throw new InvalidRequestException(
            "No Invoker present for execution mode :" + inputSetValidator.getValidatorType());
    }
    injector.injectMembers(runtimeValidator);
    return runtimeValidator;
  }
}

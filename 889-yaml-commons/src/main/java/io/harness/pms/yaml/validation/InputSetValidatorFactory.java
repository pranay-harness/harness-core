/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.yaml.validation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.EngineExpressionEvaluator;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class InputSetValidatorFactory {
  @Inject private Injector injector;

  public RuntimeValidator obtainValidator(InputSetValidator inputSetValidator,
      EngineExpressionEvaluator engineExpressionEvaluator, boolean skipUnresolvedExpressionsCheck) {
    RuntimeValidator runtimeValidator;
    switch (inputSetValidator.getValidatorType()) {
      case ALLOWED_VALUES:
        runtimeValidator = new AllowedValuesValidator(engineExpressionEvaluator, skipUnresolvedExpressionsCheck);
        break;
      case REGEX:
        runtimeValidator = new RegexValidator(engineExpressionEvaluator, skipUnresolvedExpressionsCheck);
        break;
      default:
        throw new InvalidRequestException(
            "No Invoker present for execution mode :" + inputSetValidator.getValidatorType());
    }
    injector.injectMembers(runtimeValidator);
    return runtimeValidator;
  }
}

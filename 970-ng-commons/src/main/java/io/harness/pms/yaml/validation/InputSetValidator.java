/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.yaml.validation;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.InputSetValidatorType;

import lombok.Value;

@Value
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.pms.yaml.validation.InputSetValidator")
public class InputSetValidator {
  InputSetValidatorType validatorType;
  // Content of the validator will be set here by Deserializer.
  String parameters;
}

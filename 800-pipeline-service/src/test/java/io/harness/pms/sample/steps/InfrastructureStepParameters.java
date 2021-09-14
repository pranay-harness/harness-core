/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sample.steps;

import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfrastructureStepParameters implements StepParameters {
  String environmentName;
  Map<String, Object> infrastructureDefinition;
  ParameterField<Boolean> tmpBool;
}

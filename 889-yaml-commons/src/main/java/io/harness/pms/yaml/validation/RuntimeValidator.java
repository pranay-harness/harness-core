/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.yaml.validation;

public interface RuntimeValidator {
  RuntimeValidatorResponse isValidValue(Object currentValue, String parameters);
}

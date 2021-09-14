/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.yaml.validation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RuntimeValidatorResponse {
  boolean isValid;
  // This is set only if not valid;
  String errorMessage;
}

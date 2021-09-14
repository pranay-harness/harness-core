/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import java.util.Collection;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ValidationError {
  private String message;
  @Singular private Collection<String> restrictedFeatures;
}

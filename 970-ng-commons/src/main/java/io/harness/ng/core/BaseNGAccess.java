/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
public class BaseNGAccess implements NGAccess {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
}

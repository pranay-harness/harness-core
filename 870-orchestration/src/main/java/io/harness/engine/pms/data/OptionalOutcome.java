/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.pms.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionalOutcome {
  boolean found;
  String outcome;
}

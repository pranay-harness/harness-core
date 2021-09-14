/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.pipeline;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepData {
  NGStepType type;
  String name;
}

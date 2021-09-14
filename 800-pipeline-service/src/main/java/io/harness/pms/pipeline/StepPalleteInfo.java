/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline;

import io.harness.pms.contracts.steps.StepInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepPalleteInfo {
  String moduleName;
  List<StepInfo> stepTypes;
}

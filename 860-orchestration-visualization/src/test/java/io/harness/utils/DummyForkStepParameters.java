/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.utils;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;

@Builder
@RecasterAlias("io.harness.utils.DummyForkStepParameters")
public class DummyForkStepParameters implements StepParameters {
  @Singular List<String> parallelNodeIds;
}

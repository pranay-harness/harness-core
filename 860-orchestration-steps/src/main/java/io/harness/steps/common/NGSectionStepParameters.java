/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.common;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("ngSectionStepParameters")
@RecasterAlias("io.harness.steps.common.NGSectionStepParameters")
public class NGSectionStepParameters implements StepParameters {
  String childNodeId;
  String logMessage;
}

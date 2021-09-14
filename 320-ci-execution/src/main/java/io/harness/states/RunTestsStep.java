/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.states;

import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.pms.contracts.steps.StepType;

public class RunTestsStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = RunTestsStepInfo.STEP_TYPE;
}

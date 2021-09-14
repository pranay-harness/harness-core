/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngpipeline.status;

import io.harness.pms.sdk.core.steps.io.StepParameters;

public interface BuildUpdateParameters extends StepParameters {
  BuildUpdateType getBuildUpdateType();
}

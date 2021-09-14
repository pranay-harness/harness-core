/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.pod;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CI)
public enum CIContainerType {
  STEP_EXECUTOR(CIContainerSource.BUILD_JOB),
  ADD_ON(CIContainerSource.HARNESS_WORKER),
  RUN(CIContainerSource.HARNESS_WORKER),
  PLUGIN(CIContainerSource.HARNESS_WORKER),
  SERVICE(CIContainerSource.HARNESS_WORKER),
  LITE_ENGINE(CIContainerSource.HARNESS_WORKER),
  TEST_INTELLIGENCE(CIContainerSource.HARNESS_WORKER);

  CIContainerSource ciContainerSource;

  CIContainerType(CIContainerSource ciContainerSource) {
    this.ciContainerSource = ciContainerSource;
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.environment.pod.container;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * Type of each container inside the pod for running CI job
 */

@OwnedBy(HarnessTeam.CI)
public enum ContainerType {
  STEP_EXECUTOR(ContainerSource.BUILD_JOB);
  ContainerSource containerSource;

  ContainerType(ContainerSource containerSource) {
    this.containerSource = containerSource;
  }
}

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
